package ewm.place.server.exception;

/**
 * Внешняя зависимость (Feign) недоступна, а тихая деградация невозможна —
 * например, нельзя безопасно удалить категорию, не проверив, используется
 * ли она в событиях (fail-closed: раз проверить нельзя — отказываем).
 * Транслируется в 503 Service Unavailable.
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}