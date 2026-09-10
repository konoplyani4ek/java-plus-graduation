package ewm.event.server.service;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.dto.NewEventDto;
import ewm.event.server.dto.UpdateEventUserRequestDto;
import ewm.event.server.dto.search.PageParam;
import ewm.event.server.exception.ConflictException;
import ewm.event.server.exception.NotFoundException;
import ewm.event.server.mapper.EventMapper;
import ewm.event.server.model.Event;
import ewm.event.server.model.EventState;
import ewm.event.server.repository.EventRepository;
import ewm.request.dto.EventRequestStatusUpdateRequestDto;
import ewm.request.dto.EventRequestStatusUpdateResultDto;
import ewm.request.dto.InternalUpdateRequestStatusDto;
import ewm.request.dto.ParticipationRequestDto;
import jakarta.validation.ValidationException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class PrivateEventServiceImpl implements PrivateEventService {
    private final UserGateway userGateway;
    private final EventRepository eventRepository;
    private final CategoryGateway categoryGateway;
    private final RequestGateway requestGateway;
    private final EventDtoAssembler eventDtoAssembler;
    private final PlaceGateway placeGateway;

    @Override
    public EventFullDto getEventOfUserById(long userId, long eventId) {
        log.info("Получение события для userId: {}, eventId: {}", userId, eventId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        return eventDtoAssembler.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getAllByUserId(long userId, PageParam pageParam) {
        log.info("Получение событий для userId: {}, {}", userId, pageParam);

        Pageable pageable = PageRequest.of(pageParam.getFrom() / pageParam.getSize(), pageParam.getSize());

        List<Event> events = eventRepository.findByInitiatorIdOrderByEventDateAsc(userId, pageable);

        return eventDtoAssembler.toShortDtoList(events);
    }

    @Override
    public EventFullDto createEvent(long userId, NewEventDto dto) {
        log.info("Создание события для userId: {}, детали события: {}", userId, dto);

        validateEventDate(dto.getEventDate());

        userGateway.assertExists(userId);
        categoryGateway.assertExists(dto.getCategory());

        Event event = EventMapper.toEntity(dto, userId);
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

        Long newCategoryId = dto.getCategory();
        if (newCategoryId != null) {
            categoryGateway.assertExists(newCategoryId);
        }

        EventMapper.updateEntity(event, dto, newCategoryId);

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case "SEND_TO_REVIEW" -> event.setState(EventState.PENDING);
                case "CANCEL_REVIEW" -> event.setState(EventState.CANCELED);
                default -> throw new ValidationException("Недопустимое действие: " + dto.getStateAction());
            }
        }

        Event updatedEvent = eventRepository.save(event);

        return eventDtoAssembler.toFullDto(updatedEvent);
    }

    @Override
    public List<ParticipationRequestDto> getRequestsForEvent(long userId, long eventId) {
        log.info("Получение заявок на участие для userId: {} и eventId: {}", userId, eventId);

        if (eventRepository.findOneByInitiatorIdAndId(userId, eventId).isEmpty()) {
            return List.of();
        }

        return requestGateway.getRequestsForEvent(eventId);
    }

    @Override
    public EventRequestStatusUpdateResultDto setRequestsStatus(long userId, long eventId, EventRequestStatusUpdateRequestDto dto) {
        log.info("Установка статуса заявок для userId: {}, eventId: {}, dto: {}", userId, eventId, dto);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        InternalUpdateRequestStatusDto internalDto = InternalUpdateRequestStatusDto.builder()
                .requestIds(dto.getRequestIds())
                .status(dto.getStatus())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .build();

        return requestGateway.updateStatus(eventId, internalDto);
    }

    @Override
    public EventFullDto setPlace(long userId, long eventId, long placeId) {
        log.info("Привязка события с id: {} к месту: {}", eventId, placeId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        placeGateway.assertExists(placeId);

        event.setPlaceId(placeId);

        return eventDtoAssembler.toFullDto(eventRepository.save(event));
    }

    @Override
    public void removePlace(long userId, long eventId) {
        log.info("Отвязка места от события с id: {}", eventId);

        Event event = findEventByUserIdAndEventIdOrThrow(userId, eventId);

        checkEventIsEditable(event);

        event.setPlaceId(null);

        eventRepository.save(event);
    }

    private Event findEventByUserIdAndEventIdOrThrow(long userId, long eventId) {
        return eventRepository.findOneByInitiatorIdAndId(userId, eventId)
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