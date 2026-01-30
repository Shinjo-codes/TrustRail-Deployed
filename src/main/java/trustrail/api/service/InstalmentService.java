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
public class InstalmentService {

    private final MandateRepo mandateRepo;
    private final PaymentTransactionRepo transactionRepo;
    private final PWAIntegrationService pwaIntegrationService;

    public InstalmentResponse createInstalmentPlan(CreateInstalmentRequest request) {

        Mandate mandate = mandateRepo.findByMandateReference(request.getMandateReference())
                .orElseThrow(() -> new IllegalArgumentException("Invalid mandate reference"));

        if (mandate.getStatus() != MandateStatus.ACTIVE) {
            throw new IllegalStateException("Mandate must be ACTIVE");
        }

        var pwaResponse = pwaIntegrationService.sendInstalmentInvoice(
                mandate.getCustomer().getExternalCustomerId(),
                mandate.getCustomer().getFirstName(),
                mandate.getCustomer().getLastName(),
                mandate.getCustomer().getEmail(),
                mandate.getCustomer().getPhoneNumber(),
                mandate.getCustomer().getEncryptedSecure(),
                mandate.getBusiness().getBillerCode(),
                request.getTotalAmount(),
                request.getDownPayment(),
                request.getInstalmentCount(),
                request.getRepeatFrequency(),
                request.getRepeatStartDate()
        );

        PaymentTransaction tx = PaymentTransaction.builder()
                .mandate(mandate)
                .customer(mandate.getCustomer())
                .transactionReference(pwaResponse.getInvoiceId())
                .transactionType(TransactionType.INSTALMENT)
                .status(TransactionStatus.PENDING)
                .amount(request.getTotalAmount())
                .build();

        transactionRepo.save(tx);

        return InstalmentResponse.builder()
                .invoiceId(pwaResponse.getInvoiceId())
                .virtualAccountNumber(pwaResponse.getVirtualAccountNumber())
                .totalAmount(request.getTotalAmount())
                .downPayment(request.getDownPayment())
                .instalmentCount(request.getInstalmentCount())
                .status(pwaResponse.getStatus())
                .message(pwaResponse.getMessage())
                .build();
    }
}

