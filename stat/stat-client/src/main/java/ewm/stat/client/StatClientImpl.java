package ewm.stat.client;

import ewm.stat.client.exception.StatClientException;
import ewm.stat.client.mapper.HitMapper;
import ewm.stat.client.model.GetStatsParams;
import ewm.stat.client.model.HitParams;
import ewm.stat.dto.HitDto;
import ewm.stat.dto.StatDto;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public class StatClientImpl implements StatClient {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 500L;

    private final String app;
    private final String serviceId;
    private final DiscoveryClient discoveryClient;
    private final SimpleClientHttpRequestFactory requestFactory;

    public StatClientImpl(String app, String serviceId, DiscoveryClient discoveryClient, int timeoutMs) {
        this.app = app;
        this.serviceId = serviceId;
        this.discoveryClient = discoveryClient;

        this.requestFactory = new SimpleClientHttpRequestFactory();
        if (timeoutMs > 0) {
            requestFactory.setConnectTimeout(timeoutMs);
            requestFactory.setReadTimeout(timeoutMs);
        }
    }

    @Override
    public void saveHit(HitParams params) {
        HitDto dto = HitMapper.fromHitParams(params, app);

        withRetry(restClient -> {
            restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .toBodilessEntity();
            return null;
        });
    }

    @Override
    public List<StatDto> getStats(GetStatsParams params) {
        String path = buildStatsPath(params);

        return withRetry(restClient -> restClient.get()
                .uri(path)
                .retrieve()
                .body(new ParameterizedTypeReference<List<StatDto>>() {
                }));
    }

    private <T> T withRetry(Function<RestClient, T> action) {
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                RestClient restClient = RestClient.builder()
                        .baseUrl(resolveBaseUrl())
                        .requestFactory(requestFactory)
                        .build();

                return action.apply(restClient);
            } catch (RuntimeException e) {
                lastError = e;
                if (attempt < MAX_ATTEMPTS) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        throw toStatClientException(lastError);
    }

    private String resolveBaseUrl() {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);

        if (instances.isEmpty()) {
            throw new StatClientException(
                    "Сервис '" + serviceId + "' не найден в Eureka (нет зарегистрированных инстансов)");
        }

        ServiceInstance instance = instances.get(0);
        return instance.getUri().toString();
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(RETRY_DELAY_MS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StatClientException("Повторная попытка обращения к stat-server прервана", e);
        }
    }

    private StatClientException toStatClientException(RuntimeException e) {
        if (e instanceof StatClientException statClientException) {
            return statClientException;
        }

        if (e instanceof RestClientResponseException responseException) {
            return new StatClientException(String.format("HTTP code: %s, body: %s",
                    responseException.getStatusCode(),
                    responseException.getResponseBodyAsString()), e);
        }

        if (e instanceof RestClientException) {
            return new StatClientException("Ошибка вызова сервиса статистики: " + e.getMessage(), e);
        }

        return new StatClientException("Неизвестная ошибка обращения к stat-server: " + e.getMessage(), e);
    }

    private String buildStatsPath(GetStatsParams params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/stats");
        LocalDateTime start = params.getStart();
        LocalDateTime end = params.getEnd();

        if (start != null) {
            builder.queryParam("start", start.format(DATE_TIME_FORMATTER));
        }
        if (end != null) {
            builder.queryParam("end", end.format(DATE_TIME_FORMATTER));
        }

        Boolean unique = params.getUnique();
        if (unique != null) {
            builder.queryParam("unique", unique);
        }

        List<String> uris = params.getUris();
        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", uris.toArray());
        }

        return builder.build().toUriString();
    }
}