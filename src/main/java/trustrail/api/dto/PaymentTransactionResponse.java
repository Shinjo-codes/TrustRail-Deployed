package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import trustrail.api.entity.PaymentTransaction;
import trustrail.api.entity.enums.TransactionStatus;
import trustrail.api.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class PaymentTransactionResponse {
    private Long id;
    private String transactionReference;
    private String pwaTransactionId;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private Integer instalmentNumber;
    private LocalDateTime scheduledDate;
    private LocalDateTime executedDate;
    private String failureReason;
    private MandateResponse mandate;

    public static PaymentTransactionResponse from(PaymentTransaction tx) {
        return PaymentTransactionResponse.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .pwaTransactionId(tx.getPwaTransactionId())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .failureReason(tx.getFailureReason())
                .scheduledDate(tx.getScheduledDate())
                .executedDate(tx.getExecutedDate())
                .build();
    }


}
