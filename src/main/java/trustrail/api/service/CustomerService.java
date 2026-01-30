package trustrail.api.service;

import trustrail.api.dto.*;
import trustrail.api.entity.*;
import trustrail.api.entity.enums.TrustState;
import trustrail.api.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepo customerRepository;
    private final BusinessRepo businessRepository;
    private final TrustProfileRepo trustProfileRepository;

    /**
     * Create a new customer for a business
     * Now accepts pre-encrypted data from PWA tool
     */
    @Transactional
    public CustomerResponse createCustomer(Business business, CustomerCreationRequest request) {
        log.info("Creating customer for business {}: {}", business.getId(), request.getEmail());

        // Check if customer already exists
        if (customerRepository.existsByBusinessAndExternalCustomerId(
                business, request.getExternalCustomerId())) {
            throw new RuntimeException("Customer with this external ID already exists");
        }

        // Create customer with encrypted values
        Customer customer = Customer.builder()
                .business(business)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .externalCustomerId(request.getExternalCustomerId())
                // Plain account details (for display/reference)
                .linkedAccountNumber(request.getLinkedAccountNumber())
                .linkedBankCode(request.getLinkedBankCode())
                .linkedBankName(request.getLinkedBankName())
                // Store pre-encrypted values
                .encryptedSecure(request.getEncryptedSecure())
                .encryptedBvn(request.getEncryptedBvn())
                .bvn(request.getBvn())
                // Set accountLinked to true if encrypted data is provided
                .accountLinked(request.getEncryptedSecure() != null &&
                        !request.getEncryptedSecure().isEmpty())
                .build();

        customer = customerRepository.save(customer);

        // Create initial trust profile
        TrustProfile trustProfile = TrustProfile.builder()
                .customer(customer)
                .behavioralScore(50)
                .contextualScore(50)
                .progressiveScore(50)
                .overallTrustScore(50)
                .trustState(TrustState.NEW)
                .totalPayments(0)
                .successfulPayments(0)
                .failedPayments(0)
                .consecutiveFailures(0)
                .totalAmountPaid(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .eligibleForInstalments(true)
                .eligibleForDeferred(true)
                .maxEligibleAmount(new BigDecimal("100000.00"))
                .build();

        trustProfile = trustProfileRepository.save(trustProfile);

        log.info("Customer created with ID: {} (encrypted: {})",
                customer.getId(), customer.getAccountLinked());

        return mapToCustomerResponse(customer, trustProfile);
    }

    /**
     * Get customer by external ID
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Business business, String externalCustomerId) {
        Customer customer = customerRepository
                .findByBusinessAndExternalCustomerId(business, externalCustomerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        TrustProfile trustProfile = trustProfileRepository.findByCustomer(customer)
                .orElse(null);

        return mapToCustomerResponse(customer, trustProfile);
    }

    /**
     * List all customers for a business
     */
    @Transactional(readOnly = true)
    public java.util.List<CustomerResponse> listCustomers(Business business) {
        return customerRepository.findByBusiness(business).stream()
                .map(customer -> {
                    TrustProfile profile = trustProfileRepository.findByCustomer(customer)
                            .orElse(null);
                    return mapToCustomerResponse(customer, profile);
                })
                .toList();
    }

    // Helper method to map customer to response
    private CustomerResponse mapToCustomerResponse(Customer customer, TrustProfile trustProfile) {
        TrustProfileResponse profileResponse = null;

        if (trustProfile != null) {
            profileResponse = TrustProfileResponse.builder()
                    .id(trustProfile.getId())
                    .behavioralScore(trustProfile.getBehavioralScore())
                    .contextualScore(trustProfile.getContextualScore())
                    .progressiveScore(trustProfile.getProgressiveScore())
                    .overallTrustScore(trustProfile.getOverallTrustScore())
                    .trustState(trustProfile.getTrustState())
                    .totalPayments(trustProfile.getTotalPayments())
                    .successfulPayments(trustProfile.getSuccessfulPayments())
                    .failedPayments(trustProfile.getFailedPayments())
                    .consecutiveFailures(trustProfile.getConsecutiveFailures())
                    .totalAmountPaid(trustProfile.getTotalAmountPaid())
                    .outstandingBalance(trustProfile.getOutstandingBalance())
                    .eligibleForInstalments(trustProfile.getEligibleForInstalments())
                    .eligibleForDeferred(trustProfile.getEligibleForDeferred())
                    .maxEligibleAmount(trustProfile.getMaxEligibleAmount())
                    .lastPaymentDate(trustProfile.getLastPaymentDate())
                    .build();
        }

        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .externalCustomerId(customer.getExternalCustomerId())
                .accountLinked(customer.getAccountLinked())
                .linkedBankName(customer.getLinkedBankName())
                .trustProfile(profileResponse)
                .createdAt(customer.getCreatedAt())
                .build();
    }
}