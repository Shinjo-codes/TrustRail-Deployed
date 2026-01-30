package trustrail.api.service;

import trustrail.api.dto.*;
import trustrail.api.entity.*;
import trustrail.api.entity.enums.MandateType;
import trustrail.api.entity.enums.TrustState;
import trustrail.api.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ELIGIBILITY DECISION ENGINE
 *
 * This service answers the critical question:
 * "Should this customer be allowed to defer payment or use instalments?"
 *
 * It applies business rules + trust scoring to make decisions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EligibilityService {

    private final CustomerRepo customerRepository;
    private final TrustProfileRepo trustProfileRepository;
    private final MandateRepo mandateRepository;
    private final TrustCalculationService trustCalculationService;

    // ==================== ELIGIBILITY CHECK ====================

    @Transactional(readOnly = true)
    public EligibilityCheckResponse checkEligibility(
            Business business,
            EligibilityCheckRequest request
    ) {
        log.info("Checking eligibility for customer {} in business {}",
                request.getExternalCustomerId(), business.getId());

        // Step 1: Find customer
        Customer customer = customerRepository
                .findByBusinessAndExternalCustomerId(business, request.getExternalCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Step 2: Get or create trust profile
        TrustProfile profile = trustProfileRepository.findByCustomer(customer)
                .orElseGet(() -> createInitialProfile(customer));

        // Step 3: Check basic requirements
        String basicCheck = checkBasicRequirements(customer, profile, request);
        if (basicCheck != null) {
            return buildDenialResponse(basicCheck, profile);
        }

        // Step 4: Apply trust-based rules
        EligibilityDecision decision = evaluateWithTrustRules(profile, request);

        // Step 5: Build response
        return buildEligibilityResponse(decision, profile);
    }

    // ==================== BASIC REQUIREMENT CHECKS ====================

    private String checkBasicRequirements(
            Customer customer,
            TrustProfile profile,
            EligibilityCheckRequest request
    ) {
        // 1. Account must be linked
        if (!customer.getAccountLinked()) {
            return "Bank account not linked. Please link account first.";
        }

        // 2. Check if customer has active mandates (business rule: max 3 concurrent)
        long activeMandates = mandateRepository.countActiveByCustomerAndType(
                customer,
                request.getMandateStatus(),
                request.getPaymentType()

        );
        if (activeMandates >= 3) {
            return "Maximum active mandates reached. Complete existing payments first.";
        }

        // 3. Check trust state
        if (profile.getTrustState() == TrustState.DEFAULTED) {
            return "Account in default state. Clear outstanding balance to continue.";
        }

        // 4. Check if eligible for payment type
        if (request.getPaymentType() == MandateType.INSTALMENT
                && !profile.getEligibleForInstalments()) {
            return "Not eligible for instalment payments at this time.";
        }

        // 5. Outstanding balance check
        BigDecimal utilizationRatio = profile.getOutstandingBalance()
                .divide(profile.getMaxEligibleAmount(), 2, RoundingMode.HALF_UP);

        if (utilizationRatio.compareTo(BigDecimal.valueOf(0.9)) >= 0) {
            return "Outstanding balance too high. Reduce balance before new request.";
        }

        return null; // All basic checks passed
    }

    // ==================== TRUST-BASED EVALUATION ====================

    private EligibilityDecision evaluateWithTrustRules(
            TrustProfile profile,
            EligibilityCheckRequest request
    ) {
        EligibilityDecision decision = new EligibilityDecision();
        decision.setOriginalAmount(request.getAmount());

        int trustScore = profile.getOverallTrustScore();
        BigDecimal requestedAmount = request.getAmount();

        // ===== TRUST-BASED DECISION MATRIX =====

        // TRUSTED (80-100): Full approval
        if (trustScore >= 80) {
            if (requestedAmount.compareTo(profile.getMaxEligibleAmount()) <= 0) {
                decision.setDecision("APPROVED");
                decision.setApprovedAmount(requestedAmount);
                decision.setRequiredDownPayment(BigDecimal.ZERO);
                decision.setMaxInstalments(calculateMaxInstalments(requestedAmount));
                decision.setReason("Excellent trust score. Full amount approved.");
            } else {
                decision.setDecision("CONDITIONAL");
                decision.setApprovedAmount(profile.getMaxEligibleAmount());
                decision.setRequiredDownPayment(
                        requestedAmount.subtract(profile.getMaxEligibleAmount())
                );
                decision.setMaxInstalments(calculateMaxInstalments(profile.getMaxEligibleAmount()));
                decision.setReason("Amount exceeds limit. Approved up to limit with down payment.");
            }
        }

        // VERIFIED (60-79): Conditional approval
        else if (trustScore >= 60) {
            BigDecimal approvedAmount = requestedAmount.multiply(BigDecimal.valueOf(0.8)); // 80% of request
            BigDecimal downPayment = requestedAmount.multiply(BigDecimal.valueOf(0.2)); // 20% upfront

            decision.setDecision("CONDITIONAL");
            decision.setApprovedAmount(approvedAmount);
            decision.setRequiredDownPayment(downPayment);
            decision.setMaxInstalments(calculateMaxInstalments(approvedAmount));
            decision.setReason("Good trust score. 20% down payment required.");
        }

        // NEW (40-59): Heavy conditions or emergency only
        else if (trustScore >= 40) {
            if (request.getIsEmergency()) {
                // Emergency override
                BigDecimal approvedAmount = requestedAmount.multiply(BigDecimal.valueOf(0.6));
                BigDecimal downPayment = requestedAmount.multiply(BigDecimal.valueOf(0.4));

                decision.setDecision("CONDITIONAL");
                decision.setApprovedAmount(approvedAmount);
                decision.setRequiredDownPayment(downPayment);
                decision.setMaxInstalments(3); // Short term only
                decision.setReason("Emergency approved. 40% down payment required.");
            } else {
                BigDecimal downPayment = requestedAmount.multiply(BigDecimal.valueOf(0.5)); // 50% upfront

                decision.setDecision("CONDITIONAL");
                decision.setApprovedAmount(requestedAmount.multiply(BigDecimal.valueOf(0.5)));
                decision.setRequiredDownPayment(downPayment);
                decision.setMaxInstalments(2);
                decision.setReason("Limited trust history. 50% down payment required.");
            }
        }

        // RESTRICTED (20-39): Emergency only
        else if (trustScore >= 20) {
            if (request.getIsEmergency()) {
                decision.setDecision("CONDITIONAL");
                decision.setApprovedAmount(requestedAmount.multiply(BigDecimal.valueOf(0.4)));
                decision.setRequiredDownPayment(requestedAmount.multiply(BigDecimal.valueOf(0.6)));
                decision.setMaxInstalments(2);
                decision.setReason("Emergency only. 60% down payment required.");
            } else {
                decision.setDecision("DENIED");
                decision.setReason("Trust score too low. Full payment required.");
            }
        }

        // DEFAULTED (<20): Denied
        else {
            decision.setDecision("DENIED");
            decision.setReason("Account in poor standing. Clear outstanding balance.");
        }

        return decision;
    }

    // ==================== HELPER METHODS ====================

    private int calculateMaxInstalments(BigDecimal amount) {
        // Business rule: Larger amounts can have more instalments
        if (amount.compareTo(new BigDecimal("50000")) >= 0) {
            return 6;
        } else if (amount.compareTo(new BigDecimal("20000")) >= 0) {
            return 4;
        } else {
            return 3;
        }
    }

    private TrustProfile createInitialProfile(Customer customer) {
        TrustProfile profile = TrustProfile.builder()
                .customer(customer)
                .behavioralScore(50)
                .contextualScore(50)
                .progressiveScore(50)
                .overallTrustScore(50)
                .trustState(TrustState.NEW)
                .totalPayments(0)
                .successfulPayments(0)
                .failedPayments(0)
                .consecutiveFailures(0)
                .totalAmountPaid(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .eligibleForInstalments(true)
                .eligibleForDeferred(true)
                .maxEligibleAmount(new BigDecimal("100000.00"))
                .build();

        return trustProfileRepository.save(profile);
    }

    private EligibilityCheckResponse buildEligibilityResponse(
            EligibilityDecision decision,
            TrustProfile profile
    ) {
        return EligibilityCheckResponse.builder()
                .eligible(!decision.getDecision().equals("DENIED"))
                .decision(decision.getDecision())
                .reason(decision.getReason())
                .approvedAmount(decision.getApprovedAmount())
                .requiredDownPayment(decision.getRequiredDownPayment())
                .maxInstalments(decision.getMaxInstalments())
                .trustProfile(mapToTrustProfileResponse(profile))
                .evaluatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private EligibilityCheckResponse buildDenialResponse(String reason, TrustProfile profile) {
        return EligibilityCheckResponse.builder()
                .eligible(false)
                .decision("DENIED")
                .reason(reason)
                .trustProfile(mapToTrustProfileResponse(profile))
                .evaluatedAt(java.time.LocalDateTime.now())
                .build();
    }

    private TrustProfileResponse mapToTrustProfileResponse(TrustProfile profile) {
        return TrustProfileResponse.builder()
                .id(profile.getId())
                .behavioralScore(profile.getBehavioralScore())
                .contextualScore(profile.getContextualScore())
                .progressiveScore(profile.getProgressiveScore())
                .overallTrustScore(profile.getOverallTrustScore())
                .trustState(profile.getTrustState())
                .totalPayments(profile.getTotalPayments())
                .successfulPayments(profile.getSuccessfulPayments())
                .failedPayments(profile.getFailedPayments())
                .consecutiveFailures(profile.getConsecutiveFailures())
                .totalAmountPaid(profile.getTotalAmountPaid())
                .outstandingBalance(profile.getOutstandingBalance())
                .eligibleForInstalments(profile.getEligibleForInstalments())
                .eligibleForDeferred(profile.getEligibleForDeferred())
                .maxEligibleAmount(profile.getMaxEligibleAmount())
                .lastPaymentDate(profile.getLastPaymentDate())
                .build();
    }

    // Inner class for decision logic
    private static class EligibilityDecision {
        private String decision;
        private String reason;
        private BigDecimal originalAmount;
        private BigDecimal approvedAmount;
        private BigDecimal requiredDownPayment;
        private Integer maxInstalments;

        // Getters and setters
        public String getDecision() { return decision; }
        public void setDecision(String decision) { this.decision = decision; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public BigDecimal getOriginalAmount() { return originalAmount; }
        public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
        public BigDecimal getApprovedAmount() { return approvedAmount; }
        public void setApprovedAmount(BigDecimal approvedAmount) { this.approvedAmount = approvedAmount; }
        public BigDecimal getRequiredDownPayment() { return requiredDownPayment; }
        public void setRequiredDownPayment(BigDecimal requiredDownPayment) {
            this.requiredDownPayment = requiredDownPayment;
        }
        public Integer getMaxInstalments() { return maxInstalments; }
        public void setMaxInstalments(Integer maxInstalments) { this.maxInstalments = maxInstalments; }
    }
}

//Score 80-100 (TRUSTED):
//   - APPROVED: Full amount, no down payment
//   - Max 6 instalments for large amounts
//
//   Score 60-79 (VERIFIED):
//   - CONDITIONAL: 80% approved, 20% down payment
//   - Moderate instalment terms
//
//   Score 40-59 (NEW):
//   - CONDITIONAL: 50% down payment OR
//   - Emergency override: 40% down payment
//
//   Score 20-39 (RESTRICTED):
//   - Emergency only: 60% down payment
//   - Otherwise DENIED
//
//   Score 0-19 (DEFAULTED):
//   - DENIED: Must clear balance first