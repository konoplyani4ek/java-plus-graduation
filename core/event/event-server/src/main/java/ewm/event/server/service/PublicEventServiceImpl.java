package ewm.event.server.service;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.dto.search.PageParam;
import ewm.event.server.dto.search.PublicEventSearchParam;
import ewm.event.server.exception.NotFoundException;
import ewm.event.server.exception.ValidationException;
import ewm.event.server.model.Event;
import ewm.event.server.model.EventSort;
import ewm.event.server.model.EventState;
import ewm.event.server.repository.EventRepository;
import ewm.event.server.repository.EventSpecifications;
import ewm.place.dto.PlaceDto;
import ewm.stat.client.grpc.AnalyzerClient;
import ewm.stat.client.grpc.CollectorClient;
import ewm.stat.client.model.ActionType;
import ewm.stats.proto.RecommendedEventProto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class PublicEventServiceImpl implements PublicEventService {
    private final EventRepository eventRepository;
    private final EventDtoAssembler eventDtoAssembler;
    private final PlaceGateway placeGateway;
    private final AnalyzerClient analyzerClient;
    private final CollectorClient collectorClient;
    private final RequestGateway requestGateway;

    @Override
    public List<EventShortDto> getEvents(PublicEventSearchParam searchParam, PageParam pageParam) {
        log.info("Поиск событий с параметрами: {}, {}", searchParam, pageParam);

        LocalDateTime rangeStart = searchParam.getRangeStart();
        LocalDateTime rangeEnd = searchParam.getRangeEnd();

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new ValidationException("rangeStart должен быть раньше rangeEnd");
        }

        Specification<Event> specification = Specification
                .where(EventSpecifications.stateEqual(EventState.PUBLISHED))
                .and(EventSpecifications.searchByTextInAnnotationAndDescription(searchParam.getText()));

        if (rangeStart == null && rangeEnd == null) {
            specification = specification.and(EventSpecifications.eventDateAfter(LocalDateTime.now()));
        } else {
            specification = specification
                    .and(EventSpecifications.eventDateAfter(rangeStart))
                    .and(EventSpecifications.eventDateBefore(rangeEnd));
        }

        specification = specification
                .and(EventSpecifications.paid(searchParam.getPaid()))
                .and(EventSpecifications.categoryIdIn(searchParam.getCategories()));

        Long placeId = searchParam.getPlaceId();
        PlaceDto place = placeId != null ? placeGateway.getOrThrow(placeId) : null;

        specification = specification.and(EventSpecifications.placeSearch(place, searchParam.getRadius()));

        EventSort eventSort = EventSort.parse(searchParam.getSort());

        if (eventSort == EventSort.RATING) {
            return getEventsSortedByRating(specification, pageParam);
        }

        return getEventsSortedByEventDate(specification, pageParam);
    }

    private List<EventShortDto> getEventsSortedByEventDate(Specification<Event> specification,
                                                           PageParam pageParam) {
        Pageable pageable = PageRequest.of(
                pageParam.getFrom() / pageParam.getSize(),
                pageParam.getSize(),
                Sort.by(Sort.Direction.ASC, "eventDate")
        );

        List<Event> events = eventRepository.findAll(specification, pageable).getContent();

        log.info("Найдено {} событий, соответствующих критериям.", events.size());

        return eventDtoAssembler.toShortDtoList(events);
    }

    private List<EventShortDto> getEventsSortedByRating(Specification<Event> specification,
                                                        PageParam pageParam) {
        List<Event> events = eventRepository.findAll(specification);
        log.info("Найдено {} событий для сортировки по рейтингу.", events.size());

        List<EventShortDto> dtos = eventDtoAssembler.toShortDtoList(events);

        return dtos.stream()
                .sorted(ratingComparator())
                .skip(pageParam.getFrom())
                .limit(pageParam.getSize())
                .toList();
    }

    private Comparator<EventShortDto> ratingComparator() {
        return Comparator
                .comparing(
                        EventShortDto::getRating,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
                .thenComparing(EventShortDto::getId);
    }

    @Override
    public EventFullDto getEventById(Long id) {

        Event event = eventRepository.findOneByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие с id: " + id + " не найдено или недоступно"));

        return eventDtoAssembler.toFullDto(event);
    }

    @Override
    public List<EventShortDto> getRecommendations(long userId, int maxResults) {
        List<RecommendedEventProto> recommendations = analyzerClient
                .getRecommendationsForUser(userId, maxResults)
                .toList();

        if (recommendations.isEmpty()) {
            return List.of();
        }

        List<Long> orderedIds = recommendations.stream()
                .map(RecommendedEventProto::getEventId)
                .toList();

        Map<Long, Event> publishedEventsById = eventRepository.findAllById(orderedIds).stream()
                .filter(event -> event.getState() == EventState.PUBLISHED)
                .collect(Collectors.toMap(Event::getId, event -> event));

        List<Event> orderedEvents = orderedIds.stream()
                .map(publishedEventsById::get)
                .filter(Objects::nonNull)
                .toList();

        return eventDtoAssembler.toShortDtoList(orderedEvents);
    }

    @Override
    public void likeEvent(long userId, long eventId) {
        Event event = eventRepository.findOneByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        "Событие с id: " + eventId + " не найдено или недоступно"));

        boolean attended = requestGateway.getRequestsForEvent(event.getId()).stream()
                .anyMatch(request -> request.getRequester() != null
                        && request.getRequester() == userId
                        && "CONFIRMED".equals(request.getStatus()));

        if (!attended) {
            throw new ValidationException(
                    "Пользователь " + userId + " не может лайкать мероприятие " + eventId
                            + " - нет подтверждённой заявки на участие в нём");
        }

        collectorClient.collectUserAction(userId, eventId, ActionType.LIKE, Instant.now());
    }
}