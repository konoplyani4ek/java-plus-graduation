package ewm.stats.analyzer.repository;

import ewm.stats.analyzer.model.UserActionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserActionRepository extends JpaRepository<UserActionEntity, Long> {

    Optional<UserActionEntity> findByUserIdAndEventId(Long userId, Long eventId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    List<UserActionEntity> findByUserIdOrderByActionTimestampDesc(Long userId, Pageable pageable);

    @Query("SELECT ua.eventId FROM UserActionEntity ua WHERE ua.userId = :userId")
    Set<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT ua.eventId as eventId, SUM(ua.weight) as totalWeight "
            + "FROM UserActionEntity ua WHERE ua.eventId IN :eventIds GROUP BY ua.eventId")
    List<EventWeightSum> sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventWeightSum {
        Long getEventId();

        Double getTotalWeight();
    }
}