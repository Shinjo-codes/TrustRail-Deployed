package trustrail.api.dto.accountlinking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Account Linking Callback (from PWA)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountLinkingCallbackRequestDTO {
    private String linkingToken;
    private String accountNumber;
    private String bankCode;
    private String bankName;
    private String status;
}
