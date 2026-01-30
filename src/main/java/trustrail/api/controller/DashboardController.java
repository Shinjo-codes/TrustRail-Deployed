package trustrail.api.controller;

// ==================== DASHBOARD CONTROLLER ====================

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trustrail.api.dto.DashboardStatsResponse;
import trustrail.api.entity.Business;
import trustrail.api.entity.enums.MandateStatus;
import trustrail.api.entity.enums.TransactionStatus;
import trustrail.api.repo.BusinessRepo;
import trustrail.api.security.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final BusinessRepo businessRepository;
    private final trustrail.api.repo.CustomerRepo customerRepository;
    private final trustrail.api.repo.MandateRepo mandateRepository;
    private final trustrail.api.repo.PaymentTransactionRepo transactionRepository;
    private final trustrail.api.repo.TrustProfileRepo trustProfileRepository;

    private Business getCurrentBusiness() {
        String email = SecurityUtils.getCurrentUserEmail();
        return businessRepository.findByNotificationEmail(email)
                .orElseThrow(() -> new RuntimeException("Business not found"));
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        Business business = getCurrentBusiness();

        long totalCustomers = customerRepository.findByBusiness(business).size();
        long totalMandates = mandateRepository.findByBusiness(business).size();
        long activeMandates = mandateRepository.findByBusinessAndStatus(
                business, MandateStatus.ACTIVE).size();

        // Calculate total revenue and outstanding balance
        List<trustrail.api.entity.Mandate> mandates = mandateRepository.findByBusiness(business);
        java.math.BigDecimal totalRevenue = mandates.stream()
                .map(trustrail.api.entity.Mandate::getAmountPaid)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal outstandingBalance = mandates.stream()
                .map(trustrail.api.entity.Mandate::getRemainingBalance)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // Customers at risk (3+ consecutive failures)
        long customersAtRisk = trustProfileRepository.findAtRiskCustomers(business, 3).size();

        // Average trust score
        List<trustrail.api.entity.TrustProfile> profiles =
                trustProfileRepository.findAll().stream()
                        .filter(p -> p.getCustomer().getBusiness().equals(business))
                        .toList();

        double avgTrustScore = profiles.isEmpty() ? 0 :
                profiles.stream()
                        .mapToInt(trustrail.api.entity.TrustProfile::getOverallTrustScore)
                        .average()
                        .orElse(0.0);

        long pendingPayments = transactionRepository.findAll().stream()
                .filter(t -> t.getMandate().getBusiness().equals(business))
                .filter(t -> t.getStatus() == TransactionStatus.SCHEDULED)
                .count();

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalCustomers(totalCustomers)
                .totalMandates(totalMandates)
                .activeMandates(activeMandates)
                .totalRevenue(totalRevenue)
                .outstandingBalance(outstandingBalance)
                .customersAtRisk(customersAtRisk)
                .averageTrustScore(avgTrustScore)
                .pendingPayments(pendingPayments)
                .build();

        return ResponseEntity.ok(stats);
    }
}
