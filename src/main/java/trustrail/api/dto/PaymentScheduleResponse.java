package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class PaymentScheduleResponse {
    private LocalDateTime scheduledDate;
    private String customerName;
    private String mandateReference;
    private BigDecimal amount;
    private Integer instalmentNumber;
    private TransactionStatus status;
}
