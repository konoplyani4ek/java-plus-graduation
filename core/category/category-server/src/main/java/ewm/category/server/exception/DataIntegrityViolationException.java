package ewm.category.server.exception;

public class DataIntegrityViolationException extends RuntimeException {
    public DataIntegrityViolationException(String message) {
        super(message);
    }
}