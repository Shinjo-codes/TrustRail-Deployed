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
public class PWABankLinkResponse {

    @JsonProperty("linking_token")
    private String linkingToken;

    @JsonProperty("bank_selection_url")
    private String bankSelectionUrl;
    private String status;
    private String message;
}
