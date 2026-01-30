//package trustrail.api.service;
//
//import trustrail.api.entity.enums.*;
//import trustrail.api.service.integration.PWAMandateCreationResult;
//import trustrail.api.service.integration.PWAIntegrationService;
//import trustrail.api.dto.*;
//import trustrail.api.entity.*;
//import trustrail.api.repo.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.time.LocalDateTime;
//
///**
// * MANDATE ORCHESTRATION SERVICE
// *
// * This service coordinates mandate creation between TrustRail and PWA.
// * Flow:
// * 1. Receive mandate request
// * 2. Validate eligibility
// * 3. Create PWA mandate (using pre-encrypted customer data)
// * 4. Store mandate in TrustRail
// * 5. Return activation URL to customer
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class ImprovedMandateService {
//
//    private final MandateRepo mandateRepository;
//    private final CustomerRepo customerRepository;
//    private final TrustProfileRepo trustProfileRepository;
//    private final PaymentTransactionRepo transactionRepository;
//    private final PWAIntegrationService pwaService;
//
//    // Get biller code from application.properties
//    @Value("${pwa.biller-code}")
//    private String billerCode;
//
//    @Transactional
//    public MandateResponse createMandate(Business business, MandateCreationRequest request) {
//        Customer customer = customerRepository
//                .findByBusinessAndExternalCustomerId(business, request.getExternalCustomerId())
//                .orElseThrow(() -> new RuntimeException("Customer not found"));
//
//        // Verify customer has encrypted data
//        if (!customer.getAccountLinked()) {
//            throw new RuntimeException(
//                    "Account not linked. Customer must link bank account first."
//            );
//        }
//
//        if (customer.getEncryptedSecure() == null || customer.getEncryptedSecure().isEmpty()) {
//            throw new RuntimeException(
//                    "Customer account data not encrypted. " +
//                            "Please create customer with encrypted secure field."
//            );
//        }
//
//        // Calculate correct amounts
//        BigDecimal totalAmount = request.getTotalAmount();
//        BigDecimal downPayment = request.getDownPayment() != null ?
//                request.getDownPayment() : BigDecimal.ZERO;
//
//        // FIX: Amount to finance = Total - Down payment
//        BigDecimal amountToFinance = totalAmount.subtract(downPayment);
//
//        // FIX: Calculate instalment from financed amount, not total
//        BigDecimal instalmentAmount = null;
//        if (request.getMandateType() == MandateType.INSTALMENT) {
//            instalmentAmount = amountToFinance.divide(
//                    BigDecimal.valueOf(request.getInstalmentCount()),
//                    2,
//                    RoundingMode.HALF_UP
//            );
//        }
//
//        // Create mandate
//        Mandate mandate = Mandate.builder()
//                .business(business)
//                .customer(customer)
//                .mandateReference(generateMandateReference())
//                .mandateType(request.getMandateType())
//                .status(MandateStatus.PENDING)
//                .totalAmount(totalAmount)
//                .amountPaid(BigDecimal.ZERO)
//                .remainingBalance(totalAmount) // Total including down payment
//                .instalmentCount(request.getInstalmentCount())
//                .instalmentsPaid(0)
//                .instalmentAmount(instalmentAmount)
//                .downPayment(downPayment)
//                .repeatFrequency(request.getRepeatFrequency())
//                .startDate(request.getStartDate())
//                .endDate(request.getEndDate())
//                .narration(request.getNarration())
//                .build();
//
//        mandate = mandateRepository.save(mandate);
//
//        // Call PWA with stored encrypted values
//        PWAMandateCreationResult pwaResponse = createPWAMandate(customer, mandate);
//
//        mandate.setPwaMandateId(pwaResponse.getMandateId());
//        mandate.setActivationUrl(pwaResponse.getActivationUrl());
//        mandate = mandateRepository.save(mandate);
//
//        // Create correct payment schedule
//        if (request.getMandateType() == MandateType.INSTALMENT) {
//            createCorrectInstalmentSchedule(mandate, amountToFinance);
//        }
//
//        // Update trust profile with outstanding balance
//        TrustProfile profile = trustProfileRepository.findByCustomer(customer)
//                .orElseThrow();
//        profile.setOutstandingBalance(
//                profile.getOutstandingBalance().add(amountToFinance)
//        );
//        trustProfileRepository.save(profile);
//
//        return mapToMandateResponse(mandate, customer);
//    }
//
//    // NEW METHOD: Call PWA using stored encrypted values
//    private PWAMandateCreationResult createPWAMandate(Customer customer, Mandate mandate) {
//        log.info("Creating PWA mandate for customer: {}", customer.getExternalCustomerId());
//
//        // Get encrypted values from customer (already stored!)
//        String encryptedSecure = customer.getEncryptedSecure();
//        String encryptedBvn = customer.getEncryptedBvn() != null ?
//                customer.getEncryptedBvn() : "";
//
//        // Call PWA service with pre-encrypted data
//        return pwaService.createMandate(
//                customer.getExternalCustomerId(),  // customer_ref
//                customer.getFirstName(),            // firstname
//                customer.getLastName(),             // surname
//                customer.getEmail(),                // email
//                customer.getPhoneNumber(),          // mobile_no
//                encryptedSecure,                    // Already encrypted!
//                encryptedBvn,                       // Already encrypted!
//                billerCode,                         // biller_code from config
//                mandate.getTotalAmount()            // amount
//        );
//    }
//
//    /**
//     * Create schedule only for financed amount
//     */
//    private void createCorrectInstalmentSchedule(Mandate mandate, BigDecimal financedAmount) {
//        LocalDateTime currentDate = mandate.getStartDate() != null ?
//                mandate.getStartDate() : LocalDateTime.now().plusDays(7);
//
//        BigDecimal instalmentAmount = mandate.getInstalmentAmount();
//        BigDecimal totalScheduled = BigDecimal.ZERO;
//
//        for (int i = 1; i <= mandate.getInstalmentCount(); i++) {
//            BigDecimal amount;
//
//            // Last instalment gets any remaining balance (handles rounding)
//            if (i == mandate.getInstalmentCount()) {
//                amount = financedAmount.subtract(totalScheduled);
//            } else {
//                amount = instalmentAmount;
//                totalScheduled = totalScheduled.add(amount);
//            }
//
//            PaymentTransaction transaction = PaymentTransaction.builder()
//                    .mandate(mandate)
//                    .customer(mandate.getCustomer())
//                    .transactionReference(generateTransactionReference())
//                    .transactionType(TransactionType.INSTALMENT)
//                    .status(TransactionStatus.SCHEDULED)
//                    .amount(amount)
//                    .instalmentNumber(i)
//                    .scheduledDate(currentDate)
//                    .retryCount(0)
//                    .build();
//
//            transactionRepository.save(transaction);
//            currentDate = calculateNextDate(currentDate, mandate.getRepeatFrequency());
//        }
//
//        log.info("Created {} instalments totaling {}",
//                mandate.getInstalmentCount(), financedAmount);
//    }
//
//    private LocalDateTime calculateNextDate(LocalDateTime current, RepeatFrequency frequency) {
//        if (frequency == null) frequency = RepeatFrequency.MONTHLY;
//        return switch (frequency) {
//            case DAILY -> current.plusDays(1);
//            case WEEKLY -> current.plusWeeks(1);
//            case MONTHLY -> current.plusMonths(1);
//            case QUARTERLY -> current.plusMonths(3);
//            case YEARLY -> current.plusYears(1);
//        };
//    }
//
//    private String generateMandateReference() {
//        return "MAN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//    }
//
//    private String generateTransactionReference() {
//        return "TXN-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//    }
//
//    // Map mandate to response DTO
//    private MandateResponse mapToMandateResponse(Mandate mandate, Customer customer) {
//        CustomerResponse customerResponse = CustomerResponse.builder()
//                .id(customer.getId())
//                .firstName(customer.getFirstName())
//                .lastName(customer.getLastName())
//                .email(customer.getEmail())
//                .externalCustomerId(customer.getExternalCustomerId())
//                .build();
//
//        return MandateResponse.builder()
//                .id(mandate.getId())
//                .mandateReference(mandate.getMandateReference())
//                .pwaMandateId(mandate.getPwaMandateId())
//                .mandateType(mandate.getMandateType())
//                .status(mandate.getStatus())
//                .totalAmount(mandate.getTotalAmount())
//                .amountPaid(mandate.getAmountPaid())
//                .remainingBalance(mandate.getRemainingBalance())
//                .instalmentCount(mandate.getInstalmentCount())
//                .instalmentsPaid(mandate.getInstalmentsPaid())
//                .instalmentAmount(mandate.getInstalmentAmount())
//                .activationUrl(mandate.getActivationUrl())
//                .nextDebitDate(mandate.getNextDebitDate())
//                .createdAt(mandate.getCreatedAt())
//                .customer(customerResponse)
//                .build();
//    }
//}