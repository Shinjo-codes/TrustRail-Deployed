package trustrail.api.controller;//package trustrail.api.controller;
//
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import trustrail.api.dto.*;
//import trustrail.api.entity.Business;
//import trustrail.api.repo.BusinessRepo;
//import trustrail.api.security.SecurityUtils;
//import trustrail.api.service.MandateCreationService;
//import trustrail.api.service.PaymentProcessingService;
//

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trustrail.api.dto.DebitRequest;
import trustrail.api.dto.PaymentTransactionResponse;
import trustrail.api.service.PaymentProcessingService;

///**
// * PAYMENT CONTROLLER
// *
// * Handles:
// * 1. Manual payment collection
// * 2. Subscription/instalment payments
// * 3. Mandate creation
// *
// * All operations are tied to the authenticated business (from JWT)
// */
//@RestController
//@RequestMapping("/payments")
//@RequiredArgsConstructor
//@Slf4j
//public class PaymentController {
//
//    private final PaymentProcessingService paymentService;
//    private final MandateCreationService mandateCreationService;
//    private final BusinessRepo businessRepository;
//
//    /**
//     * Get current business from authenticated user (JWT token)
//     * This is extracted from the security context
//     */
//    private Business getCurrentBusiness() {
//        String email = SecurityUtils.getCurrentUserEmail();
//        return businessRepository.findByEmail(email)
//                .orElseThrow(() -> {
//                    log.error("Business not found for email: {}", email);
//                    return new RuntimeException("Business not found");
//                });
//    }
//
//    // ==================== MANUAL COLLECTION ====================
//
//    /**
//     * POST /payments/collect
//     *
//     * Manually collect payment for a mandate
//     * Requires active mandate with valid mandate reference
//     *
//     * Request body:
//     * {
//     *   "mandateReference": "MDT-ABC123DE",
//     *   "amount": 10000.00
//     * }
//     *
//     * @param request Debit request with mandate reference and amount
//     * @return Payment transaction details
//     */
//    @PostMapping("/collect")
//    public ResponseEntity<PaymentTransactionResponse> collectPayment(
//            @Valid @RequestBody DebitRequest request
//    ) {
//        try {
//            log.info("Manual payment collection for mandate: {}", request.getMandateReference());
//
//            PaymentTransactionResponse response = paymentService.collectManualPayment(request);
//
//            log.info("Payment collected successfully: {}", response.getTransactionReference());
//            return ResponseEntity.ok(response);
//
//        } catch (IllegalArgumentException e) {
//            log.warn("Invalid payment request: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//
//        } catch (RuntimeException e) {
//            log.error("Payment collection failed", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    // ==================== SUBSCRIPTION / INSTALMENT ====================
//
//    /**
//     * POST /payments/subscription
//     *
//     * Process subscription or instalment payment for active mandate
//     * Requires mandate to be in ACTIVE status
//     *
//     * Request body:
//     * {
//     *   "mandateReference": "MDT-ABC123DE",
//     *   "amount": 16000.00
//     * }
//     *
//     * @param request Debit request with mandate reference and amount
//     * @return Payment transaction details
//     */
//    @PostMapping("/subscription")
//    public ResponseEntity<PaymentTransactionResponse> subscriptionPayment(
//            @Valid @RequestBody DebitRequest request
//    ) {
//        try {
//            log.info("Subscription payment for active mandate: {}", request.getMandateReference());
//
//            PaymentTransactionResponse response = paymentService.subscriptionPayment(request);
//
//            log.info("Subscription payment processed: {}", response.getTransactionReference());
//            return ResponseEntity.ok(response);
//
//        } catch (IllegalArgumentException e) {
//            log.warn("Invalid subscription payment request: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//
//        } catch (RuntimeException e) {
//            log.error("Subscription payment failed", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//
//    // ==================== MANDATE CREATION ====================
//
//    /**
//     * POST /payments/mandates
//     *
//     * Create a new mandate for a customer
//     * Business is extracted from authenticated user (JWT token)
//     *
//     * Request body:
//     * {
//     *   "externalCustomerId": "CUST-123",
//     *   "mandateType": "INSTALMENT",
//     *   "totalAmount": 100000.00,
//     *   "downPayment": 20000.00,
//     *   "instalmentCount": 5,
//     *   "repeatFrequency": "MONTHLY",
//     *   "narration": "Purchase of goods"
//     * }
//     *
//     * Response:
//     * {
//     *   "id": 1,
//     *   "mandateReference": "MDT-ABC123DE",
//     *   "pwaMandateId": "PWAMDT-001",
//     *   "activationUrl": "https://paywithaccount.com/activate/...",
//     *   "status": "PENDING",
//     *   "totalAmount": 100000.00,
//     *   ...
//     * }
//     *
//     * @param request Mandate creation request
//     * @return Created mandate with activation URL
//     */
//    @PostMapping("/mandates")
//    public ResponseEntity<MandateResponse> createMandate(
//            @Valid @RequestBody MandateCreationRequest request
//    ) {
//        try {
//            Business business = getCurrentBusiness();
//
//            log.info("Creating mandate for customer: {} in business: {}",
//                    request.getExternalCustomerId(), business.getId());
//
//            MandateResponse response = mandateCreationService.createMandate(business, request);
//
//            log.info("Mandate created successfully: {} - Customer should activate via: {}",
//                    response.getMandateReference(), response.getActivationUrl());
//
//            return ResponseEntity.status(HttpStatus.CREATED).body(response);
//
//        } catch (IllegalArgumentException e) {
//            log.warn("Invalid mandate creation request: {}", e.getMessage());
//            return ResponseEntity.badRequest().build();
//
//        } catch (RuntimeException e) {
//            log.error("Mandate creation failed", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//        }
//    }
//}

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentProcessingService paymentService;

    @PostMapping("/collect")
    public ResponseEntity<PaymentTransactionResponse> collectPayment(
            @Valid @RequestBody DebitRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.collectManualPayment(request)
        );
    }
}
