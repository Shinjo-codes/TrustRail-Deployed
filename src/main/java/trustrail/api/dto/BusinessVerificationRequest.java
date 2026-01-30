package trustrail.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ==================== VERIFY BUSINESS REQUEST ====================
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessVerificationRequest {

    @NotNull(message = "Business ID is required")
    private Long businessId;

    @NotNull(message = "Verification method is required")
    private String verifiedBy; // e.g., "admin" or "KYC"
}
