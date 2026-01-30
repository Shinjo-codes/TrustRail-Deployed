package trustrail.api.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.dto.DebitRequest;
import trustrail.api.dto.PaymentTransactionResponse;
import trustrail.api.entity.Customer;
import trustrail.api.entity.Mandate;
import trustrail.api.entity.PaymentTransaction;
import trustrail.api.entity.TrustProfile;
import trustrail.api.entity.enums.TransactionStatus;
import trustrail.api.entity.enums.TransactionType;
import trustrail.api.entity.enums.TrustState;
import trustrail.api.repo.MandateRepo;
import trustrail.api.repo.PaymentTransactionRepo;
import trustrail.api.repo.TrustProfileRepo;
import trustrail.api.service.integration.PWACollectResponse;
import trustrail.api.service.integration.PWAIntegrationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PAYMENT PROCESSING SERVICE
 *
 * Handles scheduled payment execution with trust-based decision logic
 * Uses pre-encrypted customer data stored in database
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentProcessingService {

    private final MandateRepo mandateRepository;
    private final PaymentTransactionRepo transactionRepository;
    private final TrustProfileRepo trustProfileRepository;
    private final TrustCalculationService trustCalculationService;
    private final PWAIntegrationService pwaService;

    @Value("${pwa.biller-code}")
    private String billerCode;

    /**
     * Scheduled job - runs every hour
     * Re-evaluates trust before EVERY debit (TrustRail's core value!)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void processDuePayments() {
        LocalDateTime now = LocalDateTime.now();
        List<PaymentTransaction> dueTransactions =
                transactionRepository.findScheduledTransactionsDue(now);

        log.info("Processing {} due payments", dueTransactions.size());

        for (PaymentTransaction transaction : dueTransactions) {
            processWithTrustEvaluation(transaction);
        }
    }

    private String generateTransactionReference() {
        return "TRX-" + System.currentTimeMillis();
    }

    @Transactional
    public PaymentTransactionResponse collectManualPayment(DebitRequest request) {

        // 1. Find mandate
        Mandate mandate = mandateRepository
                .findByMandateReference(request.getMandateReference())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Mandate not found: " + request.getMandateReference()
                        )
                );

        Customer customer = mandate.getCustomer();

        // 2. Create transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .mandate(mandate)
                .customer(customer)
                .amount(request.getAmount())
                .transactionReference(generateTransactionReference())
                .status(TransactionStatus.SCHEDULED)
                .scheduledDate(LocalDateTime.now())
                .retryCount(0)
                .transactionType(TransactionType.MANUAL_COLLECTION)
                .build();

        if (transaction.getTransactionType() == null) {
            throw new IllegalStateException("Transaction type must be specified");
        }

        transactionRepository.save(transaction);

        // 3. Apply trust evaluation + possible debit
        processWithTrustEvaluation(transaction);

        // 4. Reload updated state
        PaymentTransaction updatedTransaction =
                transactionRepository.findById(transaction.getId())
                        .orElseThrow();

        // 5. Return aligned response
        return PaymentTransactionResponse.from(updatedTransaction);
    }

    @Transactional
    public PaymentTransactionResponse subscriptionPayment(DebitRequest request) {

        // 1. Find mandate
        Mandate mandate = mandateRepository
                .findByMandateReference(request.getMandateReference())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Mandate not found: " + request.getMandateReference()
                        )
                );

        Customer customer = mandate.getCustomer();

        // 2. Create transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .mandate(mandate)
                .customer(customer)
                .amount(request.getAmount())
                .transactionReference(generateTransactionReference())
                .status(TransactionStatus.SCHEDULED)
                .scheduledDate(LocalDateTime.now())
                .retryCount(0)
                .transactionType(TransactionType.SUBSCRIPTION_PAYMENT)
                .build();

        if (transaction.getTransactionType() == null) {
            throw new IllegalStateException("Transaction type must be specified");
        }

        transactionRepository.save(transaction);

        // 3. Apply trust evaluation + possible debit
        processWithTrustEvaluation(transaction);

        // 4. Reload updated state
        PaymentTransaction updatedTransaction =
                transactionRepository.findById(transaction.getId())
                        .orElseThrow();

        // 5. Return aligned response
        return PaymentTransactionResponse.from(updatedTransaction);
    }

    /**
     * Process payment with trust evaluation
     * This implements TrustRail's "decision, not debit" philosophy
     */
    private void processWithTrustEvaluation(PaymentTransaction transaction) {
        Mandate mandate = transaction.getMandate();
        Customer customer = transaction.getCustomer();

        log.info("Evaluating payment {} for customer {}",
                transaction.getTransactionReference(),
                customer.getExternalCustomerId());

        // Get trust profile
        TrustProfile profile = trustProfileRepository.findByCustomer(customer)
                .orElseThrow(() ->
                        new IllegalStateException("TrustProfile not found for customer " + customer.getId())
                );

        // Evaluate decision based on trust
        PaymentDecision decision = evaluatePaymentDecision(profile, transaction);

        // Handle SKIP decision
        if (decision.getType() == PaymentDecision.DecisionType.SKIP) {
            log.warn("Skipping payment for customer {} - {}",
                    customer.getId(), decision.getReason());

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason(decision.getReason());
            transaction.setNextRetryDate(null);

            transactionRepository.save(transaction);
            return;
        }

        // Handle DEFER decision
        if (decision.getType() == PaymentDecision.DecisionType.DEFER) {
            log.info("Deferring payment for customer {} - {}",
                    customer.getId(), decision.getReason());

            transaction.setScheduledDate(LocalDateTime.now().plusDays(7));
            transaction.setFailureReason(decision.getReason());

            transactionRepository.save(transaction);
            return;
        }

        // PROCEED - Execute debit via PWA
        executeDebit(transaction);
    }

    /**
     * Trust-based decision logic
     * Determines whether to proceed, skip, or defer payment
     */
    private PaymentDecision evaluatePaymentDecision(
            TrustProfile profile,
            PaymentTransaction transaction
    ) {
        // Rule 1: Defaulted customers - no debits
        if (profile.getTrustState() == TrustState.DEFAULTED) {
            return PaymentDecision.skip(
                    "Customer in default state. Manual intervention required."
            );
        }

        // Rule 2: 3+ consecutive failures - pause debits
        if (profile.getConsecutiveFailures() >= 3) {
            return PaymentDecision.skip(
                    "Too many consecutive failures. Awaiting customer action."
            );
        }

        // Rule 3: Recent failure (within 3 days) - defer to allow recovery
        if (profile.getLastFailureDate() != null) {
            long daysSinceFailure =
                    java.time.temporal.ChronoUnit.DAYS.between(
                            profile.getLastFailureDate(),
                            LocalDateTime.now()
                    );

            if (daysSinceFailure < 3) {
                return PaymentDecision.defer(
                        "Recent payment failure. Deferring to allow account recovery."
                );
            }
        }

        // Rule 4: Outstanding balance too high (>95% of limit)
        BigDecimal utilizationRatio = profile.getOutstandingBalance()
                .divide(profile.getMaxEligibleAmount(), 2, RoundingMode.HALF_UP);

        if (utilizationRatio.compareTo(BigDecimal.valueOf(0.95)) >= 0) {
            return PaymentDecision.skip(
                    "Outstanding balance exceeds 95% of limit. Payment suspended."
            );
        }

        // All checks passed - proceed with debit
        return PaymentDecision.proceed();
    }

    /**
     * Execute debit via PWA using stored encrypted customer data
     */
    private void executeDebit(PaymentTransaction transaction) {
        try {
            Mandate mandate = transaction.getMandate();
            Customer customer = transaction.getCustomer();

            // Use stored encrypted values
            String encryptedSecure = customer.getEncryptedSecure();
            String encryptedBvn = customer.getEncryptedBvn() != null ?
                    customer.getEncryptedBvn() : "";

            // Validate encrypted data exists
            if (encryptedSecure == null || encryptedSecure.isEmpty()) {
                throw new IllegalStateException(
                        "Customer encrypted data not found for customer: " +
                                customer.getExternalCustomerId()
                );
            }

            log.info("Executing debit via PWA for transaction: {}",
                    transaction.getTransactionReference());

            // Call PWA with stored encrypted values
            PWACollectResponse response = pwaService.collectPayment(
                    customer.getExternalCustomerId(),
                    customer.getFirstName(),
                    customer.getLastName(),
                    customer.getEmail(),
                    customer.getPhoneNumber(),
                    encryptedSecure,
                    billerCode,
                    mandate.getPwaMandateId(),
                    transaction.getAmount(),
                    requestNarration(transaction, mandate)
            );

            // Update transaction with PWA response
            transaction.setPwaTransactionId(response.getTransactionId());
            transaction.setStatus(TransactionStatus.PENDING);
            transaction.setExecutedDate(LocalDateTime.now());

            transactionRepository.save(transaction);

            log.info("Debit initiated successfully. PWA Transaction ID: {}",
                    response.getTransactionId());

        } catch (Exception e) {
            log.error("Debit execution failed for transaction {}: {}",
                    transaction.getTransactionReference(),
                    e.getMessage(),
                    e);

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("PWA debit failed: " + e.getMessage());

            transactionRepository.save(transaction);

            // Record failure in trust profile
            trustCalculationService.recordFailedPayment(
                    transaction.getCustomer(),
                    e.getMessage()
            );
        }
    }

    private String requestNarration(PaymentTransaction tx, Mandate mandate) {
        if (tx.getInstalmentNumber() != null) {
            return "Instalment " + tx.getInstalmentNumber()
                    + " of " + mandate.getInstalmentCount();
        }
        return "Manual debit for mandate " + mandate.getMandateReference();
    }

    /**
     * Payment Decision Helper Class
     */
    @Data
    @AllArgsConstructor
    private static class PaymentDecision {

        private DecisionType type;
        private String reason;

        static PaymentDecision proceed() {
            return new PaymentDecision(DecisionType.PROCEED, null);
        }

        static PaymentDecision skip(String reason) {
            return new PaymentDecision(DecisionType.SKIP, reason);
        }

        static PaymentDecision defer(String reason) {
            return new PaymentDecision(DecisionType.DEFER, reason);
        }

        enum DecisionType {
            PROCEED,
            SKIP,
            DEFER
        }
    }
}