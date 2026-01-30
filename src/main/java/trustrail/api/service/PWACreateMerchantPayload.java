package trustrail.api.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PWACreateMerchantPayload {

    @JsonProperty("request_ref")
    private String requestRef;
    @JsonProperty("business_name")
    private String businessName;
    private String email;
    private String phone;

    @JsonProperty("settlement_account")
    private SettlementAccount settlementAccount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementAccount {
        @JsonProperty("account_number")
        private String accountNumber;
        @JsonProperty("bank_code")
        private String bankCode;
        @JsonProperty("account_name")
        private String accountName;
    }
}

