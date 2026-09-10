package ewm.stat.server.service;

import ewm.stat.dto.HitDto;
import ewm.stat.dto.StatDto;
import ewm.stat.server.exception.ValidationException;
import ewm.stat.server.storage.StatStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class StatServiceImpl implements StatService {

    @Autowired
    private StatStorage statStorage;

    private static final Logger log = LoggerFactory.getLogger(StatServiceImpl.class);

    @Override
    public void saveHit(HitDto hit) {
        log.info("Saving hit: {}", hit);
        statStorage.save(hit);
    }

    @Override
    public List<StatDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique, List<String> uris) {
        log.info("Fetching stats from {} to {} with unique={} and uris={}", start, end, unique, uris);
        if (start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты конца");
        }
        return statStorage.getStats(start, end, unique, uris);
    }
}
