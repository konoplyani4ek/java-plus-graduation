package ewm.main.stat;

import ewm.stat.client.StatClient;
import ewm.stat.client.exception.StatClientException;
import ewm.stat.client.model.GetStatsParams;
import ewm.stat.client.model.HitParams;
import ewm.stat.dto.StatDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StatServiceImpl implements StatService {

    private final StatClient statClient;

    public StatServiceImpl(StatClient statClient) {
        this.statClient = statClient;
    }

    @Override
    public void saveHit(String uri, String ip) {
        HitParams params = HitParams.builder()
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            statClient.saveHit(params);
        } catch (StatClientException e) {
            log.error("Ошибка работы statClient.saveHit: {}", e.getMessage());
        }
    }

    @Override
    public Map<String, Long> getViews(GetStatsParams params) {
        try {
            List<StatDto> statResult = statClient.getStats(params);
            return toViewsByUri(statResult);
        } catch (StatClientException e) {
            log.error("Ошибка работы statClient.getStats: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Long> toViewsByUri(List<StatDto> statResult) {
        Map<String, Long> viewsByUri = new HashMap<>();

        for (StatDto statDto : statResult) {
            String uri = statDto.getUri();
            Long hits = statDto.getHits();

            viewsByUri.merge(uri, hits, Long::sum);
        }

        return viewsByUri;
    }
}