package ewm.main.stat;

import ewm.stat.client.model.GetStatsParams;

import java.util.Map;

public interface StatService {

    void saveHit(String uri, String ip);

    Map<String, Long> getViews(GetStatsParams params);
}