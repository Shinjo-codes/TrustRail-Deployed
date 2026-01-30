package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.entity.Customer;
import trustrail.api.entity.TrustProfile;
import trustrail.api.entity.enums.TrustState;
import trustrail.api.repo.PaymentTransactionRepo;
import trustrail.api.repo.TrustProfileRepo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * TRUST CALCULATION ENGINE
 *
 * This is the CORE of TrustRail's intelligence.
 * It evaluates customers across three dimensions:
 * 1. Behavioral Score (past payment behavior)
 * 2. Contextual Score (transaction context)
 * 3. Progressive Score (trust earned over time)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrustCalculationService {

    private final TrustProfileRepo trustProfileRepository;
    private final PaymentTransactionRepo transactionRepository;

    // ==================== TRUST SCORE CALCULATION ====================

    /**
     * Calculate overall trust score from three components
     * Weights: Behavioral (50%), Contextual (30%), Progressive (20%)
     */
    @Transactional
    public void recalculateTrustScore(Customer customer) {
        TrustProfile profile = trustProfileRepository.findByCustomer(customer)
                .orElseGet(() -> createInitialTrustProfile(customer));

        // Calculate individual scores
        int behavioralScore = calculateBehavioralScore(profile);
        int contextualScore = calculateContextualScore(profile);
        int progressiveScore = calculateProgressiveScore(profile);

        // Weighted overall score
        int overallScore = (int) (
                (behavioralScore * 0.5) +
                        (contextualScore * 0.3) +
                        (progressiveScore * 0.2)
        );

        // Update profile
        profile.setBehavioralScore(behavioralScore);
        profile.setContextualScore(contextualScore);
        profile.setProgressiveScore(progressiveScore);
        profile.setOverallTrustScore(overallScore);

        // Update trust state based on score
        updateTrustState(profile, overallScore);

        trustProfileRepository.save(profile);

        log.info("Recalculated trust for customer {}: Overall={}, Behavioral={}, Contextual={}, Progressive={}",
                customer.getId(), overallScore, behavioralScore, contextualScore, progressiveScore);
    }

    // ==================== BEHAVIORAL SCORE ====================
    /**
     * Based on payment history and consistency
     * - Success rate: 40 points
     * - Consecutive failures penalty: -30 points max
     * - Recent payment bonus: +20 points
     */
    private int calculateBehavioralScore(TrustProfile profile) {
        int score = 50; // Start at neutral

        // Success Rate Component (0-40 points)
        if (profile.getTotalPayments() > 0) {
            double successRate = (double) profile.getSuccessfulPayments() / profile.getTotalPayments();
            score += (int) (successRate * 40);
        }

        // Consecutive Failures Penalty (0 to -30 points)
        int failurePenalty = Math.min(profile.getConsecutiveFailures() * 10, 30);
        score -= failurePenalty;

        // Recent Payment Bonus (0-20 points)
        if (profile.getLastPaymentDate() != null) {
            long daysSinceLastPayment = ChronoUnit.DAYS.between(
                    profile.getLastPaymentDate(),
                    LocalDateTime.now()
            );

            if (daysSinceLastPayment <= 30) {
                score += 20; // Paid recently
            } else if (daysSinceLastPayment <= 60) {
                score += 10; // Paid somewhat recently
            }
        }

        return Math.max(0, Math.min(100, score)); // Clamp between 0-100
    }

    // ==================== CONTEXTUAL SCORE ====================
    /**
     * Based on current financial position
     * - Outstanding balance ratio: -40 points max
     * - Payment amount vs limit: adjusts score
     */
    private int calculateContextualScore(TrustProfile profile) {
        int score = 50; // Start at neutral

        // Outstanding Balance Impact (0 to -40 points)
        if (profile.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balanceRatio = profile.getOutstandingBalance()
                    .divide(profile.getMaxEligibleAmount(), 2, RoundingMode.HALF_UP);

            if (balanceRatio.compareTo(BigDecimal.valueOf(0.8)) >= 0) {
                score -= 40; // 80%+ utilized - high risk
            } else if (balanceRatio.compareTo(BigDecimal.valueOf(0.5)) >= 0) {
                score -= 25; // 50-80% utilized
            } else if (balanceRatio.compareTo(BigDecimal.valueOf(0.3)) >= 0) {
                score -= 10; // 30-50% utilized
            }
        } else {
            score += 30; // No outstanding balance - excellent
        }

        // Total Amount Paid Bonus (shows financial capacity)
        if (profile.getTotalAmountPaid().compareTo(new BigDecimal("50000")) > 0) {
            score += 20; // Proven high-value customer
        } else if (profile.getTotalAmountPaid().compareTo(new BigDecimal("10000")) > 0) {
            score += 10; // Moderate value
        }

        return Math.max(0, Math.min(100, score));
    }

    // ==================== PROGRESSIVE SCORE ====================
    /**
     * Trust earned over time through consistent behavior
     * - Account age: longer = better
     * - Payment frequency: regular payments = higher score
     */
    private int calculateProgressiveScore(TrustProfile profile) {
        int score = 50; // Start at neutral

        // Account Age Bonus
        long daysSinceCreation = ChronoUnit.DAYS.between(
                profile.getCreatedAt(),
                LocalDateTime.now()
        );

        if (daysSinceCreation >= 180) {
            score += 30; // 6+ months
        } else if (daysSinceCreation >= 90) {
            score += 20; // 3-6 months
        } else if (daysSinceCreation >= 30) {
            score += 10; // 1-3 months
        }

        // Payment Frequency Bonus
        if (profile.getTotalPayments() >= 10) {
            score += 20; // Frequent user
        } else if (profile.getTotalPayments() >= 5) {
            score += 10; // Regular user
        }

        return Math.max(0, Math.min(100, score));
    }
    // ==================== TRUST STATE MANAGEMENT ====================

    private void updateTrustState(TrustProfile profile, int overallScore) {
        TrustState currentState = profile.getTrustState();
        TrustState newState;

        if (overallScore >= 80) {
            newState = TrustState.TRUSTED;
        } else if (overallScore >= 60) {
            newState = TrustState.VERIFIED;
        } else if (overallScore >= 40) {
            newState = TrustState.NEW;
        } else if (overallScore >= 20) {
            newState = TrustState.RESTRICTED;
        } else {
            newState = TrustState.DEFAULTED;
        }

        if (currentState != newState) {
            log.info("Trust state changed for customer {}: {} -> {}",
                    profile.getCustomer().getId(), currentState, newState);
            profile.setTrustState(newState);
        }
    }

    // ==================== PAYMENT TRACKING ====================

    /**
     * Called when a payment succeeds
     */
    @Transactional
    public void recordSuccessfulPayment(Customer customer, BigDecimal amount) {
        TrustProfile profile = trustProfileRepository.findByCustomer(customer)
                .orElseThrow(() -> new RuntimeException("Trust profile not found"));

        profile.setSuccessfulPayments(profile.getSuccessfulPayments() + 1);
        profile.setTotalPayments(profile.getTotalPayments() + 1);
        profile.setConsecutiveFailures(0); // Reset failure streak
        profile.setTotalAmountPaid(profile.getTotalAmountPaid().add(amount));
        profile.setLastPaymentDate(LocalDateTime.now());

        // Reduce outstanding balance
        BigDecimal newBalance = profile.getOutstandingBalance().subtract(amount);
        profile.setOutstandingBalance(newBalance.max(BigDecimal.ZERO));

        trustProfileRepository.save(profile);

        // Recalculate trust score
        recalculateTrustScore(customer);
    }

    /**
     * Called when a payment fails
     */
    @Transactional
    public void recordFailedPayment(Customer customer, String reason) {
        TrustProfile profile = trustProfileRepository.findByCustomer(customer)
                .orElseThrow(() -> new RuntimeException("Trust profile not found"));

        profile.setFailedPayments(profile.getFailedPayments() + 1);
        profile.setTotalPayments(profile.getTotalPayments() + 1);
        profile.setConsecutiveFailures(profile.getConsecutiveFailures() + 1);
        profile.setLastFailureDate(LocalDateTime.now());

        // If too many consecutive failures, restrict access
        if (profile.getConsecutiveFailures() >= 3) {
            profile.setEligibleForInstalments(false);
            profile.setEligibleForDeferred(false);
            log.warn("Customer {} restricted due to {} consecutive failures",
                    customer.getId(), profile.getConsecutiveFailures());
        }

        trustProfileRepository.save(profile);

        // Recalculate trust score
        recalculateTrustScore(customer);
    }

    // ==================== HELPER METHODS ====================

    private TrustProfile createInitialTrustProfile(Customer customer) {
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
}
