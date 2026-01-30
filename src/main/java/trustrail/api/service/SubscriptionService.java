package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import trustrail.api.dto.*;
import trustrail.api.entity.*;
import trustrail.api.entity.enums.*;
import trustrail.api.repo.*;
import trustrail.api.service.integration.PWAIntegrationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final MandateRepo mandateRepo;
    private final PaymentTransactionRepo transactionRepo;
    private final PWAIntegrationService pwaIntegrationService;

    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {

        Mandate mandate = mandateRepo.findByMandateReference(request.getMandateReference())
                .orElseThrow(() -> new IllegalArgumentException("Invalid mandate reference"));

        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            throw new IllegalStateException("Mandate must be ACTIVE");
        }

        var pwaResponse = pwaIntegrationService.sendSubscriptionInvoice(
                mandate.getCustomer().getExternalCustomerId(),
                mandate.getCustomer().getFirstName(),
                mandate.getCustomer().getLastName(),
                mandate.getCustomer().getEmail(),
                mandate.getCustomer().getPhoneNumber(),
                mandate.getCustomer().getEncryptedSecure(),
                mandate.getBusiness().getBillerCode(),
                request.getAmount(),
                request.getRepeatFrequency(),
                request.getRepeatStartDate(),
                request.getRepeatEndDate()
        );

        PaymentTransaction tx = PaymentTransaction.builder()
                .mandate(mandate)
                .customer(mandate.getCustomer())
                .transactionReference(pwaResponse. getInvoiceId())
                .transactionType(TransactionType.SUBSCRIPTION_PAYMENT)
                .status(TransactionStatus.PENDING)
                .amount(request.getAmount())
                .build();

        transactionRepo.save(tx);

        return SubscriptionResponse.builder()
                .invoiceId(pwaResponse.getInvoiceId())
                .virtualAccountNumber(pwaResponse.getVirtualAccountNumber())
                .amount(request.getAmount())
                .status(pwaResponse.getStatus())
                .message(pwaResponse.getMessage())
                .build();
    }
}

