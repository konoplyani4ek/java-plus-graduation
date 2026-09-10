package ewm.request.server.service;

import ewm.request.server.exception.NotFoundException;
import ewm.request.server.exception.ServiceUnavailableException;
import ewm.user.client.UserClient;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Шлюз к user-service. Отдельный бин: аннотации Resilience4j работают через
 * Spring AOP-прокси и не сработают при self-invocation (вызове изнутри того же класса).
 *
 * В request-service UserClient используется только для валидации перед записью
 * (подача/отмена заявки) — деградация здесь недопустима, поэтому единственный
 * метод либо возвращает успех, либо кидает понятную ошибку.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGateway {

    private static final String CB = "user-client";

    private final UserClient userClient;

    @CircuitBreaker(name = CB, fallbackMethod = "assertExistsFallback")
    @Retry(name = CB)
    public void assertExists(long userId) {
        try {
            userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    @SuppressWarnings("unused")
    private void assertExistsFallback(long userId, Throwable t) {
        if (t instanceof NotFoundException notFound) {
            throw notFound;
        }
        log.warn("user-service недоступен при проверке пользователя {}: {}", userId, t.toString());
        throw new ServiceUnavailableException("Сервис пользователей временно недоступен, попробуйте позже");
    }
}