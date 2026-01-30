package trustrail.api.controller;

// ==================== ELIGIBILITY CONTROLLER ====================

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trustrail.api.dto.EligibilityCheckRequest;
import trustrail.api.dto.EligibilityCheckResponse;
import trustrail.api.entity.Business;
import trustrail.api.repo.BusinessRepo;
import trustrail.api.security.SecurityUtils;
import trustrail.api.service.EligibilityService;

@RestController
@RequestMapping("/eligibility")
@RequiredArgsConstructor
@Slf4j
public class EligibilityController {

    private final EligibilityService eligibilityService;
    private final BusinessRepo businessRepository;

    private Business getCurrentBusiness() {
        String email = SecurityUtils.getCurrentUserEmail();
        return businessRepository.findByNotificationEmail(email)
                .orElseThrow(() -> new RuntimeException("Business not found"));
    }

    /**
     * PRIMARY ENDPOINT: Check if customer is eligible for instalment/deferred payment
     * This is what CareFlow calls before offering payment options
     */
    @PostMapping("/check")
    public ResponseEntity<EligibilityCheckResponse> checkEligibility(
            @Valid @RequestBody EligibilityCheckRequest request
    ) {
        Business business = getCurrentBusiness();
        log.info("Eligibility check for customer {} in business {}",
                request.getExternalCustomerId(), business.getId());

        EligibilityCheckResponse response = eligibilityService.checkEligibility(business, request);
        return ResponseEntity.ok(response);
    }
}
