package trustrail.api.dto;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trustrail.api.entity.enums.BusinessStatus;
import trustrail.api.entity.enums.BusinessType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String businessName;
    private String email;
    private String password;
    private String phoneNumber;
    private String businessType;
    private String settlementAccountNumber;
    private String settlementBankCode;
    private String settlementAccountName;
    private String notificationEmail;
    private String notificationPhoneNumber;
    private String rcNumber;
    private String tin;
    private String address;
    private String businessShortName;
    private String whatsappContactName;
    private String whatsappContactNumber;
}

