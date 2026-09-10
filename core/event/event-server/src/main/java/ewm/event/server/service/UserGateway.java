package ewm.event.server.service;

import ewm.event.server.exception.NotFoundException;
import ewm.event.server.exception.ServiceUnavailableException;
import ewm.user.client.UserClient;
import ewm.user.dto.UserDto;
import ewm.user.dto.UserShortDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Шлюз к user-service. Вынесен в отдельный бин намеренно: аннотации Resilience4j
 * работают через Spring AOP-прокси и не сработают при вызове метода изнутри
 * того же класса (self-invocation) — только при вызове через другой бин.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserGateway {

    private static final String CB = "user-client";

    private final UserClient userClient;

    /**
     * Для отображения (EventFullDto/EventShortDto.initiator) — при недоступности
     * user-service возвращаем заглушку, а не роняем весь ответ.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getShortFallback")
    @Retry(name = CB)
    public UserShortDto getShort(long userId) {
        UserDto user = userClient.getUser(userId);
        return new UserShortDto(user.getId(), user.getName());
    }

    @SuppressWarnings("unused")
    private UserShortDto getShortFallback(long userId, Throwable t) {
        log.warn("user-service недоступен при получении пользователя {}: {}", userId, t.toString());
        return new UserShortDto(userId, "Пользователь недоступен");
    }

    /**
     * Батч-версия для списков событий — один запрос вместо N (проблема N+1).
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getShortMapFallback")
    @Retry(name = CB)
    public Map<Long, UserShortDto> getShortMap(List<Long> userIds) {
        Map<Long, UserShortDto> result = new HashMap<>();
        for (UserDto user : userClient.getUsers(userIds)) {
            result.put(user.getId(), new UserShortDto(user.getId(), user.getName()));
        }
        return result;
    }

    @SuppressWarnings("unused")
    private Map<Long, UserShortDto> getShortMapFallback(List<Long> userIds, Throwable t) {
        log.warn("user-service недоступен при батч-получении пользователей {}: {}", userIds, t.toString());
        Map<Long, UserShortDto> result = new HashMap<>();
        for (Long id : userIds) {
            result.put(id, new UserShortDto(id, "Пользователь недоступен"));
        }
        return result;
    }

    /**
     * Для валидации перед записью (создание события) — деградация невозможна:
     * нельзя создать событие для непроверенного пользователя. 404 транслируется
     * в NotFoundException, реальная недоступность сервиса — в ServiceUnavailableException.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "assertExistsFallback")
    @Retry(name = CB)
    public void assertExists(long userId) {
        try {
            userClient.getUser(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Не найден пользователь с id: " + userId);
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