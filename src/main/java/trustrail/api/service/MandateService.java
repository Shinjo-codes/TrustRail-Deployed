//package trustrail.api.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import trustrail.api.dto.MandateCreationRequest;
//import trustrail.api.dto.MandateResponse;
//import trustrail.api.entity.Business;
//import trustrail.api.entity.Customer;
//import trustrail.api.entity.Mandate;
//import trustrail.api.entity.PaymentTransaction;
//import trustrail.api.entity.enums.MandateStatus;
//import trustrail.api.entity.enums.MandateType;
//import trustrail.api.entity.enums.TransactionStatus;
//import trustrail.api.repo.CustomerRepo;
//import trustrail.api.repo.MandateRepo;
//import trustrail.api.repo.PaymentTransactionRepo;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class MandateService {
//
//    private final MandateRepo mandateRepository;
//    private final CustomerRepo customerRepository;
//    private final PaymentTransactionRepo transactionRepository;
//
//    @Transactional
//    public MandateResponse createMandate(
//            Business business,
//            MandateCreationRequest request
//    ) {
//        Customer customer = customerRepository
//                .findByBusinessAndExternalCustomerId(
//                        business,
//                        request.getExternalCustomerId()
//                )
//                .orElseThrow(() ->
//                        new IllegalStateException("Customer not found")
//                );
//
//        Mandate mandate = Mandate.builder()
//                .business(business)
//                .customer(customer)
//                .mandateReference(generateReference())
//                .mandateType(request.getMandateType())
//                .repeatFrequency(request.getRepeatFrequency())
//                .totalAmount(request.getTotalAmount())
//                .remainingBalance(request.getTotalAmount())
//                .instalmentCount(request.getInstalmentCount())
//                .instalmentsPaid(0)
//                .status(MandateStatus.PENDING)
//                .build();
//
//        mandateRepository.save(mandate);
//
//        return MandateResponse.from(mandate);
//    }
//
//    @Transactional
//    public void activateMandate(String pwaMandateId) {
//        Mandate mandate = mandateRepository
//                .findByPwaMandateId(pwaMandateId)
//                .orElseThrow(() ->
//                        new IllegalStateException("Mandate not found for PWA ID: " + pwaMandateId)
//                );
//
//        if (mandate.getStatus() == MandateStatus.ACTIVE) {
//            log.info("Mandate already active: {}", mandate.getMandateReference());
//            return;
//        }
//
//        mandate.setStatus(MandateStatus.ACTIVE);
//        mandate.setActivatedAt(LocalDateTime.now());
//        mandate.setNextDebitDate(LocalDateTime.now().plusDays(1));
//
//        mandateRepository.save(mandate);
//
//        scheduleInitialTransactions(mandate);
//
//        log.info("Mandate activated: {}", mandate.getMandateReference());
//    }
//
//    @Transactional(readOnly = true)
//    public List<MandateResponse> getBusinessMandates(Business business) {
//        return mandateRepository.findByBusiness(business)
//                .stream()
//                .map(MandateResponse::from)
//                .toList();
//    }
//
//    @Transactional(readOnly = true)
//    public MandateResponse getMandateByReference(String mandateReference) {
//        Mandate mandate = mandateRepository
//                .findByMandateReference(mandateReference)
//                .orElseThrow(() ->
//                        new IllegalStateException("Mandate not found")
//                );
//
//        return MandateResponse.from(mandate);
//    }
//
//    private void scheduleInitialTransactions(Mandate mandate) {
//        if (mandate.getMandateType() == MandateType.INSTALMENT) {
//            for (int i = 1; i <= mandate.getInstalmentCount(); i++) {
//                PaymentTransaction txn = PaymentTransaction.builder()
//                        .mandate(mandate)
//                        .customer(mandate.getCustomer())
//                        .amount(
//                                mandate.getTotalAmount()
//                                        .divide(
//                                                new java.math.BigDecimal(mandate.getInstalmentCount()),
//                                                java.math.RoundingMode.HALF_UP
//                                        )
//                        )
//                        .instalmentNumber(i)
//                        .scheduledDate(mandate.getNextDebitDate())
//                        .status(TransactionStatus.SCHEDULED)
//                        .build();
//
//                transactionRepository.save(txn);
//            }
//        }
//    }
//
//    private String generateReference() {
//        return "MDT-" + System.currentTimeMillis();
//    }
//}
