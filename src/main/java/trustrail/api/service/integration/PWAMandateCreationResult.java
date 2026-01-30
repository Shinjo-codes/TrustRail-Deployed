package trustrail.api.service.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PWAMandateCreationResult {
    private String mandateId;
    private String activationUrl;
    private String status;
    private String message;
}
