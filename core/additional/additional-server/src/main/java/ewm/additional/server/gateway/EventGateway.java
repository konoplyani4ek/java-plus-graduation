package ewm.additional.server.gateway;

import ewm.additional.server.exception.ServiceUnavailableException;
import ewm.event.client.EventClient;
import ewm.event.dto.EventSummaryDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Шлюз к event-service. Отдельный бин: аннотации Resilience4j работают через
 * Spring AOP-прокси и не сработают при self-invocation.
 *
 * Единственная внешняя зависимость additional-service, но используется по двум
 * принципиально разным сценариям — см. комментарии к каждому методу.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventGateway {

    private static final String CB = "event-client";

    private final EventClient eventClient;

    /**
     * Проверка перед удалением категории — деградация здесь означает fail-closed:
     * если event-service недоступен и мы не можем проверить, используется ли
     * категория, правильнее отказать в удалении, чем рискнуть снести категорию,
     * которая на самом деле используется. "Тихого разрешения" тут быть не должно.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "existsByCategoryFallback")
    @Retry(name = CB)
    public boolean existsByCategory(long categoryId) {
        return eventClient.existsByCategory(categoryId);
    }

    @SuppressWarnings("unused")
    private boolean existsByCategoryFallback(long categoryId, Throwable t) {
        log.warn("event-service недоступен при проверке категории {}: {}", categoryId, t.toString());
        throw new ServiceUnavailableException(
                "Не удалось проверить, используется ли категория — сервис событий временно недоступен");
    }

    /**
     * Карточки событий для отображения подборки — read-only, деградация уместна:
     * лучше показать подборку без карточек событий, чем не показать её вовсе.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getSummariesFallback")
    @Retry(name = CB)
    public List<EventSummaryDto> getSummaries(List<Long> eventIds) {
        return eventClient.getSummaries(eventIds);
    }

    @SuppressWarnings("unused")
    private List<EventSummaryDto> getSummariesFallback(List<Long> eventIds, Throwable t) {
        log.warn("event-service недоступен при получении карточек событий {}: {}", eventIds, t.toString());
        return List.of();
    }
}