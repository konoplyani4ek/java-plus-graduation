package ewm.main.event.service;

import ewm.main.category.Category;
import ewm.main.category.repository.CategoryRepository;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.UpdateEventAdminRequestDto;
import ewm.main.event.mapper.EventMapper;
import ewm.main.dto.search.AdminEventSearchParam;
import ewm.main.event.model.Event;
import ewm.main.event.model.EventStateAction;
import ewm.main.event.model.EventState;
import ewm.main.dto.search.PageParam;
import ewm.main.event.repository.EventRepository;
import ewm.main.event.repository.EventSpecifications;
import ewm.main.exception.ConflictException;
import ewm.main.exception.NotFoundException;
import ewm.main.place.Place;
import ewm.main.place.repository.PlaceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class AdminEventServiceImpl implements AdminEventService {
    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final EventDtoAssembler eventDtoAssembler;
    private final PlaceRepository placeRepository;

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
            Place place = null;

            if (placeId != null) {
                place = placeRepository.findById(placeId)
                        .orElseThrow(() -> new NotFoundException("Не найдено место с id: " + placeId));
            }

            spec = spec.and(EventSpecifications.placeSearch(place, searchParam.getRadius()));
        }

        List<Event> events = eventRepository.findAll(spec, pageable).getContent();

        log.info("Найдено {} событий, соответствующих критериям.", events.size());

        return eventDtoAssembler.toFullDtoList(events);
    }

    @Override
    public EventFullDto updateEvent(Long eventId, UpdateEventAdminRequestDto request) {
        log.info("Обновление события с id: {}, запрос: {}", eventId, request);

        Event event = findEventByOrThrow(eventId);

        Long newCategoryId = request.getCategory();
        Category newCategory = null;
        if (newCategoryId != null) {
            newCategory = categoryRepository.findById(newCategoryId)
                    .orElseThrow(() -> new NotFoundException("Не найдена категория с id: " + newCategoryId));
        }

        EventMapper.updateEntity(event, request, newCategory);

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

    @Override
    public EventFullDto setPlace(long eventId, long placeId) {
        log.info("Привязка события с id: {} к месту: {}", eventId, placeId);

        Event event = findEventByOrThrow(eventId);

        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("Не найдено место: " + placeId));

        event.setPlace(place);

        return eventDtoAssembler.toFullDto(eventRepository.save(event));
    }

    @Override
    public void removePlace(long eventId) {
        log.info("Отвязка места от события с id: {}", eventId);
        Event event = findEventByOrThrow(eventId);

        event.setPlace(null);

        eventRepository.save(event);
    }

    private Event findEventByOrThrow(long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие с id " + eventId + " не найдено."));
    }

}
