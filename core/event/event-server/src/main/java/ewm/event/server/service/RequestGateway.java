package ewm.event.server.service;

import ewm.event.server.exception.ConflictException;
import ewm.event.server.exception.NotFoundException;
import ewm.event.server.exception.ServiceUnavailableException;
import ewm.request.client.RequestClient;
import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.request.dto.EventRequestStatusUpdateResultDto;
import ewm.request.dto.InternalUpdateRequestStatusDto;
import ewm.request.dto.ParticipationRequestDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Шлюз к request-service. Отдельный бин по той же причине, что и остальные —
 * self-invocation ломает Resilience4j-аннотации.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestGateway {

    private static final String CB = "request-client";

    private final RequestClient requestClient;

    /**
     * confirmedRequests — необязательное поле для отображения, при недоступности
     * request-service подставляем 0, а не роняем весь ответ.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getConfirmedCountFallback")
    @Retry(name = CB)
    public Long getConfirmedCount(long eventId) {
        return requestClient.getConfirmedCount(eventId);
    }

    @SuppressWarnings("unused")
    private Long getConfirmedCountFallback(long eventId, Throwable t) {
        log.warn("request-service недоступен при подсчёте заявок события {}: {}", eventId, t.toString());
        return 0L;
    }

    /**
     * Батч-версия для списков событий — один запрос вместо N (проблема N+1).
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getConfirmedCountsFallback")
    @Retry(name = CB)
    public List<EventConfirmedRequestsCountDto> getConfirmedCounts(List<Long> eventIds) {
        return requestClient.getConfirmedCounts(eventIds);
    }

    @SuppressWarnings("unused")
    private List<EventConfirmedRequestsCountDto> getConfirmedCountsFallback(List<Long> eventIds, Throwable t) {
        log.warn("request-service недоступен при батч-подсчёте заявок {}: {}", eventIds, t.toString());
        List<EventConfirmedRequestsCountDto> result = new ArrayList<>();
        for (Long id : eventIds) {
            result.add(new EventConfirmedRequestsCountDto(id, 0L));
        }
        return result;
    }

    /**
     * Список заявок на своё событие — read-only, для владельца события.
     * При недоступности request-service возвращаем пустой список, а не 5xx:
     * лучше показать событие без заявок, чем не показать ничего.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getRequestsForEventFallback")
    @Retry(name = CB)
    public List<ParticipationRequestDto> getRequestsForEvent(long eventId) {
        return requestClient.getRequestsForEvent(eventId);
    }

    @SuppressWarnings("unused")
    private List<ParticipationRequestDto> getRequestsForEventFallback(long eventId, Throwable t) {
        log.warn("request-service недоступен при получении заявок события {}: {}", eventId, t.toString());
        return List.of();
    }

    /**
     * Подтверждение/отклонение заявок — это запись, деградация невозможна:
     * нельзя молча "притвориться", что заявки подтверждены. 404/409 от
     * request-service — легитимные бизнес-ответы, пробрасываются как есть;
     * реальная недоступность сервиса — в ServiceUnavailableException.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "updateStatusFallback")
    @Retry(name = CB)
    public EventRequestStatusUpdateResultDto updateStatus(long eventId, InternalUpdateRequestStatusDto dto) {
        try {
            return requestClient.updateStatus(eventId, dto);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Заявки для события " + eventId + " не найдены");
        } catch (FeignException.Conflict e) {
            throw new ConflictException(e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private EventRequestStatusUpdateResultDto updateStatusFallback(long eventId, InternalUpdateRequestStatusDto dto, Throwable t) {
        if (t instanceof NotFoundException || t instanceof ConflictException) {
            throw (RuntimeException) t;
        }
        log.warn("request-service недоступен при изменении статуса заявок события {}: {}", eventId, t.toString());
        throw new ServiceUnavailableException("Сервис заявок временно недоступен, попробуйте позже");
    }
}