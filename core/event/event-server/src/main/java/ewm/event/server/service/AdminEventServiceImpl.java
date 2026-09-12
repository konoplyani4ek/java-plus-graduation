package ewm.event.server.service;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.UpdateEventAdminRequestDto;
import ewm.event.server.dto.search.AdminEventSearchParam;
import ewm.event.server.dto.search.PageParam;
import ewm.event.server.exception.ConflictException;
import ewm.event.server.exception.NotFoundException;
import ewm.event.server.mapper.EventMapper;
import ewm.event.server.model.Event;
import ewm.event.server.model.EventState;
import ewm.event.server.model.EventStateAction;
import ewm.event.server.repository.EventRepository;
import ewm.event.server.repository.EventSpecifications;
import ewm.place.dto.PlaceDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {
    private final EventRepository eventRepository;
    private final CategoryGateway categoryGateway;
    private final EventDtoAssembler eventDtoAssembler;
    private final PlaceGateway placeGateway;

    @Override
    public List<EventFullDto> searchEvents(AdminEventSearchParam searchParam, PageParam pageParam) {
        log.info("Поиск событий с параметрами: {}, с: {}, {}", searchParam, pageParam);

        Pageable pageable = PageRequest.of(pageParam.getFrom() / pageParam.getSize(), pageParam.getSize());

        Specification<Event> spec = EventSpecifications.withoutConditions();

        if (searchParam != null) {
            spec = spec.and(EventSpecifications.eventDateAfter(searchParam.getRangeStart()))
                    .and(EventSpecifications.eventDateBefore(searchParam.getRangeEnd()))
                    .and(EventSpecifications.initiatorIdIn(searchParam.getUsers()))
                    .and(EventSpecifications.categoryIdIn(searchParam.getCategories()))
                    .and(EventSpecifications.stateIn(searchParam.getStates()));

            Long placeId = searchParam.getPlaceId();
            PlaceDto place = placeId != null ? placeGateway.getOrThrow(placeId) : null;

            spec = spec.and(EventSpecifications.placeSearch(place, searchParam.getRadius()));
        }

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        log.info("Найдено {} событий, соответствующих критериям.", events.size());

        return eventDtoAssembler.toFullDtoList(events);
    }

    @Transactional
    @Override
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequestDto request) {
        log.info("Обновление события с id: {}, запрос: {}", eventId, request);

        Event event = findEventByOrThrow(eventId);

        Long newCategoryId = request.getCategory();
        if (newCategoryId != null) {
            categoryGateway.assertExists(newCategoryId);
        }

        EventMapper.updateEntity(event, request, newCategoryId);

        LocalDateTime now = LocalDateTime.now();

        String stateActionParam = request.getStateAction();
        if (stateActionParam != null) {
            EventStateAction eventStateAction = EventStateAction.valueOf(stateActionParam);

            if (eventStateAction == EventStateAction.PUBLISH_EVENT) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Событие может быть опубликовано только в состоянии ожидания публикации.");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(now);
            } else if (eventStateAction == EventStateAction.REJECT_EVENT) {
                if (event.getState().equals(EventState.PUBLISHED)) {
                    throw new ConflictException("Событие уже опубликовано и не может быть отклонено.");
                }
                event.setState(EventState.CANCELED);
            }
        }

        if (event.getState() == EventState.PUBLISHED) {
            LocalDateTime eventDate = event.getEventDate();
            LocalDateTime publishedOn = event.getPublishedOn();

            long hoursBeforeEvent = Duration.between(publishedOn, eventDate).toHours();
            if (hoursBeforeEvent < 1) {
                throw new ConflictException("Дата публикации должна быть не раньше чем за 1 час до события");
            }
        }

        Event updatedEvent = eventRepository.save(event);

        log.info("Событие с ID {} обновлено.", eventId);

        return eventDtoAssembler.toFullDto(updatedEvent);
    }

    @Transactional
    @Override
    public EventFullDto setPlace(long eventId, long placeId) {
        log.info("Привязка события с id: {} к месту: {}", eventId, placeId);

        Event event = findEventByOrThrow(eventId);

        placeGateway.assertExists(placeId); // просто проверяем существование

        event.setPlaceId(placeId);

        return eventDtoAssembler.toFullDto(eventRepository.save(event));
    }

    @Transactional
    @Override
    public void removePlace(long eventId) {
        log.info("Отвязка места от события с id: {}", eventId);
        Event event = findEventByOrThrow(eventId);

        event.setPlaceId(null);

        eventRepository.save(event);
    }

    private Event findEventByOrThrow(long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие с id " + eventId + " не найдено."));
    }
}