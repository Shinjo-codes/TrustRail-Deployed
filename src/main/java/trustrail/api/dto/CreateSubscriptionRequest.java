package trustrail.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotBlank
    private String mandateReference;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    @NotBlank
    private String repeatFrequency; // DAILY, WEEKLY, MONTHLY

    @NotBlank
    private String repeatStartDate; // yyyy-MM-dd

    private String repeatEndDate;   // optional

    @NotBlank
    private String narration;
}
