package ewm.main.event.service;

import ewm.main.dto.EventRequestStatusUpdateRequestDto;
import ewm.main.dto.EventRequestStatusUpdateResultDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.ParticipationRequestDto;
import ewm.main.dto.UpdateEventUserRequestDto;
import ewm.main.event.mapper.EventMapper;
import ewm.main.event.model.Event;
import ewm.main.event.model.EventState;
import ewm.main.dto.search.PageParam;
import ewm.main.event.repository.EventRepository;
import ewm.main.exception.ConflictException;
import ewm.main.place.Place;
import ewm.main.place.repository.PlaceRepository;
import ewm.main.request.mapper.ParticipationRequestMapper;
import ewm.main.request.model.ParticipationRequest;
import ewm.main.request.model.RequestStatus;
import ewm.main.request.repository.ParticipationRequestRepository;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ewm.main.category.Category;
import ewm.main.category.repository.CategoryRepository;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.NewEventDto;
import ewm.main.exception.NotFoundException;
import ewm.main.user.User;
import ewm.main.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final EventDtoAssembler eventDtoAssembler;
    private final PlaceRepository placeRepository;

    @Override
    public EventFullDto getEventOfUserById(long userId, long eventId) {
        log.info("Получение события для userId: {}, eventId: {}", userId, eventId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        log.info("Событие успешно получено, eventId: {}", eventId);

        return eventDtoAssembler.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllByUserId(long userId, PageParam pageParam) {
        log.info("Получение событий для userId: {}, с: {}, размер: {}", userId, pageParam);

        Pageable pageable = PageRequest.of(pageParam.getFrom() / pageParam.getSize(), pageParam.getSize());

        List<Event> events = eventRepository.findByInitiator_IdOrderByEventDateAsc(userId, pageable);

        log.info("Количество событий: {}", events.size());

        return eventDtoAssembler.toShortDtoList(events);
    }

    @Override
    public EventFullDto createEvent(long userId, NewEventDto dto) {
        log.info("Создание события для userId: {}, детали события: {}", userId, dto);

        validateEventDate(dto.getEventDate());

        User user = findUserByIdOrThrow(userId);

        Category category = findCategoryByIdOrThrow(dto.getCategory());

        Event event = EventMapper.toEntity(dto, category, user);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        Event savedEvent = eventRepository.save(event);
        log.info("Событие успешно создано с id: {}", savedEvent.getId());

        return eventDtoAssembler.toFullDto(savedEvent);
    }

    @Override
    public EventFullDto updateEventOfUser(long userId, long eventId, UpdateEventUserRequestDto dto) {
        log.info("Обновление события для userId: {}, eventId: {}, детали обновления: {}", userId, eventId, dto);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        validateEventDate(event.getEventDate());

        Category category = dto.getCategory() != null ? findCategoryByIdOrThrow(dto.getCategory()) : null;

        EventMapper.updateEntity(event, dto, category);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "SEND_TO_REVIEW" -> event.setState(EventState.PENDING);
                case "CANCEL_REVIEW" -> event.setState(EventState.CANCELED);
                default -> throw new ValidationException("Недопустимое действие: " + dto.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);
        log.info("Событие успешно обновлено с id: {}", updatedEvent.getId());

        return eventDtoAssembler.toFullDto(updatedEvent);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(long userId, long eventId) {
        log.info("Получение заявок на участие для userId: {} и eventId: {}", userId, eventId);

        if (eventRepository.findOneByInitiator_IdAndId(userId, eventId).isEmpty()) {
            return Collections.emptyList();
        }

        return participationRequestRepository.findAllByEventId(eventId)
                .stream()
                .map(ParticipationRequestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto setRequestsStatus(long userId, long eventId, EventRequestStatusUpdateRequestDto dto) {
        log.info("Установка статуса заявок на участие для userId: {}, eventId: {}, dto: ", userId, eventId, dto);
        RequestStatus statusToUpdate = RequestStatus.parse(dto.getStatus());

        if (statusToUpdate != RequestStatus.CONFIRMED && statusToUpdate != RequestStatus.REJECTED) {
            throw new ConflictException("Недопустимый статус для этой операции:" + statusToUpdate);
        }

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        List<Long> requestIds = dto.getRequestIds();
        if (requestIds.isEmpty()) {
            return new EventRequestStatusUpdateResultDto(Collections.emptyList(),
                    Collections.emptyList());
        }

        List<Long> distinctIds = dto.getRequestIds().stream().distinct().toList();

        if (statusToUpdate == RequestStatus.CONFIRMED) {
            return confirmRequests(event, distinctIds);
        }

        return rejectRequests(event, distinctIds);
    }

    @Override
    public EventFullDto setPlace(long userId, long eventId, long placeId) {
        log.info("Привязка события с id: {} к месту: {}", eventId, placeId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("Не найдено место: " + placeId));

        event.setPlace(place);

        return eventDtoAssembler.toFullDto(eventRepository.save(event));
    }

    @Override
    public void removePlace(long userId, long eventId) {
        log.info("Отвязка места от события с id: {}", eventId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        event.setPlace(null);

        eventRepository.save(event);
    }

    private EventRequestStatusUpdateResultDto confirmRequests(Event event, List<Long> requestIds) {

        int participantsLimit = event.getParticipantLimit();
        if (!event.isRequestModeration() || participantsLimit == 0) {
            throw new ConflictException("Подтверждение заявок не требуется");
        }

        long confirmedRequests = participationRequestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long available = participantsLimit - confirmedRequests;

        if (requestIds.size() > available) {
            throw new ConflictException("Превышен лимит участников");
        }

        List<ParticipationRequest> requests = participationRequestRepository.findAllByIdInAndEvent_Id(requestIds, event.getId());
        if (requests.size() < requestIds.size()) {
            throw new NotFoundException("Найдены не все заявки");
        }

        List<ParticipationRequest> confirmRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            RequestStatus currentStatus = request.getStatus();
            if (currentStatus != RequestStatus.PENDING) {
                throw new ConflictException("Нельзя изменить статус заявки с id: " + request.getId() + ", она в статусе:" + currentStatus);
            }

            request.setStatus(RequestStatus.CONFIRMED);
            confirmRequests.add(request);
        }

        if (available != 0 && available == requestIds.size()) {
            List<ParticipationRequest> requestsToReject = participationRequestRepository.findAllByEvent_IdAndStatus(event.getId(),
                    RequestStatus.PENDING);
            for (ParticipationRequest request : requestsToReject) {
                request.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(request);
            }
        }

        EventRequestStatusUpdateResultDto resultDto = new EventRequestStatusUpdateResultDto();
        resultDto.setConfirmedRequests(confirmRequests.stream().map(ParticipationRequestMapper::toDto).toList());
        resultDto.setRejectedRequests(rejectedRequests.stream().map(ParticipationRequestMapper::toDto).toList());

        log.info("Результат установки статуса CONFIRMED: {}", resultDto);

        return resultDto;
    }

    private EventRequestStatusUpdateResultDto rejectRequests(Event event, List<Long> requestIds) {

        List<ParticipationRequest> requests = participationRequestRepository.findAllByIdInAndEvent_Id(requestIds, event.getId());
        if (requests.size() < requestIds.size()) {
            throw new NotFoundException("Найдены не все заявки");
        }

        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest request : requests) {
            RequestStatus currentStatus = request.getStatus();
            if (currentStatus != RequestStatus.PENDING) {
                throw new ConflictException("Нельзя изменить статус заявки с id: " + request.getId() + ", она в статусе:" + currentStatus);
            }

            request.setStatus(RequestStatus.REJECTED);
            rejectedRequests.add(request);
        }

        EventRequestStatusUpdateResultDto resultDto = new EventRequestStatusUpdateResultDto();
        resultDto.setConfirmedRequests(Collections.emptyList());
        resultDto.setRejectedRequests(rejectedRequests.stream().map(ParticipationRequestMapper::toDto).toList());

        log.info("Результат установки статуса REJECTED: {}", resultDto);

        return resultDto;
    }

    private User findUserByIdOrThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Не найден пользователь с id: " + userId));
    }

    private Category findCategoryByIdOrThrow(long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Не найдена категория с id: " + categoryId));
    }

    private Event findEventByUserIdAndEventIdOrThrow(long userId, long eventId) {
        return eventRepository.findOneByInitiator_IdAndId(userId, eventId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("У пользователя с id: %d нет события с id: %d", userId, eventId)));
    }

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate != null && eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Дата события должна быть не ранее чем через 2 часа от текущего момента");
        }
    }

    private void checkEventIsEditable(Event event) {
        if (event.getState() != EventState.PENDING && event.getState() != EventState.CANCELED) {
            throw new ConflictException("Можно изменять события только в статусах PENDING и CANCELED");
        }
    }

}
