package trustrail.api.dto;

// ==================== TRUST PROFILE DTOs ====================

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import trustrail.api.entity.enums.TrustState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class TrustProfileResponse {
    private Long id;
    private Integer behavioralScore;
    private Integer contextualScore;
    private Integer progressiveScore;
    private Integer overallTrustScore;
    private TrustState trustState;
    private Integer totalPayments;
    private Integer successfulPayments;
    private Integer failedPayments;
    private Integer consecutiveFailures;
    private BigDecimal totalAmountPaid;
    private BigDecimal outstandingBalance;
    private Boolean eligibleForInstalments;
    private Boolean eligibleForDeferred;
    private BigDecimal maxEligibleAmount;
    private LocalDateTime lastPaymentDate;
}
