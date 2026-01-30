package trustrail.api.dto;

// ==================== DASHBOARD DTOs ====================

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalCustomers;
    private Long totalMandates;
    private Long activeMandates;
    private BigDecimal totalRevenue;
    private BigDecimal outstandingBalance;
    private Long customersAtRisk;
    private Double averageTrustScore;
    private Long pendingPayments;
}
