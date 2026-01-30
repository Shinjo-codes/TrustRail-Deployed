package trustrail.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PWA WEBHOOK PAYLOAD - MATCHES ACTUAL PWA STRUCTURE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PWAWebhookPayload {

    @JsonProperty("request_ref")
    private String requestRef;

    @JsonProperty("request_type")
    private String requestType; // Always "transaction_notification"

    @JsonProperty("requester")
    private String requester; // "Scheduler", "PaywithAccount", "NIBSS"

    @JsonProperty("mock_mode")
    private String mockMode;

    @JsonProperty("app_info")
    private AppInfo appInfo;

    @JsonProperty("details")
    private Details details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppInfo {
        @JsonProperty("app_code")
        private String appCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Details {

        @JsonProperty("status")
        private String status; // "Successful", "Failed"

        @JsonProperty("transaction_ref")
        private String transactionRef;

        @JsonProperty("transaction_type")
        private String transactionType; // "collect", "activate_mandate"

        @JsonProperty("amount")
        private Object amount; // Can be String or Integer

        @JsonProperty("customer_ref")
        private String customerRef;

        @JsonProperty("customer_email")
        private String customerEmail;

        @JsonProperty("customer_firstname")
        private String customerFirstname;

        @JsonProperty("customer_surname")
        private String customerSurname;

        @JsonProperty("customer_mobile_no")
        private String customerMobileNo;

        @JsonProperty("transaction_desc")
        private String transactionDesc;

        @JsonProperty("provider")
        private String provider;

        @JsonProperty("meta")
        private Map<String, Object> meta; // Contains event_type, mandate_id, etc.

        @JsonProperty("data")
        private WebhookData data;

        // Helper methods
        public String getEventType() {
            return meta != null ? (String) meta.get("event_type") : null;
        }

        public String getMandateId() {
            return meta != null ? (String) meta.get("mandate_id") : null; // Fixed: was "mandateId"
        }

        public String getBillerCode() {
            return meta != null ? (String) meta.get("biller_code") : null;
        }

        public String getAccountNo() {
            return meta != null ? (String) meta.get("account_no") : null;
        }

        public String getPaymentId() {
            return meta != null ? (String) meta.get("payment_id") : null;
        }

        public BigDecimal getAmountAsBigDecimal() {
            if (amount == null) return BigDecimal.ZERO;
            if (amount instanceof Number) {
                return BigDecimal.valueOf(((Number) amount).doubleValue());
            }
            if (amount instanceof String) {
                return new BigDecimal((String) amount);
            }
            return BigDecimal.ZERO;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookData {  // Renamed from "Data" to avoid confusion
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("data")
        private MandateData data;

        @JsonProperty("type")
        private String type;

        @JsonProperty("timestamp")
        private String timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MandateData {
        @JsonProperty("id")
        private Integer id; // PWA mandate ID

        @JsonProperty("amount")
        private String amount;

        @JsonProperty("status")
        private String status; // "active"

        @JsonProperty("end_date")
        private String endDate;

        @JsonProperty("frequency")
        private String frequency;

        @JsonProperty("reference")
        private String reference;

        @JsonProperty("start_date")
        private String startDate;
    }
}