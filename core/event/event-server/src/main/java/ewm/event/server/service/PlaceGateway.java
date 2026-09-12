package ewm.event.server.service;

import ewm.event.server.exception.NotFoundException;
import ewm.event.server.exception.ServiceUnavailableException;
import ewm.place.client.PlaceClient;
import ewm.place.dto.PlaceDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Шлюз к location-service. Отдельный бин по той же причине, что и
 * UserGateway/CategoryGateway — self-invocation ломает Resilience4j-аннотации.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceGateway {

    private static final String CB = "place-client";

    private final PlaceClient placeClient;

    /**
     * Для отображения (EventFullDto.place) — поле необязательное, поэтому при
     * недоступности location-service просто не показываем место (null),
     * а не роняем весь ответ и не выдумываем заглушку.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getFallback")
    @Retry(name = CB)
    public PlaceDto get(long placeId) {
        return placeClient.getPlace(placeId);
    }

    @SuppressWarnings("unused")
    private PlaceDto getFallback(long placeId, Throwable t) {
        log.warn("location-service недоступен при получении места {}: {}", placeId, t.toString());
        return null;
    }

    /**
     * Для поиска по радиусу (searchEvents/getEvents) — в отличие от get(), здесь
     * координаты места обязательны для расчёта радиуса, деградация до null не подходит:
     * без данных места радиус-поиск в принципе невозможен выполнить корректно.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getOrThrowFallback")
    @Retry(name = CB)
    public PlaceDto getOrThrow(long placeId) {
        try {
            return placeClient.getPlace(placeId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Не найдено место с id: " + placeId);
        }
    }

    @SuppressWarnings("unused")
    private PlaceDto getOrThrowFallback(long placeId, Throwable t) {
        if (t instanceof NotFoundException notFound) {
            throw notFound;
        }
        log.warn("location-service недоступен при получении места {} для поиска: {}", placeId, t.toString());
        throw new ServiceUnavailableException("Сервис мест временно недоступен, попробуйте позже");
    }

    /**
     * Для валидации перед записью (привязка события к месту) — деградация
     * невозможна: нельзя привязать событие к непроверенному месту.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "assertExistsFallback")
    @Retry(name = CB)
    public void assertExists(long placeId) {
        try {
            placeClient.getPlace(placeId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Не найдено место с id: " + placeId);
        }
    }

    @SuppressWarnings("unused")
    private void assertExistsFallback(long placeId, Throwable t) {
        if (t instanceof NotFoundException notFound) {
            throw notFound;
        }
        log.warn("location-service недоступен при проверке места {}: {}", placeId, t.toString());
        throw new ServiceUnavailableException("Сервис мест временно недоступен, попробуйте позже");
    }
}