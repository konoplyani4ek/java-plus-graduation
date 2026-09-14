package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.EventSimilarityEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, Long> {

    List<EventSimilarityEntity> findByEventAOrEventB(Long eventA, Long eventB);

    @Query("SELECT e FROM EventSimilarityEntity e WHERE e.eventA IN :eventIds OR e.eventB IN :eventIds")
    List<EventSimilarityEntity> findByEventAInOrEventBIn(@Param("eventIds") List<Long> eventIds);

    Optional<EventSimilarityEntity> findByEventAAndEventB(Long eventA, Long eventB);
}