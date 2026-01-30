package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class EligibilityCheckResponse {
    private Boolean eligible;
    private String decision; // APPROVED, CONDITIONAL, DENIED
    private String reason;
    private BigDecimal approvedAmount;
    private BigDecimal requiredDownPayment;
    private Integer maxInstalments;
    private TrustProfileResponse trustProfile;
    private LocalDateTime evaluatedAt;
}