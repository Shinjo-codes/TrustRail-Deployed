package trustrail.api.dto;

// ==================== ELIGIBILITY CHECK DTOs ====================

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trustrail.api.entity.enums.MandateStatus;
import trustrail.api.entity.enums.MandateType;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityCheckRequest {

    @NotBlank(message = "Customer ID is required")
    private String externalCustomerId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment type is required")
    private MandateType paymentType;

    @NotNull(message = "Payment type is required")
    private MandateStatus mandateStatus;

    private Integer instalmentCount; // Required if paymentType = INSTALMENT

    private Boolean isEmergency = false;
}


