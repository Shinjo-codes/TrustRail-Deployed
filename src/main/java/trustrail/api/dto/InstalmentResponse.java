package trustrail.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstalmentResponse {

    private String invoiceId;
    private String virtualAccountNumber;
    private BigDecimal totalAmount;
    private BigDecimal downPayment;
    private Integer instalmentCount;
    private String status;
    private String message;
}
