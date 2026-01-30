package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import trustrail.api.entity.Customer;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class CustomerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private String externalCustomerId;
    private Boolean accountLinked;
    private String linkedBankName;
    private TrustProfileResponse trustProfile;
    private LocalDateTime createdAt;

    public static CustomerResponse from(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .externalCustomerId(customer.getExternalCustomerId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhoneNumber())
                .accountLinked(customer.getAccountLinked())
                .build();
    }

}
