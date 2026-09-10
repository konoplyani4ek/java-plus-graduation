package ewm.request.server.service;

import ewm.event.dto.EventInternalDto;
import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.request.dto.EventRequestStatusUpdateResultDto;
import ewm.request.dto.InternalUpdateRequestStatusDto;
import ewm.request.dto.ParticipationRequestDto;
import ewm.request.server.exception.ConflictException;
import ewm.request.server.exception.NotFoundException;
import ewm.request.server.mapper.ParticipationRequestMapper;
import ewm.request.server.model.ParticipationRequest;
import ewm.request.server.model.RequestStatus;
import ewm.request.server.repository.EventConfirmedRequestsCount;
import ewm.request.server.repository.ParticipationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private static final String PUBLISHED = "PUBLISHED";

    private final ParticipationRequestRepository requestRepository;
    private final UserGateway userGateway;
    private final EventGateway eventGateway;

    @Override
    public List<ParticipationRequestDto> getRequests(long userId) {
        log.info("Getting requests for userId: {}", userId);
        userGateway.assertExists(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(long userId, long eventId) {
        log.info("Adding request from userId: {} to eventId: {}", userId, eventId);

        userGateway.assertExists(userId);
        EventInternalDto event = eventGateway.getOrThrow(eventId);

        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("Нельзя подать заявку на участие в своём событии");
        }

        if (!PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Заявка на участие уже существует");
        }

        int limit = event.getParticipantLimit();
        if (limit > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= limit) {
                throw new ConflictException("Достигнут лимит участников события");
            }
        }

        RequestStatus status = RequestStatus.PENDING;
        if (!Boolean.TRUE.equals(event.getRequestModeration()) || limit == 0) {
            status = RequestStatus.CONFIRMED;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .eventId(eventId)
                .requesterId(userId)
                .status(status)
                .created(LocalDateTime.now())
                .build();

        ParticipationRequestDto result = ParticipationRequestMapper.toDto(requestRepository.save(request));
        log.info("Request created with id: {}", result.getId());
        return result;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        log.info("Cancelling requestId: {} by userId: {}", requestId, userId);

        userGateway.assertExists(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("Нельзя отменить чужую заявку");
        }

        request.setStatus(RequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    public Long getConfirmedCount(long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
    }

    @Override
    public List<EventConfirmedRequestsCountDto> getConfirmedCounts(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return List.of();
        }

        List<EventConfirmedRequestsCount> counts =
                requestRepository.countConfirmedRequestsByEventIds(eventIds, RequestStatus.CONFIRMED);

        Map<Long, Long> byEventId = new HashMap<>();
        for (EventConfirmedRequestsCount c : counts) {
            byEventId.put(c.getEventId(), c.getConfirmedRequests());
        }

        List<EventConfirmedRequestsCountDto> result = new ArrayList<>();
        for (Long eventId : eventIds) {
            result.add(new EventConfirmedRequestsCountDto(eventId, byEventId.getOrDefault(eventId, 0L)));
        }
        return result;
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto updateStatus(long eventId, InternalUpdateRequestStatusDto dto) {
        RequestStatus statusToUpdate = RequestStatus.parse(dto.getStatus());

        if (statusToUpdate != RequestStatus.CONFIRMED && statusToUpdate != RequestStatus.REJECTED) {
            throw new ConflictException("Недопустимый статус для этой операции:" + statusToUpdate);
        }

        List<Long> requestIds = dto.getRequestIds();
        if (requestIds.isEmpty()) {
            return new EventRequestStatusUpdateResultDto(Collections.emptyList(), Collections.emptyList());
        }

        List<Long> distinctIds = requestIds.stream().distinct().toList();

        if (statusToUpdate == RequestStatus.CONFIRMED) {
            return confirmRequests(eventId, distinctIds, dto.getParticipantLimit(), dto.getRequestModeration());
        }

        return rejectRequests(eventId, distinctIds);
    }

    private EventRequestStatusUpdateResultDto confirmRequests(long eventId, List<Long> requestIds,
                                                              Integer participantLimit, Boolean requestModeration) {
        int limit = participantLimit == null ? 0 : participantLimit;
        if (!Boolean.TRUE.equals(requestModeration) || limit == 0) {
            throw new ConflictException("Подтверждение заявок не требуется");
        }

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        long available = limit - confirmedRequests;

        if (requestIds.size() > available) {
            throw new ConflictException("Превышен лимит участников");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(requestIds, eventId);
        if (requests.size() < requestIds.size()) {
            throw new NotFoundException("Найдены не все заявки");
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Нельзя изменить статус заявки с id: " + request.getId()
                        + ", она в статусе:" + request.getStatus());
            }
            request.setStatus(RequestStatus.CONFIRMED);
            confirmed.add(request);
        }

        if (available != 0 && available == requestIds.size()) {
            List<ParticipationRequest> toReject =
                    requestRepository.findAllByEventIdAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequest request : toReject) {
                request.setStatus(RequestStatus.REJECTED);
                rejected.add(request);
            }
        }

        EventRequestStatusUpdateResultDto result = new EventRequestStatusUpdateResultDto();
        result.setConfirmedRequests(confirmed.stream().map(ParticipationRequestMapper::toDto).toList());
        result.setRejectedRequests(rejected.stream().map(ParticipationRequestMapper::toDto).toList());
        return result;
    }

    private EventRequestStatusUpdateResultDto rejectRequests(long eventId, List<Long> requestIds) {
        List<ParticipationRequest> requests = requestRepository.findAllByIdInAndEventId(requestIds, eventId);
        if (requests.size() < requestIds.size()) {
            throw new NotFoundException("Найдены не все заявки");
        }

        List<ParticipationRequest> rejected = new ArrayList<>();
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Нельзя изменить статус заявки с id: " + request.getId()
                        + ", она в статусе:" + request.getStatus());
            }
            request.setStatus(RequestStatus.REJECTED);
            rejected.add(request);
        }

        EventRequestStatusUpdateResultDto result = new EventRequestStatusUpdateResultDto();
        result.setConfirmedRequests(Collections.emptyList());
        result.setRejectedRequests(rejected.stream().map(ParticipationRequestMapper::toDto).toList());
        return result;
    }
}