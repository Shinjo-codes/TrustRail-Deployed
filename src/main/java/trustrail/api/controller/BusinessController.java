package trustrail.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trustrail.api.dto.BusinessRegistrationRequest;
import trustrail.api.dto.BusinessRegistrationResponse;
import trustrail.api.entity.Business;
import trustrail.api.service.BusinessService;

@RestController
@RequestMapping("/businesses")
@RequiredArgsConstructor
@Slf4j
public class BusinessController {

    private final BusinessService businessService;

    @PostMapping("/register")
    public ResponseEntity<BusinessRegistrationResponse> register(
            @Valid @RequestBody BusinessRegistrationRequest request
    ) {
        log.info("Business registration request received for: {}", request.getBusinessName());

        Business business = businessService.registerBusiness(request);

        BusinessRegistrationResponse response = BusinessRegistrationResponse.builder()
                .businessId(business.getId())
                .businessName(business.getBusinessName())
                .notificationEmail(business.getNotificationEmail())
                .phoneNumber(business.getPhoneNumber())
                .pwaBusinessId(business.getPwaBusinessId())
                .status(business.getStatus())
                .message("Business registered successfully")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
