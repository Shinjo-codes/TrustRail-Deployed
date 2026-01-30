package trustrail.api.dto.accountlinking;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

// Account Linking Request
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountLinkingRequestDTO {

    @NotBlank(message = "External customer ID is required")
    private String externalCustomerId;
}




