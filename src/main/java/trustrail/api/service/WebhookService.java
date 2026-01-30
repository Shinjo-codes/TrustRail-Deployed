package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.dto.PWAWebhookPayload;
import trustrail.api.entity.*;
import trustrail.api.entity.enums.BusinessStatus;
import trustrail.api.entity.enums.MandateStatus;
import trustrail.api.entity.enums.MandateType;
import trustrail.api.entity.enums.RepeatFrequency;
import trustrail.api.entity.enums.TransactionStatus;
import trustrail.api.repo.BusinessRepo;
import trustrail.api.repo.CustomerRepo;
import trustrail.api.repo.MandateRepo;
import trustrail.api.repo.PaymentTransactionRepo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WEBHOOK SERVICE
 *
 * Handles callbacks from PayWithAccount
 * Updates trust scores based on payment outcomes
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentTransactionRepo transactionRepository;
    private final MandateRepo mandateRepository;
    private final CustomerRepo customerRepository;
    private final BusinessRepo businessRepository;
    private final TrustCalculationService trustCalculationService;

    /**
     * Process payment success webhook
     */
    @Transactional
    public void handlePaymentSuccess(PWAWebhookPayload payload) {
        String transactionRef = payload.getDetails().getTransactionRef();
        log.info("Processing payment success webhook: {}", transactionRef);

        // Find transaction by PWA transaction reference
        PaymentTransaction transaction = transactionRepository
                .findByTransactionReference(transactionRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionRef));

        Mandate mandate = transaction.getMandate();
        Customer customer = transaction.getCustomer();

        // Update transaction status
        transaction.setStatus(TransactionStatus.SUCCESSFUL);
        transaction.setExecutedDate(LocalDateTime.now());
        transaction.setPwaTransactionId(payload.getDetails().getPaymentId());
        transactionRepository.save(transaction);

        // Update mandate
        BigDecimal newAmountPaid = mandate.getAmountPaid().add(transaction.getAmount());
        BigDecimal newRemaining = mandate.getRemainingBalance().subtract(transaction.getAmount());

        mandate.setAmountPaid(newAmountPaid);
        mandate.setRemainingBalance(newRemaining);

        if (mandate.getMandateType() == MandateType.INSTALMENT) {
            mandate.setInstalmentsPaid(mandate.getInstalmentsPaid() + 1);

            // Check if all instalments paid
            if (mandate.getInstalmentsPaid().equals(mandate.getInstalmentCount())) {
                mandate.setStatus(MandateStatus.COMPLETED);
                log.info("Mandate completed: {}", mandate.getMandateReference());
            } else {
                // Set next debit date
                LocalDateTime nextDate = calculateNextDebitDate(
                        mandate.getNextDebitDate(),
                        mandate.getRepeatFrequency()
                );
                mandate.setNextDebitDate(nextDate);
            }
        }

        mandateRepository.save(mandate);

        // Update trust profile
        trustCalculationService.recordSuccessfulPayment(customer, transaction.getAmount());

        log.info("Payment success processed for transaction: {}", transactionRef);
    }

    /**
     * Process payment failure webhook
     */
    @Transactional
    public void handlePaymentFailure(PWAWebhookPayload payload) {
        String transactionRef = payload.getDetails().getTransactionRef();
        log.info("Processing payment failure webhook: {}", transactionRef);

        PaymentTransaction transaction = transactionRepository
                .findByTransactionReference(transactionRef)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionRef));

        Customer customer = transaction.getCustomer();

        // Update transaction
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setFailureReason(payload.getDetails().getTransactionDesc());
        transaction.setRetryCount(transaction.getRetryCount() + 1);

        // Schedule retry if eligible
        if (transaction.getRetryCount() < 3) {
            transaction.setNextRetryDate(LocalDateTime.now().plusDays(3));
            log.info("Retry scheduled for transaction: {}", transactionRef);
        }

        transactionRepository.save(transaction);

        // Update trust profile
        trustCalculationService.recordFailedPayment(customer, payload.getDetails().getTransactionDesc());

        log.info("Payment failure processed for transaction: {}", transactionRef);
    }

    /**
     * Handle mandate activation webhook
     */
    @Transactional
    public void handleMandateActivation(PWAWebhookPayload payload) {
        String transactionRef = payload.getDetails().getTransactionRef();
        Integer pwaMandateId = payload.getDetails().getData() != null &&
                payload.getDetails().getData().getData() != null ?
                payload.getDetails().getData().getData().getId() : null;

        log.info("Processing mandate activation: ref={}, pwaMandateId={}", transactionRef, pwaMandateId);

        if (pwaMandateId == null) {
            log.error("PWA mandate ID not found in webhook payload");
            return;
        }

        // Find mandate by transaction reference or PWA mandate ID
        Mandate mandate = mandateRepository.findByMandateReference(transactionRef)
                .or(() -> mandateRepository.findByPwaMandateId(String.valueOf(pwaMandateId)))
                .orElseThrow(() -> new RuntimeException("Mandate not found: " + transactionRef));

        // Update mandate status
        mandate.setStatus(MandateStatus.ACTIVE);
        mandate.setPwaMandateId(String.valueOf(pwaMandateId));
        mandateRepository.save(mandate);

        log.info("Mandate activated: {}", mandate.getMandateReference());
    }

    /**
     * Process mandate revocation webhook
     */
    @Transactional
    public void handleMandateRevocation(PWAWebhookPayload payload) {
        String mandateId = payload.getDetails().getMandateId();
        log.info("Processing mandate revocation: {}", mandateId);

        Mandate mandate = mandateRepository.findByPwaMandateId(mandateId)
                .orElseThrow(() -> new RuntimeException("Mandate not found: " + mandateId));

        // Update mandate status
        mandate.setStatus(MandateStatus.REVOKED);
        mandateRepository.save(mandate);

        // Cancel all scheduled transactions
        List<PaymentTransaction> scheduledTransactions =
                transactionRepository.findByMandate(mandate).stream()
                        .filter(t -> t.getStatus() == TransactionStatus.SCHEDULED)
                        .toList();

        for (PaymentTransaction txn : scheduledTransactions) {
            txn.setStatus(TransactionStatus.FAILED);
            txn.setFailureReason("Mandate revoked by customer");
            transactionRepository.save(txn);
        }

        // Update trust score
        Customer customer = mandate.getCustomer();
        trustCalculationService.recordFailedPayment(customer, "Mandate revoked");

        log.info("Mandate revocation processed: {}", mandate.getMandateReference());
    }

    /**
     * Handle biller approved webhook
     */
    @Transactional
    public void handleBillerApproved(PWAWebhookPayload payload) {
        String billerCode = payload.getDetails().getBillerCode();
        log.info("Processing biller approved: {}", billerCode);

        Business business = businessRepository.findByPwaBusinessId(billerCode)
                .orElseThrow(() -> new RuntimeException("Business not found with biller code: " + billerCode));

        business.setStatus(BusinessStatus.ACTIVE);
        businessRepository.save(business);

        log.info("Business approved and activated: {}", business.getBusinessName());
    }

    /**
     * Handle biller rejected webhook
     */
    @Transactional
    public void handleBillerRejected(PWAWebhookPayload payload) {
        String billerCode = payload.getDetails().getBillerCode();
        log.info("Processing biller rejected: {}", billerCode);

        Business business = businessRepository.findByPwaBusinessId(billerCode)
                .orElseThrow(() -> new RuntimeException("Business not found with biller code: " + billerCode));

        business.setStatus(BusinessStatus.REJECTED);
        businessRepository.save(business);

        log.info("Business rejected: {}", business.getBusinessName());
    }

    /**
     * Handle biller deactivated webhook
     */
    @Transactional
    public void handleBillerDeactivated(PWAWebhookPayload payload) {
        String billerCode = payload.getDetails().getBillerCode();
        log.info("Processing biller deactivated: {}", billerCode);

        Business business = businessRepository.findByPwaBusinessId(billerCode)
                .orElseThrow(() -> new RuntimeException("Business not found with biller code: " + billerCode));

        business.setStatus(BusinessStatus.INACTIVE);
        businessRepository.save(business);

        log.info("Business deactivated: {}", business.getBusinessName());
    }

    private LocalDateTime calculateNextDebitDate(LocalDateTime current, RepeatFrequency frequency) {
        if (frequency == null) frequency = RepeatFrequency.MONTHLY;

        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case YEARLY -> current.plusYears(1);
        };
    }
}