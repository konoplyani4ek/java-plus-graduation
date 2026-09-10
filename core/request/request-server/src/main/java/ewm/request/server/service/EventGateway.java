package ewm.request.server.service;

import ewm.event.client.EventClient;
import ewm.event.dto.EventInternalDto;
import ewm.request.server.exception.NotFoundException;
import ewm.request.server.exception.ServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventGateway {

    private static final String CB = "event-client";

    private final EventClient eventClient;

    @CircuitBreaker(name = CB, fallbackMethod = "getOrThrowFallback")
    @Retry(name = CB)
    public EventInternalDto getOrThrow(long eventId) {
        try {
            return eventClient.getEvent(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
    }

    @SuppressWarnings("unused")
    private EventInternalDto getOrThrowFallback(long eventId, Throwable t) {
        if (t instanceof NotFoundException notFound) {
            throw notFound;
        }
        log.warn("event-service недоступен при получении события {}: {}", eventId, t.toString());
        throw new ServiceUnavailableException("Сервис событий временно недоступен, попробуйте позже");
    }
}