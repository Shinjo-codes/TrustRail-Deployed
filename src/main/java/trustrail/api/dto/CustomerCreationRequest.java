package trustrail.api.dto;

// ==================== CUSTOMER DTOs ====================

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreationRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "External customer ID is required")
    private String externalCustomerId; // From CareFlow

    private String linkedAccountNumber;
    private String linkedBankCode;
    private String linkedBankName;

    private String encryptedSecure;  // Pre-encrypted from PWA tool
    private String encryptedBvn;     // Pre-encrypted from PWA tool
    private String bvn;
}

