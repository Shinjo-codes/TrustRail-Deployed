package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import trustrail.api.entity.enums.BusinessStatus;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private Long businessId;
    private String businessName;
    private String notificationEmail;
    private BusinessStatus status;
    private String message;
}
