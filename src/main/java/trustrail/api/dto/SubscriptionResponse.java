package trustrail.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionResponse {

    private String invoiceId;
    private String virtualAccountNumber;
    private BigDecimal amount;
    private String status;
    private String message;
}

