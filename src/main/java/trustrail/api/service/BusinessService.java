package trustrail.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trustrail.api.dto.BusinessRegistrationRequest;
import trustrail.api.entity.Business;
import trustrail.api.entity.enums.BusinessStatus;
import trustrail.api.repo.BusinessRepo;
import trustrail.api.service.integration.PWACreateMerchantRequest;
import trustrail.api.service.integration.PWACreateMerchantResponse;
import trustrail.api.service.integration.PWAIntegrationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessRepo businessRepo;
    private final PasswordEncoder passwordEncoder;
    private final PWAIntegrationService pwaIntegrationService;

//    @Transactional
//    public Business registerBusiness(BusinessRegistrationRequest request) {
//        log.info("Registering business: {}", request.getBusinessName());
//
//        // 1️⃣ Build Business entity
//        Business business = Business.builder()
//                .businessName(request.getBusinessName())
//                .notificationEmail(request.getNotificationEmail())
//                .password(passwordEncoder.encode(request.getPassword()))
//                .phoneNumber(request.getPhoneNumber())
//                .businessType(request.getBusinessType())
//                .settlementAccountNumber(request.getSettlementAccountNumber())
//                .settlementBankCode(request.getSettlementBankCode())
//                .settlementAccountName(request.getSettlementAccountName())
//                .notificationPhoneNumber(request.getNotificationPhoneNumber())
//                .rcNumber(request.getRcNumber())
//                .tin(request.getTin())
//                .address(request.getAddress())
//                .businessShortName(request.getBusinessShortName())
//                .whatsappContactName(request.getWhatsappContactName())
//                .whatsappContactNumber(request.getWhatsappContactNumber())
//                .status(BusinessStatus.PENDING)
//                .build();
//
//        try {
//            // 2️⃣ Call PWA to create merchant
//            log.info("Calling PWA to create merchant for: {}", business.getBusinessName());
//            PWACreateMerchantResponse pwaResponse = pwaIntegrationService.createMerchant(business);
//
//            log.info("PWA Response - Status: {}, Message: {}, MerchantId: {}",
//                    pwaResponse.getStatus(),
//                    pwaResponse.getMessage(),
//                    pwaResponse.getMerchantId());
//
//            // 3️⃣ Update business with PWA response
//            if (pwaResponse.getMerchantId() != null) {
//                business.setPwaBusinessId(pwaResponse.getMerchantId());
//                business.setBillerCode(pwaResponse.getMerchantId()); // Use merchantId as billerCode for now
//            } else {
//                log.error("PWA returned null merchantId!");
//                throw new RuntimeException("PWA did not return merchant ID");
//            }
//
//            if ("Successful".equalsIgnoreCase(pwaResponse.getStatus())) {
//                business.setStatus(BusinessStatus.ACTIVE);
//            } else {
//                business.setStatus(BusinessStatus.PENDING);
//                log.warn("PWA merchant creation not immediately successful: {}", pwaResponse.getMessage());
//            }
//
//        } catch (Exception e) {
//            log.error("PWA merchant creation failed", e);
//            // Set to PENDING and save anyway - webhook will update later
//            business.setStatus(BusinessStatus.PENDING);
//            business.setPwaBusinessId("PENDING_" + System.currentTimeMillis()); // Temporary ID
//            business.setBillerCode("PENDING_" + System.currentTimeMillis());     // Temporary ID
//        }
//
//        // 4️⃣ Save to database
//        Business savedBusiness = businessRepo.save(business);
//        log.info("Business saved with ID: {}, PWA ID: {}", savedBusiness.getId(), savedBusiness.getPwaBusinessId());
//
//        return savedBusiness;
//    }
@Transactional
public Business registerBusiness(BusinessRegistrationRequest request) {
    log.info("Registering business: {}", request.getBusinessName());

    // 1️⃣ Call PWA FIRST (don't save Business yet)
    Business tempBusiness = Business.builder()
            .businessName(request.getBusinessName())
            .notificationEmail(request.getNotificationEmail())
            .phoneNumber(request.getPhoneNumber())
            .rcNumber(request.getRcNumber())
            .tin(request.getTin())
            .address(request.getAddress())
            .businessShortName(request.getBusinessShortName())
            .whatsappContactName(request.getWhatsappContactName())
            .whatsappContactNumber(request.getWhatsappContactNumber())
            .settlementAccountNumber(request.getSettlementAccountNumber())
            .settlementBankCode(request.getSettlementBankCode())
            .settlementAccountName(request.getSettlementAccountName())
            .notificationPhoneNumber(request.getNotificationPhoneNumber())
            .build();

    // 2️⃣ Call PWA
    PWACreateMerchantResponse pwaResponse = pwaIntegrationService.createMerchant(tempBusiness);

    if (pwaResponse.getMerchantId() == null) {
        throw new RuntimeException("PWA did not return merchant ID. Cannot register business.");
    }

    // 3️⃣ NOW build the complete Business entity with PWA data
    Business business = Business.builder()
            .businessName(request.getBusinessName())
            .notificationEmail(request.getNotificationEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phoneNumber(request.getPhoneNumber())
            .businessType(request.getBusinessType())
            .settlementAccountNumber(request.getSettlementAccountNumber())
            .settlementBankCode(request.getSettlementBankCode())
            .settlementAccountName(request.getSettlementAccountName())
            .notificationPhoneNumber(request.getNotificationPhoneNumber())
            .rcNumber(request.getRcNumber())
            .tin(request.getTin())
            .address(request.getAddress())
            .businessShortName(request.getBusinessShortName())
            .whatsappContactName(request.getWhatsappContactName())
            .whatsappContactNumber(request.getWhatsappContactNumber())
            .pwaBusinessId(pwaResponse.getMerchantId())  // ← Set from PWA
            .billerCode(pwaResponse.getMerchantId())     // ← Set from PWA
            .status("Successful".equalsIgnoreCase(pwaResponse.getStatus())
                    ? BusinessStatus.ACTIVE
                    : BusinessStatus.PENDING)
            .build();

    // 4️⃣ Save to database
    return businessRepo.save(business);
}

    // ==================== HELPERS ====================

    private PWACreateMerchantRequest.Transaction buildMerchantTransaction(
            BusinessRegistrationRequest request
    ) {
        return PWACreateMerchantRequest.Transaction.builder()
                .mockMode("Live")
                .transactionRef("TR-TXN-" + System.currentTimeMillis())
                .transactionDesc("Applying for a new merchant account")
                .transactionRefParent("")
                .amount(0)
                .customer(
                        PWACreateMerchantRequest.Customer.builder()
                                .customerRef(request.getPhoneNumber())
                                .firstname(request.getBusinessName())
                                .surname("Owner")
                                .email(request.getNotificationEmail())
                                .mobileNo(request.getPhoneNumber())
                                .build()
                )
                .meta(
                        PWACreateMerchantRequest.Meta.builder()
                                .beta("enabled")
                                .billerSector("Aggregattor")
                                .simplePayment("enabled")
                                .webhookUrl("https://trustrail.app/api/webhooks/pwa")
                                .businessShortName(
                                        request.getBusinessName()
                                                .replaceAll("\\s+", "")
                                                .toUpperCase()
                                )
                                .build()
                )
                .details(
                        PWACreateMerchantRequest.Details.builder()
                                .businessName(request.getBusinessName())
                                .settlementAccountNo(request.getSettlementAccountNumber())
                                .settlementBankCode(request.getSettlementBankCode())
                                .address("Demo Address")
                                .notificationEmail(request.getNotificationEmail())
                                .build()
                )
                .options(null)
                .build();
    }
}
