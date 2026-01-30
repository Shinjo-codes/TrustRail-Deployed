package trustrail.api.entity;

import jakarta.persistence.*;
import lombok.*;
import trustrail.api.entity.enums.TransactionStatus;
import trustrail.api.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ==================== PAYMENT TRANSACTION ENTITY ====================
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction extends Base {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mandate_id", nullable = false)
    private Mandate mandate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(unique = true, nullable = false)
    private String transactionReference;

    @Column(unique = true)
    private String pwaTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    private Integer instalmentNumber; // For tracking which instalment this is

    private LocalDateTime scheduledDate;
    private LocalDateTime executedDate;

    @Column(columnDefinition = "TEXT")
    private String failureReason;

    private Integer retryCount = 0;
    private LocalDateTime nextRetryDate;

    @Column(columnDefinition = "TEXT")
    private String pwaWebhookPayload; // Store webhook data for audit
}

