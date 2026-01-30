package trustrail.api.service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Direct Debit Collection
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PWACollectRequest {

    @JsonProperty ("mandate_id")
    private String mandateId;
    private BigDecimal amount;
    private String narration;
}
