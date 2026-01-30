package trustrail.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trustrail.api.dto.MandateCreationRequest;
import trustrail.api.dto.MandateResponse;
import trustrail.api.entity.Business;
import trustrail.api.repo.BusinessRepo;
import trustrail.api.security.SecurityUtils;
import trustrail.api.service.MandateCreationService;

import java.util.List;

/**
 * MANDATE CONTROLLER
 *
 * REST API for mandate operations:
 * - POST /mandates - Create new mandate
 * - GET /mandates - List all mandates for business
 * - GET /mandates/{reference} - Get specific mandate
 *
 * All operations are authenticated and tied to the authenticated user's business
 */
@RestController
@RequestMapping("/mandates")
@RequiredArgsConstructor
@Slf4j
public class MandateController {

    // MandateCreationService
    private final MandateCreationService mandateCreationService;
    private final BusinessRepo businessRepository;

    /**
     * Get current business from authenticated user (JWT token)
     * Extracts email from security context and looks up associated business
     */
    private Business getCurrentBusiness() {
        String email = SecurityUtils.getCurrentUserEmail();
        return businessRepository.findByNotificationEmail(email)
                .orElseThrow(() -> {
                    log.error("Business not found for email: {}", email);
                    return new RuntimeException("Business not found");
                });
    }

    /**
     * POST /mandates
     *
     * Create a new mandate for a customer
     * Business is extracted from JWT token (not from request body)
     *
     * @param request Mandate creation request with customer and terms
     * @return 201 Created with mandate details and activation URL
     */
    @PostMapping
    public ResponseEntity<MandateResponse> createMandate(
            @Valid @RequestBody MandateCreationRequest request
    ) {
        try {
            Business business = getCurrentBusiness();
            log.info("Creating mandate for customer {} in business {}",
                    request.getExternalCustomerId(), business.getId());

            // FIXED: Now calling mandateCreationService instead of mandateService
            MandateResponse response = mandateCreationService.createMandate(business, request);

            log.info("Mandate created successfully: {} - Activation URL: {}",
                    response.getMandateReference(), response.getActivationUrl());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid mandate creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (RuntimeException e) {
            log.error("Mandate creation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /mandates
     *
     * List all mandates for the authenticated business
     *
     * @return 200 OK with list of mandates
     */
    @GetMapping
    public ResponseEntity<List<MandateResponse>> listMandates() {
        try {
            Business business = getCurrentBusiness();
            log.info("Listing mandates for business: {}", business.getId());

            // Now calling correct method on mandateCreationService
            List<MandateResponse> mandates = mandateCreationService.getBusinessMandates(business);

            log.info("Found {} mandates for business: {}", mandates.size(), business.getId());
            return ResponseEntity.ok(mandates);

        } catch (RuntimeException e) {
            log.error("Failed to list mandates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /mandates/{mandateReference}
     *
     * Get a specific mandate by its TrustRail reference
     *
     * @param mandateReference The mandate reference (e.g., MDT-ABC123DE)
     * @return 200 OK with mandate details or 404 if not found
     */
    @GetMapping("/{mandateReference}")
    public ResponseEntity<MandateResponse> getMandate(
            @PathVariable String mandateReference
    ) {
        try {
            log.info("Retrieving mandate: {}", mandateReference);

            // FIXED: Now calling correct method on mandateCreationService
            MandateResponse mandate = mandateCreationService.getMandateByReference(mandateReference);

            log.info("Mandate retrieved: {}", mandateReference);
            return ResponseEntity.ok(mandate);

        } catch (RuntimeException e) {
            log.warn("Mandate not found: {}", mandateReference);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Failed to retrieve mandate: {}", mandateReference, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}