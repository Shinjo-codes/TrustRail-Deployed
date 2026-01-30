//package trustrail.api.config;
//
//// ==================== DATABASE SEEDER (OPTIONAL) ====================
//
//import trustrail.api.entity.*;
//import trustrail.api.entity.enums.BusinessStatus;
//import trustrail.api.entity.enums.BusinessType;
//import trustrail.api.entity.enums.TrustState;
//import trustrail.api.repo.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.math.BigDecimal;
//
///**
// * DATABASE SEEDER
// *
// * Creates sample data for testing
// * Remove this in production!
// */
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class DatabaseSeeder {
//
//    private final BusinessRepo businessRepository;
//    private final CustomerRepo customerRepository;
//    private final TrustProfileRepo trustProfileRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Bean
//    public CommandLineRunner seedDatabase() {
//        return args -> {
//            // Only seed if database is empty
//            if (businessRepository.count() > 0) {
//                log.info("Database already seeded, skipping...");
//                return;
//            }
//
//            log.info("Seeding database with sample data...");
//
//            // Create sample business (CareFlow)
//            Business careflow = Business.builder()
//                    .businessName("CareFlow Healthcare")
//                    .notificationEmail("admin@careflow.com")
//                    .password(passwordEncoder.encode("password123"))
//                    .phoneNumber("+2348012345678")
//                    .businessType(BusinessType.HEALTHCARE)
//                    .status(BusinessStatus.ACTIVE)
//                    .settlementAccountNumber("1234567890")
//                    .settlementBankCode("058")
//                    .settlementAccountName("CareFlow Healthcare Ltd")
//                    .address("Lagos, Nigeria")
//                    .billerCode("000987")
//                    .pwaBusinessId("PWA-MERCHANT-001") // Mock PWA ID
//                    .build();
//
//            careflow = businessRepository.save(careflow);
//            log.info("Created business: {}", careflow.getBusinessName());
//
//            // Create sample customer (Patient)
//            Customer patient = Customer.builder()
//                    .business(careflow)
//                    .firstName("John")
//                    .lastName("Ade")
//                    .email("john.ade@example.com")
//                    .phoneNumber("+2348098765432")
//                    .externalCustomerId("PATIENT-001")
//                    .accountLinked(true)
//                    .linkedAccountNumber("0123456789")
//                    .linkedBankCode("058")
//                    .linkedBankName("GTBank")
//                    .pwaAccountReference("PWA-ACCOUNT-001")
//                    .build();
//
//            patient = customerRepository.save(patient);
//            log.info("Created customer: {} {}", patient.getFirstName(), patient.getLastName());
//
//            // Create trust profile
//            TrustProfile profile = TrustProfile.builder()
//                    .customer(patient)
//                    .behavioralScore(70)
//                    .contextualScore(65)
//                    .progressiveScore(60)
//                    .overallTrustScore(66)
//                    .trustState(TrustState.VERIFIED)
//                    .totalPayments(5)
//                    .successfulPayments(4)
//                    .failedPayments(1)
//                    .consecutiveFailures(0)
//                    .totalAmountPaid(new BigDecimal("50000.00"))
//                    .outstandingBalance(new BigDecimal("20000.00"))
//                    .eligibleForInstalments(true)
//                    .eligibleForDeferred(true)
//                    .maxEligibleAmount(new BigDecimal("100000.00"))
//                    .build();
//
//            trustProfileRepository.save(profile);
//            log.info("Created trust profile with score: {}", profile.getOverallTrustScore());
//
//            log.info("Database seeding completed!");
//        };
//    }
//}
//
