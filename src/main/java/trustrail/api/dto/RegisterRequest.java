package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    private String businessName;
    private String email;
    private String password;
    private String phoneNumber;
    private String businessType;  // Make sure this exists
    private String settlementAccountNumber;  // Make sure this exists
    private String settlementBankCode;  // Make sure this exists
    private String settlementAccountName;  // Make sure this exists
    private String notificationEmail;
    private String notificationPhoneNumber;  // Make sure this exists
    private String rcNumber;
    private String tin;  // Make sure this exists
    private String address;  // Make sure this exists
    private String businessShortName;  // Make sure this exists
    private String whatsappContactName;
    private String whatsappContactNumber;
}