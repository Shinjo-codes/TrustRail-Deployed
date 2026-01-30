package trustrail.api.entity;

import jakarta.persistence.*;
import lombok.*;
import trustrail.api.entity.enums.MandateStatus;
import trustrail.api.entity.enums.MandateType;
import trustrail.api.entity.enums.RepeatFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// ==================== MANDATE ENTITY ====================
@Entity
@Table(name = "mandates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mandate extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(unique = true, nullable = false)
    private String mandateReference; // TrustRail-generated

    @Column(unique = true)
    private String pwaMandateId; // From PayWithAccount

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MandateType mandateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MandateStatus status = MandateStatus.PENDING;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    // For instalments
    private Integer instalmentCount;
    private Integer instalmentsPaid = 0;

    @Column(precision = 19, scale = 2)
    private BigDecimal instalmentAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal downPayment;

    // Schedule
    @Enumerated(EnumType.STRING)
    private RepeatFrequency repeatFrequency;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime nextDebitDate;

    // Activation
    private String activationUrl; // From PWA
    private LocalDateTime activatedAt;

    @Column(columnDefinition = "TEXT")
    private String narration;

    @OneToMany(mappedBy = "mandate", cascade = CascadeType.ALL)
    private Set<PaymentTransaction> transactions = new HashSet<>();
}
