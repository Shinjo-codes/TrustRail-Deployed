package trustrail.api.service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PWACreateMerchantRequest {

    @JsonProperty("request_ref")
    private String requestRef;
    @JsonProperty("request_type")
    private String requestType = "create merchant";
    @JsonProperty("auth")
    private Auth auth;
    @JsonProperty("transaction")
    private Transaction transaction;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Auth {
        @JsonProperty("auth_provider")
        private String authProvider = "PaywithAccount";
        private String secure;
        private String type;
        @JsonProperty("route_mode")
        private String routeMode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Transaction {
        @JsonProperty("mock_mode")
        private String mockMode;
        @JsonProperty("transaction_ref")
        private String transactionRef;
        @JsonProperty("transaction_desc")
        private String transactionDesc;
        @JsonProperty("transaction_ref_parent")
        private String transactionRefParent;
        private Integer amount;
        private Customer customer;
        private Meta meta;
        private Details details;
        private Object options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Customer {
        @JsonProperty("customer_ref")
        private String customerRef;
        private String firstname;
        private String surname;
        private String email;
        @JsonProperty("mobile_no")
        private String mobileNo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Meta {
        private String beta;
        @JsonProperty("biller_sector")
        private String billerSector;
        @JsonProperty("simple_payment")
        private String simplePayment;
        @JsonProperty("webhook_url")
        private String webhookUrl;
        @JsonProperty("whatsapp_contact_name")
        private String whatsappContactName;
        @JsonProperty("whatsapp_contact_no")
        private String whatsappContactNo;
        @JsonProperty("business_short_name")
        private String businessShortName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Details {
        @JsonProperty("business_name")
        private String businessName;
        @JsonProperty("rc_number")
        private String rcNumber;
        @JsonProperty("settlement_account_no")
        private String settlementAccountNo;
        @JsonProperty("settlement_bank_code")
        private String settlementBankCode;
        private String tin;
        private String address;
        @JsonProperty("notification_phone_number")
        private String notificationPhoneNumber;
        @JsonProperty("notification_email")
        private String notificationEmail;
    }
}
