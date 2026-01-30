package trustrail.api.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map; /**
 * Standard error response structure
 */
@Data
@Builder
@AllArgsConstructor
 public class ErrorResponse {
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private Map<String, String> errors;
}
