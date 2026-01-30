package trustrail.api.entity;

import jakarta.persistence.*;
import lombok.*;
import trustrail.api.entity.enums.TrustState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ==================== TRUST PROFILE ENTITY ====================
@Entity
@Table(name = "trust_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrustProfile extends Base {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Trust Scores (0-100)
    @Column(nullable = false)
    private Integer behavioralScore = 50; // Default: neutral

    @Column(nullable = false)
    private Integer contextualScore = 50;

    @Column(nullable = false)
    private Integer progressiveScore = 50;

    @Column(nullable = false)
    private Integer overallTrustScore = 50;

    // Trust State
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrustState trustState = TrustState.NEW;

    // Payment History Metrics
    @Column(nullable = false)
    private Integer totalPayments = 0;

    @Column(nullable = false)
    private Integer successfulPayments = 0;

    @Column(nullable = false)
    private Integer failedPayments = 0;

    @Column(nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalAmountPaid = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    private LocalDateTime lastPaymentDate;
    private LocalDateTime lastFailureDate;

    // Eligibility Flags
    @Column(nullable = false)
    private Boolean eligibleForInstalments = true;

    @Column(nullable = false)
    private Boolean eligibleForDeferred = true;

    @Column(precision = 19, scale = 2)
    private BigDecimal maxEligibleAmount = new BigDecimal("100000.00");
}
