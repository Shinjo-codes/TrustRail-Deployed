package trustrail.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstalmentRequest {

    @NotBlank
    private String mandateReference;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalAmount;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal downPayment;

    @NotNull
    @Min(1)
    private Integer instalmentCount;

    @NotBlank
    private String repeatFrequency; // daily

    @NotBlank
    private String repeatStartDate;

    @NotBlank
    private String narration;

    @NotBlank
    private String encryptedSecure;
}

