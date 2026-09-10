package ewm.stat.client;

import ewm.stat.client.model.GetStatsParams;
import ewm.stat.client.model.HitParams;
import ewm.stat.dto.StatDto;

import java.util.List;

public interface StatClient {

    void saveHit(HitParams params);

    List<StatDto> getStats(GetStatsParams params);

}
