package trustrail.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trustrail.api.dto.PWAWebhookPayload;
import trustrail.api.service.WebhookService;

/**
 * PWA WEBHOOK CONTROLLER
 *
 * Receives payment event notifications from PayWithAccount.
 * No signature verification required as per PWA documentation.
 */
@RestController
@RequestMapping("/webhooks/pwa")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final WebhookService webhookService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<WebhookResponse> handleWebhook(@RequestBody String rawPayload) {
        log.info("Received PWA webhook");
        log.debug("Raw payload: {}", rawPayload);

        try {
            // Parse webhook payload
            PWAWebhookPayload payload = objectMapper.readValue(
                    rawPayload,
                    PWAWebhookPayload.class
            );

            // Validate payload
            if (!"transaction_notification".equals(payload.getRequestType())) {
                log.warn("Unknown request_type: {}", payload.getRequestType());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new WebhookResponse("error", "Unknown request_type"));
            }

            String eventType = payload.getDetails().getEventType();
            String transactionType = payload.getDetails().getTransactionType();

            log.info("Processing webhook: event_type={}, transaction_type={}, transaction_ref={}",
                    eventType, transactionType, payload.getDetails().getTransactionRef());

            // Route based on event_type and transaction_type
            if (eventType != null) {
                // Event-based routing (for debit, biller events)
                routeByEventType(payload, eventType);
            } else if (transactionType != null) {
                // Transaction type routing (for mandate activation)
                routeByTransactionType(payload, transactionType);
            } else {
                log.warn("No event_type or transaction_type found in webhook");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new WebhookResponse("error", "Missing event identifiers"));
            }

            return ResponseEntity.ok(new WebhookResponse("success", "Webhook processed successfully"));

        } catch (Exception e) {
            log.error("Webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new WebhookResponse("error", "Webhook processing failed: " + e.getMessage()));
        }
    }

    private void routeByEventType(PWAWebhookPayload payload, String eventType) {
        switch (eventType) {
            case "debit" -> {
                if ("Successful".equalsIgnoreCase(payload.getDetails().getStatus())) {
                    webhookService.handlePaymentSuccess(payload);
                    log.info("Debit success processed: {}", payload.getDetails().getTransactionRef());
                } else {
                    webhookService.handlePaymentFailure(payload);
                    log.info("Debit failure processed: {}", payload.getDetails().getTransactionRef());
                }
            }
            case "biller_approved" -> {
                webhookService.handleBillerApproved(payload);
                log.info("Biller approved: {}", payload.getDetails().getBillerCode());
            }
            case "biller_rejected" -> {
                webhookService.handleBillerRejected(payload);
                log.info("Biller rejected: {}", payload.getDetails().getBillerCode());
            }
            case "biller_deactivated" -> {
                webhookService.handleBillerDeactivated(payload);
                log.info("Biller deactivated: {}", payload.getDetails().getBillerCode());
            }
            default -> log.warn("Unknown event_type: {}", eventType);
        }
    }

    private void routeByTransactionType(PWAWebhookPayload payload, String transactionType) {
        if ("activate_mandate".equals(transactionType)) {
            webhookService.handleMandateActivation(payload);
            log.info("Mandate activation processed: {}", payload.getDetails().getTransactionRef());
        } else {
            log.warn("Unknown transaction_type: {}", transactionType);
        }
    }

    // Response DTO
    public record WebhookResponse(String status, String message) {}
}