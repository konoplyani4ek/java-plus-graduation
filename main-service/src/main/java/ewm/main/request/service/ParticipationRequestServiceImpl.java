package ewm.main.request.service;

import ewm.main.dto.ParticipationRequestDto;
import ewm.main.event.model.Event;
import ewm.main.event.model.EventState;
import ewm.main.event.repository.EventRepository;
import ewm.main.exception.ConflictException;
import ewm.main.exception.NotFoundException;
import ewm.main.request.mapper.ParticipationRequestMapper;
import ewm.main.request.model.ParticipationRequest;
import ewm.main.request.model.RequestStatus;
import ewm.main.request.repository.ParticipationRequestRepository;
import ewm.main.user.User;
import ewm.main.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Override
    public List<ParticipationRequestDto> getRequests(long userId) {
        log.info("Getting requests for userId: {}", userId);
        findUserOrThrow(userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(ParticipationRequestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto addRequest(long userId, long eventId) {
        log.info("Adding request from userId: {} to eventId: {}", userId, eventId);

        User requester = findUserOrThrow(userId);
        Event event = findEventOrThrow(eventId);

        // нельзя участвовать в своём событии
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Нельзя подать заявку на участие в своём событии");
        }

        // событие должно быть опубликовано
        if (!EventState.PUBLISHED.equals(event.getState())) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        // нельзя подать повторную заявку
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Заявка на участие уже существует");
        }

        // проверка лимита участников (0 = без ограничений)
        int limit = event.getParticipantLimit();
        if (limit > 0) {
            long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
            if (confirmed >= limit) {
                throw new ConflictException("Достигнут лимит участников события");
            }
        }

        // если модерация отключена или количество участников не ограничено — сразу подтверждаем
        RequestStatus status = RequestStatus.PENDING;
        if (!event.isRequestModeration() || limit == 0) {
            status = RequestStatus.CONFIRMED;
        }

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(requester)
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

        findUserOrThrow(userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Заявка с id=" + requestId + " не найдена"));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Нельзя отменить чужую заявку");
        }

        request.setStatus(RequestStatus.CANCELED);
        return ParticipationRequestMapper.toDto(requestRepository.save(request));
    }

    private User findUserOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
    }

    private Event findEventOrThrow(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));
    }
}