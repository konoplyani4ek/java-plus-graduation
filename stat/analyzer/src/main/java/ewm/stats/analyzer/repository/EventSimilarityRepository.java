package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.EventSimilarityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    List<EventSimilarityEntity> findByEventAOrEventB(Long eventA, Long eventB);

    Optional<EventSimilarityEntity> findByEventAAndEventB(Long eventA, Long eventB);
}