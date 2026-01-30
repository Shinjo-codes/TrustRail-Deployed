package trustrail.api.service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Bank Linking Request
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PWABankLinkRequest {

    @JsonProperty ("merchant_id")
    private String merchantId;
    @JsonProperty ("customer_email")
    private String customerEmail;
    @JsonProperty ("customer_phone")
    private String customerPhone;
    @JsonProperty ("callback_url")
    private String callbackUrl;
}
