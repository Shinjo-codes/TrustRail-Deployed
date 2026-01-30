package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trustrail.api.entity.enums.BusinessStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BusinessRegistrationResponse {
    private Long businessId;
    private String businessName;
    private String notificationEmail;
    private String phoneNumber;
    private String pwaBusinessId;
    private BusinessStatus status;
    private String message;
}
