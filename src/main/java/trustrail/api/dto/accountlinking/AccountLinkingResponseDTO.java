package trustrail.api.dto.accountlinking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

// Account Linking Response
@Data
@Builder
@AllArgsConstructor
public class AccountLinkingResponseDTO {
    private String linkingUrl;
    private LocalDateTime expiresAt;
    private String message;
}
