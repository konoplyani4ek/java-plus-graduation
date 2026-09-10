package ewm.request.server.exception;

/**
 * Внешняя зависимость (Feign) недоступна, а тихая деградация невозможна —
 * например, нельзя подать заявку, не проверив существование события.
 * Транслируется в 503 Service Unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}