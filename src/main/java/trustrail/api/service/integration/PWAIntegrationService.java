package trustrail.api.service.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import trustrail.api.entity.Business;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * PWA INTEGRATION SERVICE - UNIFIED ENDPOINT ARCHITECTURE
 *
 * PWA uses ONE endpoint for ALL services: /v2/transact
 * The 'request_type' field determines which service is called
 */
@Service
@Slf4j
public class PWAIntegrationService {

    private final WebClient webClient;
    private final String apiKey;
    private final String secretKey;
    private final ObjectMapper objectMapper;

    // Base URL: https://api.dev.onepipe.io
    // Endpoint: /v2/transact (same for ALL requests)
    private static final String TRANSACT_ENDPOINT = "/v2/transact";

    public PWAIntegrationService(
            @Value("${pwa.base-url}") String baseUrl,
            @Value("${pwa.api-key}") String apiKey,
            @Value("${pwa.secret-key}") String secretKey,
            @Value("${pwa.biller-code}") String billerCode,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.objectMapper = objectMapper;

        log.info("PWA Integration initialized with base URL: {}", baseUrl);
    }

    // ==================== CORE REQUEST METHOD ====================

    /**
     * Execute PWA request with signature
     * ALL PWA calls go through this method
     */
    private PWAUnifiedResponse executeRequest(PWAUnifiedRequest request) {
        log.info("Executing PWA request: {}", request.getRequestType());

        String signature = generateSignature(request.getRequestRef());

        PWAUnifiedResponse response = webClient.post()
                .uri(TRANSACT_ENDPOINT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Signature", signature)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PWAUnifiedResponse.class)
                .block();

        log.info("PWA response: status={}, message={}",
                response.getStatus(), response.getMessage());

        return response;
    }


    /**
     * Generate MD5 signature for request
     */


    private String generateSignature(String requestRef) {
        try {
            String raw = requestRef + secretKey;

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PWA MD5 signature", e);
        }
    }

    // ==================== BANK ACCOUNT LINKING ====================

    public PWABankLinkResponse getBankLinkingUrl(PWABankLinkRequest request) {
        try {
            PWAUnifiedRequest unifiedRequest = PWAUnifiedRequest.builder()
                    .requestRef(generateRequestRef())
                    .requestType("get_bank") // PWA bank linking request
                    .auth(PWAAuth.builder()
                            .type("bank.account")
                            .authProvider("PaywithAccount")
                            .build())
                    .transaction(PWATransaction.builder()
                            .mockMode("Live")
                            .transactionRef(generateTransactionRef())
                            .transactionDesc("Bank account linking")
                            .amount(BigDecimal.ZERO)
                            .meta(Map.of(
                                    "merchant_id", request.getMerchantId(),
                                    "customer_email", request.getCustomerEmail(),
                                    "customer_phone", request.getCustomerPhone(),
                                    "callback_url", request.getCallbackUrl()
                            ))
                            .details(new HashMap<>())
                            .build())
                    .build();

            PWAUnifiedResponse response = executeRequest(unifiedRequest);

            return PWABankLinkResponse.builder()
                    .linkingToken(
                            response.getData() != null
                                    ? response.getData().get("linking_token")
                                    : null
                    )
                    .bankSelectionUrl(
                            response.getData() != null
                                    ? response.getData().get("bank_selection_url")
                                    : null
                    )
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Failed to initiate bank linking", e);
            throw new RuntimeException("Bank linking failed: " + e.getMessage());
        }
    }


    // ==================== CREATE MANDATE ====================

    public PWAMandateCreationResult createMandate(
            String customerRef,
            String firstName,
            String surname,
            String email,
            String mobileNo,
            String encryptedSecure,
            String encryptedBvn,
            String billerCode,
            BigDecimal amount
    ) {
        try {
            PWAUnifiedRequest request = PWAUnifiedRequest.builder()
                    .requestRef(generateRequestRef())
                    .requestType("create mandate")
                    .auth(PWAAuth.builder()
                            .type("bank.account")
                            .secure(encryptedSecure)
                            .authProvider("PaywithAccount")
                            .build())
                    .transaction(PWATransaction.builder()
                            .mockMode("Live")
                            .transactionRef(generateTransactionRef())
                            .transactionDesc("Creating a mandate")
                            .amount(BigDecimal.ZERO)
                            .customer(PWACustomer.builder()
                                    .customerRef(customerRef)
                                    .firstname(firstName)
                                    .surname(surname)
                                    .email(email)
                                    .mobileNo(mobileNo)
                                    .build())
                            .meta(Map.of(
                                    "amount", amount.toString(),
                                    "skip_consent", "true",
                                    "bvn", encryptedBvn,
                                    "biller_code", billerCode,
                                    "customer_consent", "https://paywithaccount.com/consent_template.pdf"
                            ))
                            .details(new HashMap<>())
                            .build())
                    .build();

            PWAUnifiedResponse response = executeRequest(request);

            return PWAMandateCreationResult.builder()
                    .mandateId(extractFromResponse(response, "mandate_id"))
                    .activationUrl(extractFromResponse(response, "activation_url"))
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create mandate", e);
            throw new RuntimeException("Mandate creation failed: " + e.getMessage());
        }
    }

//    public PWACreateMerchantResponse createMerchant(Business business) {
//
//        PWACreateMerchantPayload payload =
//                PWACreateMerchantPayload.builder()
//                        .requestRef(generateRequestRef())
//                        .businessName(business.getBusinessName())
//                        .email(business.getEmail())
//                        .phone(business.getPhoneNumber())
//                        .settlementAccount(
//                                new PWACreateMerchantPayload.SettlementAccount(
//                                        business.getSettlementAccountNumber(),
//                                        business.getSettlementBankCode(),
//                                        business.getSettlementAccountName()
//                                )
//                        )
//                        .build();
//
//        return pwaClient.sendTransaction(request);
//    }

//    public PWACreateMerchantResponse createMerchant(Business business) {
//
//        // Build the transaction details from your Business entity
//        PWACreateMerchantRequest.Details details = PWACreateMerchantRequest.Details.builder()
//                .businessName(business.getBusinessName())
//                .rcNumber(business.getRcNumber())
//                .settlementAccountNo(business.getSettlementAccountNumber())
//                .settlementBankCode(business.getSettlementBankCode())
//                .tin(business.getTin())
//                .address(business.getAddress())
//                .notificationPhoneNumber(business.getNotificationPhoneNumber())
//                .notificationEmail(business.getNotificationEmail())
//                .build();
//
//        // Build the meta section
//        PWACreateMerchantRequest.Meta meta = PWACreateMerchantRequest.Meta.builder()
//                .businessShortName(business.getBusinessShortName())
//                .billerSector("Aggregattor")       // Example value, adjust as needed
//                .simplePayment("enabled")         // Example value
//                .beta("enabled")                  // Example value
//                .webhookUrl("https://yourapp.com/pwa-webhook") // Your webhook URL
//                .whatsappContactName(business.getWhatsappContactName())
//                .whatsappContactNo(business.getWhatsappContactNumber())
//                .build();
//
//        // Build the transaction object
//        PWACreateMerchantRequest.Transaction transaction = PWACreateMerchantRequest.Transaction.builder()
//                .transactionRef("TR-TXN-" + System.currentTimeMillis())  // Unique transaction ref
//                .transactionDesc("Creating merchant account")
//                .mockMode("Live")  // Or "Inspect" depending on environment
//                .details(details)
//                .meta(meta)
//                .amount(0) // Create merchant doesn’t require an amount
//                .build();
//
//        // Build the auth object
//        PWACreateMerchantRequest.Auth auth = PWACreateMerchantRequest.Auth.builder()
//                .authProvider("PaywithAccount")
//                .type("bank.account")
//                .build();
//
//        // Build the full PWA request
//        PWACreateMerchantRequest request = PWACreateMerchantRequest.builder()
//                .requestRef("TR-REQ-" + System.currentTimeMillis()) // Unique request ref
//                .requestType("create merchant")
//                .auth(auth)
//                .transaction(transaction)
//                .build();
//
//        // Send request through Feign client
//        return pwaClient.sendTransaction(request);
//    }

    public PWACreateMerchantResponse createMerchant(Business business) {
        try {
            PWAUnifiedRequest request = PWAUnifiedRequest.builder()
                    .requestRef(generateRequestRef())
                    .requestType("create merchant")
                    .auth(PWAAuth.builder()
                            .authProvider("PaywithAccount")
                            .type("bank.account")
                            .build())
                    .transaction(PWATransaction.builder()
                            .mockMode("Live")
                            .transactionRef(generateTransactionRef())
                            .transactionDesc("Applying for a new merchant account")
                            .amount(BigDecimal.ZERO)
                            .customer(PWACustomer.builder()
                                    .customerRef(business.getPhoneNumber())
                                    .firstname(business.getBusinessName())
                                    .surname(business.getBusinessName())
                                    .email(business.getNotificationEmail())
                                    .mobileNo(business.getPhoneNumber())
                                    .build())
                            .meta(Map.of(
                                    "beta", "enabled",
                                    "biller_sector", "Aggregator",
                                    "simple_payment", "enabled",
                                    "webhook_url", "https://yourapp.com/pwa-webhook",
                                    "whatsapp_contact_name", business.getWhatsappContactName(),
                                    "whatsapp_contact_no", business.getWhatsappContactNumber(),
                                    "business_short_name", business.getBusinessShortName()
                            ))
                            .details(Map.of(
                                    "business_name", business.getBusinessName(),
                                    "rc_number", business.getRcNumber(),
                                    "settlement_account_no", business.getSettlementAccountNumber(),
                                    "settlement_bank_code", business.getSettlementBankCode(),
                                    "tin", business.getTin(),
                                    "address", business.getAddress(),
                                    "notification_phone_number", business.getNotificationPhoneNumber(),
                                    "notification_email", business.getNotificationEmail()
                            ))
                            .build())
                    .build();

            PWAUnifiedResponse response = executeRequest(request);

            // ADD DEBUGGING
            log.info("PWA Raw Response: {}", response);
            log.info("PWA Response Data Map: {}", response.getData());

            String merchantId = extractFromResponse(response, "merchant_id");
            String billerCode = extractFromResponse(response, "biller_code");

            log.info("Extracted merchantId: {}", merchantId);
            log.info("Extracted billerCode: {}", billerCode);

            return PWACreateMerchantResponse.builder()
                    .merchantId(extractFromResponse(response, "merchant_id"))
                    .billerCode(extractFromResponse(response, "biller_code"))  //
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Failed to create merchant", e);
            throw new RuntimeException("Create merchant failed: " + e.getMessage());
        }
    }


    // ==================== SEND INVOICE (INSTALMENT) ====================

    public PWAInvoiceResponse sendInstalmentInvoice(
            String customerRef,
            String firstName,
            String surname,
            String email,
            String mobileNo,
            String encryptedSecure,
            String billerCode,
            BigDecimal totalAmount,
            BigDecimal downPayment,
            Integer instalmentCount,
            String repeatFrequency,
            String repeatStartDate
    ) {
        try {
            PWAUnifiedRequest request = PWAUnifiedRequest.builder()
                    .requestRef(generateRequestRef())
                    .requestType("send invoice")
                    .auth(PWAAuth.builder()
                            .type("bank.account")
                            .secure(encryptedSecure)
                            .authProvider("PaywithAccount")
                            .build())
                    .transaction(PWATransaction.builder()
                            .mockMode("Live")
                            .transactionRef(generateTransactionRef())
                            .transactionDesc("Instalment payment plan")
                            .amount(totalAmount)
                            .customer(PWACustomer.builder()
                                    .customerRef(customerRef)
                                    .firstname(firstName)
                                    .surname(surname)
                                    .email(email)
                                    .mobileNo(mobileNo)
                                    .build())
                            .meta(Map.of(
                                    "type", "instalment",
                                    "down_payment", downPayment.toString(),
                                    "number_of_payments", instalmentCount.toString(),
                                    "repeat_frequency", repeatFrequency,
                                    "repeat_start_date", repeatStartDate,
                                    "biller_code", billerCode
                            ))

                            .details(new HashMap<>())
                            .build())
                    .build();

            PWAUnifiedResponse response = executeRequest(request);

            return PWAInvoiceResponse.builder()
                    .invoiceId(extractFromResponse(response, "invoice_id"))
                    .virtualAccountNumber(extractFromResponse(response, "virtual_account"))
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Failed to send instalment invoice", e);
            throw new RuntimeException("Instalment invoice failed: " + e.getMessage());
        }
    }

    //SEND SUBSCRIPTION//
    public PWAInvoiceResponse sendSubscriptionInvoice(
            String customerRef,
            String firstName,
            String surname,
            String email,
            String mobileNo,
            String encryptedSecure,
            String billerCode,
            BigDecimal amount,
            String repeatFrequency,
            String repeatStartDate,
            String repeatEndDate
    ) {
        try {
            PWAUnifiedRequest request = PWAUnifiedRequest.builder()
                    .requestRef(generateRequestRef())
                    .requestType("send invoice")
                    .auth(PWAAuth.builder()
                            .type("bank.account")
                            .secure(encryptedSecure)
                            .authProvider("PaywithAccount")
                            .build())
                    .transaction(PWATransaction.builder()
                            .mockMode("Live")
                            .transactionRef(generateTransactionRef())
                            .transactionDesc("Setup subscription payment")
                            .amount(amount)
                            .customer(PWACustomer.builder()
                                    .customerRef(customerRef)
                                    .firstname(firstName)
                                    .surname(surname)
                                    .email(email)
                                    .mobileNo(mobileNo)
                                    .build())
                            .meta(Map.of(
                                    "type", "subscription",
                                    "repeat_frequency", repeatFrequency,
                                    "repeat_start_date", repeatStartDate,
                                    "repeat_end_date", repeatEndDate,
                                    "biller_code", billerCode
                            ))
                            .details(new HashMap<>())
                            .build())
                    .build();

            PWAUnifiedResponse response = executeRequest(request);

            return PWAInvoiceResponse.builder()
                    .invoiceId(extractFromResponse(response, "invoice_id"))
                    .virtualAccountNumber(extractFromResponse(response, "virtual_account"))
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Failed to send subscription invoice", e);
            throw new RuntimeException("Subscription invoice failed: " + e.getMessage());
        }
    }


    // ==================== COLLECT PAYMENT ====================

    public PWACollectResponse collectPayment(
            String customerRef,
            String firstName,
            String surname,
            String email,
            String mobileNo,
            String encryptedSecure,
            String billerCode,
            String mandateId,
            BigDecimal amount,
            String narration
    ) {
        try {
            PWAUnifiedRequest request = PWAUnifiedRequest.builder()
                    .requestRef(generateRequestRef())
                    .requestType("collect")
                    .auth(PWAAuth.builder()
                            .type("bank.account")
                            .secure(encryptedSecure)
                            .authProvider("PaywithAccount")
                            .build())
                    .transaction(PWATransaction.builder()
                            .mockMode("Live")
                            .transactionRef(generateTransactionRef())
                            .transactionDesc(narration)
                            .amount(amount)
                            .customer(PWACustomer.builder()
                                    .customerRef(customerRef)
                                    .firstname(firstName)
                                    .surname(surname)
                                    .email(email)
                                    .mobileNo(mobileNo)
                                    .build())
                            .meta(Map.of(
                                    "mandate_id", mandateId,
                                    "biller_code", billerCode
                            ))
                            .details(new HashMap<>())
                            .build())
                    .build();

            PWAUnifiedResponse response = executeRequest(request);

            return PWACollectResponse.builder()
                    .transactionId(response.getData() != null ?
                            response.getData().get("transaction_id") : null)
                    .status(response.getStatus())
                    .message(response.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("Failed to collect payment", e);
            throw new RuntimeException("Payment collection failed: " + e.getMessage());
        }
    }

    // ==================== HELPER METHODS ====================

    private String generateRequestRef() {
        return "TR-REQ-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateTransactionRef() {
        return "TR-TXN-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String extractFromResponse(PWAUnifiedResponse response, String key) {
        if (response.getData() != null) {
            return response.getData().get(key);
        }
        return null;
    }
}

// ==================== PWA UNIFIED REQUEST DTOs ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PWAUnifiedRequest {

    @JsonProperty("request_ref")
    private String requestRef;

    @JsonProperty("request_type")
    private String requestType;  // "create mandate", "send_invoice", "collect", etc.

    @JsonProperty("auth")
    private PWAAuth auth;

    @JsonProperty("transaction")
    private PWATransaction transaction;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PWAAuth {

    @JsonProperty("type")
    private String type;  // "bank.account"

    @JsonProperty("secure")
    private String secure;  // Encrypted secure field

    @JsonProperty("auth_provider")
    private String authProvider;  // "PaywithAccount"
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PWATransaction {

    @JsonProperty("mock_mode")
    private String mockMode;  // "Live" or "Inspect"

    @JsonProperty("transaction_ref")
    private String transactionRef;

    @JsonProperty("transaction_desc")
    private String transactionDesc;

    @JsonProperty("transaction_ref_parent")
    private String transactionRefParent;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("customer")
    private PWACustomer customer;

    @JsonProperty("meta")
    private Map<String, String> meta;

    @JsonProperty("details")
    private Map<String, Object> details;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PWACustomer {

    @JsonProperty("customer_ref")
    private String customerRef;

    @JsonProperty("firstname")
    private String firstname;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("email")
    private String email;

    @JsonProperty("mobile_no")
    private String mobileNo;
}

// ==================== PWA UNIFIED RESPONSE ====================

@Data
@NoArgsConstructor
@AllArgsConstructor
class PWAUnifiedResponse {

    @JsonProperty("status")
    private String status;  // "Successful", "Failed"

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private Map<String, String> data;

    @JsonProperty("request_ref")
    private String requestRef;

    @JsonProperty("transaction_ref")
    private String transactionRef;
}

// ==================== LEGACY RESPONSE DTOs (for backward compatibility) ====================

