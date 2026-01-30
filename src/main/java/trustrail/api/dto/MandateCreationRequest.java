package trustrail.api.dto;

// ==================== MANDATE DTOs ====================

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trustrail.api.entity.enums.MandateType;
import trustrail.api.entity.enums.RepeatFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandateCreationRequest {

    @NotBlank(message = "Customer ID is required")
    private String externalCustomerId;

    @NotNull(message = "Mandate type is required")
    private MandateType mandateType;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01")
    private BigDecimal totalAmount;

    // For instalments
    private Integer instalmentCount;
    private BigDecimal downPayment;

    // For subscriptions
    private RepeatFrequency repeatFrequency;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Size(max = 500)
    private String narration;
}


