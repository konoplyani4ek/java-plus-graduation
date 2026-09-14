package ewm.event.server.service;

import ewm.category.dto.CategoryDto;
import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.mapper.EventMapper;
import ewm.event.server.model.Event;
import ewm.place.dto.PlaceDto;
import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.stat.client.grpc.AnalyzerClient;
import ewm.stats.proto.RecommendedEventProto;
import ewm.user.dto.UserShortDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class EventDtoAssembler {

    private final AnalyzerClient analyzerClient;
    private final RequestGateway requestGateway;
    private final UserGateway userGateway;
    private final CategoryGateway categoryGateway;
    private final PlaceGateway placeGateway;

    public EventDtoAssembler(AnalyzerClient analyzerClient,
                             RequestGateway requestGateway,
                             UserGateway userGateway,
                             CategoryGateway categoryGateway,
                             PlaceGateway placeGateway) {
        this.analyzerClient = analyzerClient;
        this.requestGateway = requestGateway;
        this.userGateway = userGateway;
        this.categoryGateway = categoryGateway;
        this.placeGateway = placeGateway;
    }

    public EventShortDto toShortDto(Event event) {
        UserShortDto initiator = userGateway.getShort(event.getInitiatorId());
        CategoryDto category = categoryGateway.get(event.getCategoryId());

        EventShortDto dto = EventMapper.toShortDto(event, category, initiator);

        dto.setRating(getRating(event));
        dto.setConfirmedRequests(requestGateway.getConfirmedCount(event.getId()));

        return dto;
    }

    public EventFullDto toFullDto(Event event) {
        UserShortDto initiator = userGateway.getShort(event.getInitiatorId());
        CategoryDto category = categoryGateway.get(event.getCategoryId());
        PlaceDto place = event.getPlaceId() != null ? placeGateway.get(event.getPlaceId()) : null;

        EventFullDto dto = EventMapper.toFullDto(event, category, initiator, place);

        dto.setRating(getRating(event));
        dto.setConfirmedRequests(requestGateway.getConfirmedCount(event.getId()));

        return dto;
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        Map<Long, Double> ratingsByEventId = getRatingsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);
        Map<Long, UserShortDto> initiatorsByUserId = getInitiatorsByUserId(events);
        Map<Long, CategoryDto> categoriesByCategoryId = getCategoriesByCategoryId(events);

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = EventMapper.toShortDto(
                    event,
                    categoriesByCategoryId.get(event.getCategoryId()),
                    initiatorsByUserId.get(event.getInitiatorId())
            );
            dto.setRating(getRatingForEvent(event, ratingsByEventId));
            dto.setConfirmedRequests(confirmedRequestsByEventId.getOrDefault(event.getId(), 0L));
            result.add(dto);
        }

        return result;
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        Map<Long, Double> ratingsByEventId = getRatingsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);
        Map<Long, UserShortDto> initiatorsByUserId = getInitiatorsByUserId(events);
        Map<Long, CategoryDto> categoriesByCategoryId = getCategoriesByCategoryId(events);

        List<EventFullDto> result = new ArrayList<>();

        for (Event event : events) {
            PlaceDto place = event.getPlaceId() != null ? placeGateway.get(event.getPlaceId()) : null;

            EventFullDto dto = EventMapper.toFullDto(
                    event,
                    categoriesByCategoryId.get(event.getCategoryId()),
                    initiatorsByUserId.get(event.getInitiatorId()),
                    place
            );
            dto.setRating(getRatingForEvent(event, ratingsByEventId));
            dto.setConfirmedRequests(confirmedRequestsByEventId.getOrDefault(event.getId(), 0L));
            result.add(dto);
        }

        return result;
    }

    /**
     * Один запрос к user-service на весь список событий вместо N запросов (проблема N+1).
     */
    private Map<Long, UserShortDto> getInitiatorsByUserId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .distinct()
                .toList();

        return userGateway.getShortMap(initiatorIds);
    }

    private Map<Long, CategoryDto> getCategoriesByCategoryId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> categoryIds = events.stream()
                .map(Event::getCategoryId)
                .distinct()
                .toList();

        return categoryGateway.getMap(categoryIds);
    }

    private Map<Long, Long> getConfirmedRequestsByEventId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = getEventIds(events);

        List<EventConfirmedRequestsCountDto> counts = requestGateway.getConfirmedCounts(eventIds);

        Map<Long, Long> result = new HashMap<>();
        for (EventConfirmedRequestsCountDto count : counts) {
            result.put(count.getEventId(), count.getConfirmedRequests());
        }

        return result;
    }

    /**
     * Рейтинг мероприятия = сумма максимальных весов действий всех пользователей с ним
     * (метод GetInteractionsCount сервиса Analyzer). Для ещё не опубликованных мероприятий
     * взаимодействий быть не может — рейтинг не запрашиваем, возвращаем null.
     */
    private Double getRating(Event event) {
        if (event.getPublishedOn() == null) {
            return null;
        }

        try {
            return analyzerClient.getInteractionsCount(List.of(event.getId()))
                    .findFirst()
                    .map(RecommendedEventProto::getScore)
                    .orElse(0.0);
        } catch (Exception e) {
            log.warn("Analyzer недоступен при расчёте рейтинга события {}: {}", event.getId(), e.getMessage());
            return null;
        }
    }

    private Map<Long, Double> getRatingsByEventId(List<Event> events) {
        List<Event> publishedEvents = getEventsWithPublishedOn(events);

        if (publishedEvents.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = getEventIds(publishedEvents);

        try {
            Map<Long, Double> ratings = new HashMap<>();
            analyzerClient.getInteractionsCount(eventIds)
                    .forEach(r -> ratings.put(r.getEventId(), r.getScore()));
            return ratings;
        } catch (Exception e) {
            log.warn("Analyzer недоступен при батч-расчёте рейтинга событий {}: {}", eventIds, e.getMessage());
            return null;
        }
    }

    private Double getRatingForEvent(Event event, Map<Long, Double> ratingsByEventId) {
        if (ratingsByEventId == null) {
            return null;
        }
        if (event.getPublishedOn() == null) {
            return null;
        }

        return ratingsByEventId.getOrDefault(event.getId(), 0.0);
    }

    private List<Event> getEventsWithPublishedOn(List<Event> events) {
        List<Event> result = new ArrayList<>();

        for (Event event : events) {
            if (event.getPublishedOn() != null) {
                result.add(event);
            }
        }

        return result;
    }

    private List<Long> getEventIds(List<Event> events) {
        Set<Long> uniqueIds = new LinkedHashSet<>();

        for (Event event : events) {
            uniqueIds.add(event.getId());
        }

        return new ArrayList<>(uniqueIds);
    }
}