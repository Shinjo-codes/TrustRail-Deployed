package trustrail.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import trustrail.api.entity.Mandate;
import trustrail.api.entity.enums.MandateStatus;
import trustrail.api.entity.enums.MandateType;
import trustrail.api.entity.enums.RepeatFrequency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MANDATE RESPONSE DTO
 *
 * Response object for mandate operations
 * Contains mandate details and activation information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MandateResponse {

    // Mandate Identification
    private Long id;
    private String mandateReference;              // TrustRail-generated
    private String pwaMandateId;                  // From PWA
    private String activationUrl;                 // URL for customer to activate

    // Mandate Details
    private MandateType mandateType;              // INSTALMENT, SUBSCRIPTION, MANAGED
    private MandateStatus status;                 // PENDING, ACTIVE, COMPLETED, etc.

    // Amounts
    private BigDecimal totalAmount;               // Total including down payment
    private BigDecimal downPayment;               // Initial payment (optional)
    private BigDecimal amountPaid;                // Total paid so far
    private BigDecimal remainingBalance;          // Amount still owed

    // Instalments
    private Integer instalmentCount;              // Number of instalments
    private Integer instalmentsPaid;              // Instalments completed
    private BigDecimal instalmentAmount;          // Per-instalment amount

    // Schedule
    private RepeatFrequency repeatFrequency;      // DAILY, WEEKLY, MONTHLY, etc.
    private LocalDateTime startDate;              // When instalments start
    private LocalDateTime endDate;                // When mandate expires
    private LocalDateTime nextDebitDate;          // Next scheduled debit

    // Metadata
    private String narration;                     // Description
    private LocalDateTime createdAt;              // When mandate was created

    // Customer Information
    private CustomerResponse customer;

    /**
     * Map from Mandate entity to DTO
     */
    public static MandateResponse from(Mandate mandate) {
        return MandateResponse.builder()
                .id(mandate.getId())
                .mandateReference(mandate.getMandateReference())
                .pwaMandateId(mandate.getPwaMandateId())
                .activationUrl(mandate.getActivationUrl())
                .mandateType(mandate.getMandateType())
                .status(mandate.getStatus())
                .totalAmount(mandate.getTotalAmount())
                .downPayment(mandate.getDownPayment())
                .amountPaid(mandate.getAmountPaid())
                .remainingBalance(mandate.getRemainingBalance())
                .instalmentCount(mandate.getInstalmentCount())
                .instalmentsPaid(mandate.getInstalmentsPaid())
                .instalmentAmount(mandate.getInstalmentAmount())
                .repeatFrequency(mandate.getRepeatFrequency())
                .startDate(mandate.getStartDate())
                .endDate(mandate.getEndDate())
                .nextDebitDate(mandate.getNextDebitDate())
                .narration(mandate.getNarration())
                .createdAt(mandate.getCreatedAt())
                .customer(CustomerResponse.from(mandate.getCustomer()))
                .build();
    }

    /**
     * NESTED: Customer Response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerResponse {
        private Long id;
        private String externalCustomerId;
        private String firstName;
        private String lastName;
        private String email;
        private String phoneNumber;

        /**
         * Map from Customer entity to DTO
         */
        public static CustomerResponse from(trustrail.api.entity.Customer customer) {
            return CustomerResponse.builder()
                    .id(customer.getId())
                    .externalCustomerId(customer.getExternalCustomerId())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .email(customer.getEmail())
                    .phoneNumber(customer.getPhoneNumber())
                    .build();
        }
    }
}