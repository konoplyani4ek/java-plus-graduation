package ewm.stat.server.service;

import ewm.stat.dto.HitDto;
import ewm.stat.dto.StatDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatService {

    void saveHit(HitDto dto);

    List<StatDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique, List<String> uris);

}
