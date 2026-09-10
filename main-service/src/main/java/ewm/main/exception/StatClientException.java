package ewm.main.exception;

public class StatClientException extends RuntimeException {

    public StatClientException(String message) {
        super(message);
    }

    public StatClientException(String message, Throwable cause) {
        super(message, cause);
    }
}