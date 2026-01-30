package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.dto.MandateCreationRequest;
import trustrail.api.dto.MandateResponse;
import trustrail.api.entity.*;
import trustrail.api.entity.enums.*;
import trustrail.api.exception.TrustLimitExceededException;
import trustrail.api.repo.*;
import trustrail.api.service.integration.PWAIntegrationService;
import trustrail.api.service.integration.PWAMandateCreationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * MANDATE CREATION SERVICE - SINGLE ORCHESTRATION POINT
 *
 * Handles the complete mandate creation flow:
 * 1. Validate customer and eligibility
 * 2. Create mandate in TrustRail database (PENDING state)
 * 3. Call PWA to create mandate
 * 4. Update mandate with PWA response (activation URL, mandate ID)
 * 5. Create payment schedule
 * 6. Update trust profile
 *
 * All or nothing - transaction rolls back if PWA call fails
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MandateCreationService {

    private final MandateRepo mandateRepository;
    private final CustomerRepo customerRepository;
    private final TrustProfileRepo trustProfileRepository;
    private final PaymentTransactionRepo transactionRepository;
    private final PWAIntegrationService pwaIntegrationService;

    @Value("${pwa.biller-code}")
    private String billerCode;

    /**
     * CREATE MANDATE - Main entry point
     *
     * Flow:
     * 1. Validate customer exists and has linked account
     * 2. Calculate amounts (total, down payment, financed amount)
     * 3. Create mandate in PENDING state
     * 4. Call PWA to create mandate
     * 5. Update mandate with PWA response
     * 6. Create payment schedule
     * 7. Update trust profile
     */
    @Transactional
    public MandateResponse createMandate(Business business, MandateCreationRequest request) {
        log.info("Starting mandate creation for customer: {} in business: {}",
                request.getExternalCustomerId(), business.getId());

        // ========== STEP 1: Validate Customer ==========
        Customer customer = validateAndFetchCustomer(business, request.getExternalCustomerId());

        // ========== STEP 1.5: VALIDATE TRUST LIMITS (ADD THIS) ==========
        validateTrustLimits(customer, request);

        // ========== STEP 2: Calculate Amounts ==========
        BigDecimal totalAmount = request.getTotalAmount();
        BigDecimal downPayment = request.getDownPayment() != null ?
                request.getDownPayment() : BigDecimal.ZERO;
        BigDecimal amountToFinance = totalAmount.subtract(downPayment);

        log.info("Mandate amounts - Total: {}, Down: {}, Finance: {}",
                totalAmount, downPayment, amountToFinance);

        // ========== STEP 3: Create Local Mandate (PENDING) ==========
        Mandate mandate = createLocalMandate(
                business,
                customer,
                request,
                totalAmount,
                downPayment,
                amountToFinance
        );

        log.info("Local mandate created: {} with status: {}",
                mandate.getMandateReference(), mandate.getStatus());

        // ========== STEP 4: Call PWA ==========
        PWAMandateCreationResult pwaResult = callPWAForMandateCreation(customer, mandate);

        // ========== STEP 5: Update Mandate with PWA Response ==========
        updateMandateWithPWAResponse(mandate, pwaResult);

        log.info("Mandate updated with PWA response - ID: {}, URL: {}",
                mandate.getPwaMandateId(), mandate.getActivationUrl());

        // ========== STEP 6: Create Payment Schedule ==========
        if (request.getMandateType() == MandateType.INSTALMENT) {
            createInstalmentSchedule(mandate, amountToFinance);
        }

        // ========== STEP 7: Update Trust Profile ==========
        updateTrustProfile(customer, amountToFinance);

        log.info("Mandate creation completed successfully: {}", mandate.getMandateReference());

        return mapToMandateResponse(mandate, customer);
    }

    /**
     * STEP 1: Validate customer exists and has encrypted data
     */
    private Customer validateAndFetchCustomer(Business business, String externalCustomerId) {
        Customer customer = customerRepository
                .findByBusinessAndExternalCustomerId(business, externalCustomerId)
                .orElseThrow(() -> {
                    log.error("Customer not found: {} in business: {}",
                            externalCustomerId, business.getId());
                    return new RuntimeException(
                            "Customer not found: " + externalCustomerId
                    );
                });

        // Verify account is linked
        if (!Boolean.TRUE.equals(customer.getAccountLinked())) {
            log.error("Customer account not linked: {}", externalCustomerId);
            throw new RuntimeException(
                    "Account not linked. Customer must link bank account first."
            );
        }

        // Verify encrypted data exists
        if (customer.getEncryptedSecure() == null || customer.getEncryptedSecure().isEmpty()) {
            log.error("No encrypted secure data for customer: {}", externalCustomerId);
            throw new RuntimeException(
                    "Customer account data not encrypted. Please create customer with encrypted secure field."
            );
        }

        log.info("Customer validated: {}", externalCustomerId);
        return customer;
    }

    // Add this method to your MandateCreationService class

    /**
     * VALIDATE TRUST LIMITS - Enforce max eligible amount
     */
    private void validateTrustLimits(Customer customer, MandateCreationRequest request) {
        TrustProfile trustProfile = trustProfileRepository.findByCustomer(customer)
                .orElseThrow(() -> new RuntimeException(
                        "Trust profile not found for customer: " + customer.getExternalCustomerId()
                ));

        BigDecimal requestedAmount = request.getTotalAmount();
        BigDecimal maxEligible = trustProfile.getMaxEligibleAmount();
        BigDecimal currentOutstanding = trustProfile.getOutstandingBalance();

        // Check 1: Total amount exceeds max eligible
        if (requestedAmount.compareTo(maxEligible) > 0) {
            throw new TrustLimitExceededException(
                    String.format(
                            "Requested amount ₦%s exceeds your eligible limit of ₦%s. " +
                                    "Current trust score: %d. Build trust by making successful payments.",
                            requestedAmount, maxEligible, trustProfile.getOverallTrustScore()
                    )
            );
        }

        // Check 2: Outstanding balance + new amount exceeds limit
        BigDecimal totalExposure = currentOutstanding.add(requestedAmount);
        if (totalExposure.compareTo(maxEligible) > 0) {
            throw new TrustLimitExceededException(
                    String.format(
                            "Total exposure (₦%s outstanding + ₦%s new) exceeds your limit of ₦%s. " +
                                    "Please clear existing balance or reduce amount.",
                            currentOutstanding, requestedAmount, maxEligible
                    )
            );
        }

        // Check 3: Trust state restrictions
        switch (trustProfile.getTrustState()) {
            case DEFAULTED -> throw new TrustLimitExceededException(
                    "Account is in default state. No new mandates allowed. Please contact support."
            );
            case RESTRICTED -> {
                if (requestedAmount.compareTo(new BigDecimal("10000")) > 0) {
                    throw new TrustLimitExceededException(
                            "Your account is restricted. Maximum allowed amount: ₦10,000"
                    );
                }
            }
        }

        log.info("Trust validation passed: customer={}, amount={}, trustScore={}, limit={}",
                customer.getExternalCustomerId(),
                requestedAmount,
                trustProfile.getOverallTrustScore(),
                maxEligible
        );
    }

    /**
     * STEP 3: Create mandate record in database (PENDING state)
     */
    private Mandate createLocalMandate(
            Business business,
            Customer customer,
            MandateCreationRequest request,
            BigDecimal totalAmount,
            BigDecimal downPayment,
            BigDecimal amountToFinance) {

        // Calculate instalment amount if type is INSTALMENT
        BigDecimal instalmentAmount = null;
        if (request.getMandateType() == MandateType.INSTALMENT && request.getInstalmentCount() > 0) {
            instalmentAmount = amountToFinance.divide(
                    BigDecimal.valueOf(request.getInstalmentCount()),
                    2,
                    RoundingMode.HALF_UP
            );
        }

        Mandate mandate = Mandate.builder()
                .business(business)
                .customer(customer)
                .mandateReference(generateMandateReference())
                .mandateType(request.getMandateType())
                .status(MandateStatus.PENDING)  // Will be ACTIVE after PWA activation
                .totalAmount(totalAmount)
                .amountPaid(BigDecimal.ZERO)
                .remainingBalance(totalAmount)
                .downPayment(downPayment)
                .instalmentCount(request.getInstalmentCount())
                .instalmentsPaid(0)
                .instalmentAmount(instalmentAmount)
                .repeatFrequency(request.getRepeatFrequency())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .narration(request.getNarration())
                .build();

        return mandateRepository.save(mandate);
    }

    /**
     * STEP 4: Call PWA Integration Service
     * Passes pre-encrypted customer data to PWA
     */
    private PWAMandateCreationResult callPWAForMandateCreation(Customer customer, Mandate mandate) {
        log.info("Calling PWA to create mandate for customer: {}",
                customer.getExternalCustomerId());

        try {
            PWAMandateCreationResult result = pwaIntegrationService.createMandate(
                    customer.getExternalCustomerId(),      // customer_ref
                    customer.getFirstName(),               // firstname
                    customer.getLastName(),                // surname
                    customer.getEmail(),                   // email
                    customer.getPhoneNumber(),             // mobile_no
                    customer.getEncryptedSecure(),         // Already encrypted!
                    customer.getEncryptedBvn() != null ?
                            customer.getEncryptedBvn() : "", // Already encrypted!
                    billerCode,                            // From config
                    mandate.getTotalAmount()               // amount
            );

            if (result == null) {
                log.error("PWA returned null result");
                throw new RuntimeException("PWA service returned null");
            }

            if (!"Successful".equalsIgnoreCase(result.getStatus())) {
                log.error("PWA mandate creation failed: {}", result.getMessage());
                throw new RuntimeException(
                        "PWA mandate creation failed: " + result.getMessage()
                );
            }

            log.info("PWA mandate created successfully - ID: {}", result.getMandateId());
            return result;

        } catch (Exception e) {
            log.error("PWA mandate creation call failed", e);
            throw new RuntimeException("Failed to create mandate in PWA: " + e.getMessage(), e);
        }
    }

    /**
     * STEP 5: Update mandate with PWA response
     */
    private void updateMandateWithPWAResponse(Mandate mandate, PWAMandateCreationResult pwaResult) {
        mandate.setPwaMandateId(pwaResult.getMandateId());
        mandate.setActivationUrl(pwaResult.getActivationUrl());
        // Status remains PENDING until customer activates via PWA link
        mandateRepository.save(mandate);
    }

    /**
     * STEP 6: Create payment schedule for instalments
     * Only schedules the financed amount (not including down payment)
     */
    private void createInstalmentSchedule(Mandate mandate, BigDecimal amountToFinance) {
        log.info("Creating instalment schedule for mandate: {} - {} instalments",
                mandate.getMandateReference(), mandate.getInstalmentCount());

        LocalDateTime scheduleStart = mandate.getStartDate() != null ?
                mandate.getStartDate() : LocalDateTime.now().plusDays(7);

        BigDecimal instalmentAmount = mandate.getInstalmentAmount();
        BigDecimal totalScheduled = BigDecimal.ZERO;

        for (int i = 1; i <= mandate.getInstalmentCount(); i++) {
            // Last instalment gets remainder (handles rounding)
            BigDecimal amount = (i == mandate.getInstalmentCount()) ?
                    amountToFinance.subtract(totalScheduled) :
                    instalmentAmount;

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .mandate(mandate)
                    .customer(mandate.getCustomer())
                    .transactionReference(generateTransactionReference())
                    .transactionType(TransactionType.INSTALMENT)
                    .status(TransactionStatus.SCHEDULED)
                    .amount(amount)
                    .instalmentNumber(i)
                    .scheduledDate(scheduleStart)
                    .retryCount(0)
                    .build();

            transactionRepository.save(transaction);

            if (i < mandate.getInstalmentCount()) {
                totalScheduled = totalScheduled.add(amount);
                scheduleStart = calculateNextScheduleDate(scheduleStart, mandate.getRepeatFrequency());
            }
        }

        log.info("Instalment schedule created - {} transactions scheduled",
                mandate.getInstalmentCount());
    }

    /**
     * Calculate next schedule date based on frequency
     */
    private LocalDateTime calculateNextScheduleDate(LocalDateTime current, RepeatFrequency frequency) {
        if (frequency == null) frequency = RepeatFrequency.MONTHLY;

        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case YEARLY -> current.plusYears(1);
        };
    }

    /**
     * STEP 7: Update trust profile with outstanding balance
     */
    private void updateTrustProfile(Customer customer, BigDecimal amountToFinance) {
        TrustProfile profile = trustProfileRepository.findByCustomer(customer)
                .orElseThrow(() -> {
                    log.error("Trust profile not found for customer: {}",
                            customer.getExternalCustomerId());
                    return new RuntimeException("Trust profile not found");
                });

        BigDecimal currentOutstanding = profile.getOutstandingBalance() != null ?
                profile.getOutstandingBalance() : BigDecimal.ZERO;

        profile.setOutstandingBalance(currentOutstanding.add(amountToFinance));
        trustProfileRepository.save(profile);

        log.info("Trust profile updated - Outstanding balance now: {}",
                profile.getOutstandingBalance());
    }

    /**
     * Map mandate entity to response DTO
     */
    private MandateResponse mapToMandateResponse(Mandate mandate, Customer customer) {
        return MandateResponse.builder()
                .id(mandate.getId())
                .mandateReference(mandate.getMandateReference())
                .pwaMandateId(mandate.getPwaMandateId())
                .mandateType(mandate.getMandateType())
                .status(mandate.getStatus())
                .totalAmount(mandate.getTotalAmount())
                .amountPaid(mandate.getAmountPaid())
                .remainingBalance(mandate.getRemainingBalance())
                .downPayment(mandate.getDownPayment())
                .instalmentCount(mandate.getInstalmentCount())
                .instalmentsPaid(mandate.getInstalmentsPaid())
                .instalmentAmount(mandate.getInstalmentAmount())
                .repeatFrequency(mandate.getRepeatFrequency())
                .startDate(mandate.getStartDate())
                .endDate(mandate.getEndDate())
                .activationUrl(mandate.getActivationUrl())
                .nextDebitDate(mandate.getNextDebitDate())
                .narration(mandate.getNarration())
                .createdAt(mandate.getCreatedAt())
                .customer(mapCustomerToResponse(customer))
                .build();
    }

    private MandateResponse.CustomerResponse mapCustomerToResponse(Customer customer) {
        return MandateResponse.CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .externalCustomerId(customer.getExternalCustomerId())
                .build();
    }

    // ==================== HELPER METHODS ====================

    private String generateMandateReference() {
        return "MDT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * GET BUSINESS MANDATES
     *
     * Retrieve all mandates for a business
     *
     * @param business The business
     * @return List of mandate responses
     */
    @Transactional(readOnly = true)
    public List<MandateResponse> getBusinessMandates(Business business) {
        log.info("Retrieving mandates for business: {}", business.getId());

        List<MandateResponse> mandates = mandateRepository.findByBusiness(business)
                .stream()
                .map(MandateResponse::from)
                .toList();

        log.info("Found {} mandates for business: {}", mandates.size(), business.getId());
        return mandates;
    }

    /**
     * GET MANDATE BY REFERENCE
     *
     * Retrieve a specific mandate by its TrustRail reference
     *
     * @param mandateReference The mandate reference (e.g., MDT-ABC123DE)
     * @return Mandate response
     * @throws RuntimeException if mandate not found
     */
    @Transactional(readOnly = true)
    public MandateResponse getMandateByReference(String mandateReference) {
        log.info("Retrieving mandate by reference: {}", mandateReference);

        Mandate mandate = mandateRepository
                .findByMandateReference(mandateReference)
                .orElseThrow(() -> {
                    log.error("Mandate not found: {}", mandateReference);
                    return new RuntimeException("Mandate not found: " + mandateReference);
                });

        return MandateResponse.from(mandate);
    }

    private String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}