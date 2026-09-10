package ewm.event.server.repository;

import ewm.event.server.model.Event;
import ewm.event.server.model.EventState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    Page<Event> findAll(Specification<Event> specification, Pageable pageable);

    List<Event> findByInitiatorIdOrderByEventDateAsc(long userId, Pageable pageable);

    Optional<Event> findOneByInitiatorIdAndId(long userId, long eventId);

    Optional<Event> findOneByIdAndState(long id, EventState state);

    boolean existsByCategoryId(long categoryId);
}