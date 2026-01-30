package trustrail.api.controller;

// ==================== CUSTOMER CONTROLLER ====================

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trustrail.api.dto.CustomerCreationRequest;
import trustrail.api.dto.CustomerResponse;
import trustrail.api.entity.Business;
import trustrail.api.repo.BusinessRepo;
import trustrail.api.security.SecurityUtils;
import trustrail.api.service.CustomerService;

import java.util.List;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final BusinessRepo businessRepository;

    private Business getCurrentBusiness() {
        String email = SecurityUtils.getCurrentUserEmail();
        return businessRepository.findByNotificationEmail(email)
                .orElseThrow(() -> new RuntimeException("Business not found"));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerCreationRequest request
    ) {
        Business business = getCurrentBusiness();
        log.info("Creating customer for business {}: {}", business.getId(), request.getEmail());

        CustomerResponse response = customerService.createCustomer(business, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{externalCustomerId}")
    public ResponseEntity<CustomerResponse> getCustomer(
            @PathVariable String externalCustomerId
    ) {
        Business business = getCurrentBusiness();
        CustomerResponse response = customerService.getCustomer(business, externalCustomerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> listCustomers() {
        Business business = getCurrentBusiness();
        List<CustomerResponse> customers = customerService.listCustomers(business);
        return ResponseEntity.ok(customers);
    }
}
