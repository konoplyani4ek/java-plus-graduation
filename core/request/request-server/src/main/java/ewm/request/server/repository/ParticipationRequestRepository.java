package ewm.request.server.repository;

import ewm.request.server.model.ParticipationRequest;
import ewm.request.server.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    // все заявки конкретного пользователя
    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    // проверка дубля: один юзер — одна заявка на событие
    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    // количество подтверждённых заявок на событие (для проверки лимита)
    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByIdInAndEventId(List<Long> ids, long eventId);

    List<ParticipationRequest> findAllByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("""
        select r.eventId as eventId,
               count(r.id) as confirmedRequests
        from ParticipationRequest r
        where r.eventId in :eventIds
          and r.status = :status
        group by r.eventId
        """)
    List<EventConfirmedRequestsCount> countConfirmedRequestsByEventIds(
            @Param("eventIds") List<Long> eventIds,
            @Param("status") RequestStatus status
    );
}