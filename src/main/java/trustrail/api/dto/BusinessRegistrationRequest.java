package trustrail.api.dto;

import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;
import trustrail.api.entity.enums.BusinessType;

// ==================== AUTH DTOs ====================

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessRegistrationRequest {

    @NotBlank(message = "Business name is required")
    @Size(min = 2, max = 100)
    private String businessName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String notificationEmail;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone format")
    @Size(max = 13, message = "Phone number must not exceed 13 characters")
    private String phoneNumber;

    @NotNull(message = "Business type is required")
    private BusinessType businessType;

    @Column(nullable = false, unique = true)
    private String notificationPhoneNumber;
    @Column(nullable = false, unique = true)
    private String rcNumber;
    @Column(nullable = false, unique = true)
    private String tin;
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Address is required")
    private String address;
    @Column(nullable = false, unique = true)
    private String businessShortName;
    @Column(nullable = false, unique = true)
    private String whatsappContactName;
    @Column(nullable = false, unique = true)
    private String whatsappContactNumber;
    // Settlement details
    @Column(nullable = false, unique = true)
    private String settlementAccountNumber;
    @Column(nullable = false, unique = true)
    private String settlementBankCode;
    @Column(nullable = false, unique = true)
    private String settlementAccountName;


}

