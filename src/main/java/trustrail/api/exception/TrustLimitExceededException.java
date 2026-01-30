package trustrail.api.exception;

public class TrustLimitExceededException extends RuntimeException{

        public TrustLimitExceededException(String message) {
            super(message);
        }
    }

