package ewm.event.server.service;

import ewm.category.client.CategoryClient;
import ewm.category.dto.CategoryDto;
import ewm.event.server.exception.NotFoundException;
import ewm.event.server.exception.ServiceUnavailableException;
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
 * Шлюз к category-service. Отдельный бин по той же причине, что и
 * UserGateway — self-invocation ломает работу аннотаций Resilience4j.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryGateway {

    private static final String CB = "category-client";

    private final CategoryClient categoryClient;

    /**
     * Для отображения (EventFullDto/EventShortDto.category) — при недоступности
     * category-service возвращаем заглушку, а не роняем весь ответ.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getFallback")
    @Retry(name = CB)
    public CategoryDto get(long categoryId) {
        return categoryClient.getCategory(categoryId);
    }

    @SuppressWarnings("unused")
    private CategoryDto getFallback(long categoryId, Throwable t) {
        log.warn("category-service недоступен при получении категории {}: {}", categoryId, t.toString());
        return CategoryDto.builder().id(categoryId).name("Категория недоступна").build();
    }

    /**
     * Батч-версия для списков событий — один запрос вместо N (проблема N+1).
     */
    @CircuitBreaker(name = CB, fallbackMethod = "getMapFallback")
    @Retry(name = CB)
    public Map<Long, CategoryDto> getMap(List<Long> categoryIds) {
        Map<Long, CategoryDto> result = new HashMap<>();
        for (CategoryDto category : categoryClient.getCategories(categoryIds)) {
            result.put(category.getId(), category);
        }
        return result;
    }

    @SuppressWarnings("unused")
    private Map<Long, CategoryDto> getMapFallback(List<Long> categoryIds, Throwable t) {
        log.warn("category-service недоступен при батч-получении категорий {}: {}", categoryIds, t.toString());
        Map<Long, CategoryDto> result = new HashMap<>();
        for (Long id : categoryIds) {
            result.put(id, CategoryDto.builder().id(id).name("Категория недоступна").build());
        }
        return result;
    }

    /**
     * Для валидации перед записью (создание/обновление события) — деградация
     * невозможна: нельзя привязать событие к непроверенной категории.
     */
    @CircuitBreaker(name = CB, fallbackMethod = "assertExistsFallback")
    @Retry(name = CB)
    public void assertExists(long categoryId) {
        try {
            categoryClient.getCategory(categoryId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Не найдена категория с id: " + categoryId);
        }
    }

    @SuppressWarnings("unused")
    private void assertExistsFallback(long categoryId, Throwable t) {
        if (t instanceof NotFoundException notFound) {
            throw notFound;
        }
        log.warn("category-service недоступен при проверке категории {}: {}", categoryId, t.toString());
        throw new ServiceUnavailableException("Сервис категорий временно недоступен, попробуйте позже");
    }
}