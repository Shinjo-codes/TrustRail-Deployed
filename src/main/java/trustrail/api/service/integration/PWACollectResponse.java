package trustrail.api.service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PWACollectResponse {
    @JsonProperty("transaction_id")
    private String transactionId;
    private String status; // PENDING, SUCCESSFUL, FAILED
    private String message;
}
