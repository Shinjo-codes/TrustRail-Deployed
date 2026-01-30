package trustrail.api.service;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.dto.accountlinking.AccountLinkingResponseDTO;
import trustrail.api.entity.Business;
import trustrail.api.entity.Customer;
import trustrail.api.repo.CustomerRepo;
import trustrail.api.service.integration.PWABankLinkRequest;
import trustrail.api.service.integration.PWABankLinkResponse;
import trustrail.api.service.integration.PWAIntegrationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// ==================== ACCOUNT LINKING SERVICE ====================

/**
 * ACCOUNT LINKING SERVICE
 * Handles PWA bank account linking flow
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountLinkingService {

    private final PWAIntegrationService pwaService;
    private final CustomerRepo customerRepository;

    /**
     * Initiate account linking for customer
     * Returns URL for customer to select bank and authenticate
     */
    @Transactional
    public AccountLinkingResponseDTO initiateAccountLinking(
            Business business,
            String externalCustomerId
    ) {
        Customer customer = customerRepository
                .findByBusinessAndExternalCustomerId(business, externalCustomerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Call PWA Get_bank endpoint
        PWABankLinkRequest request = PWABankLinkRequest.builder()
                .merchantId(business.getPwaBusinessId())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhoneNumber())
                .callbackUrl("https://trustrail.app/callback/account-linked")
                .build();

        PWABankLinkResponse response = pwaService.getBankLinkingUrl(request);

        // Store temporary linking token
        customer.setPwaAccountReference(response.getLinkingToken());
        customerRepository.save(customer);

        return AccountLinkingResponseDTO.builder()
                .linkingUrl(response.getBankSelectionUrl())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    /**
     * Complete account linking after customer authenticates
     * Called via callback from PWA
     */
    @Transactional
    public void completeAccountLinking(
            String linkingToken,
            String accountNumber,
            String bankCode,
            String bankName
    ) {
        Customer customer = customerRepository
                .findByPwaAccountReference(linkingToken)
                .orElseThrow(() -> new RuntimeException("Linking token not found"));

        customer.setLinkedAccountNumber(accountNumber);
        customer.setLinkedBankCode(bankCode);
        customer.setLinkedBankName(bankName);
        customer.setAccountLinked(true);

        customerRepository.save(customer);

        log.info("Account linked successfully for customer {}: {}",
                customer.getId(), bankName);
    }
}
