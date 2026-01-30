package trustrail.api.service.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
 public class PWAInvoiceResponse {
    private String invoiceId;
    private String virtualAccountNumber;
    private String status;
    private String message;
}
