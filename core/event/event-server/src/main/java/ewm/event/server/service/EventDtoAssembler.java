package ewm.event.server.service;

import ewm.category.dto.CategoryDto;
import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.mapper.EventMapper;
import ewm.event.server.model.Event;
import ewm.event.server.stat.StatService;
import ewm.place.dto.PlaceDto;
import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.stat.client.model.GetStatsParams;
import ewm.user.dto.UserShortDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EventDtoAssembler {
    private static final boolean UNIQUE_VIEWS = true;

    private final StatService statService;
    private final RequestGateway requestGateway;
    private final UserGateway userGateway;
    private final CategoryGateway categoryGateway;
    private final PlaceGateway placeGateway;

    public EventDtoAssembler(StatService statService,
                             RequestGateway requestGateway,
                             UserGateway userGateway,
                             CategoryGateway categoryGateway,
                             PlaceGateway placeGateway) {
        this.statService = statService;
        this.requestGateway = requestGateway;
        this.userGateway = userGateway;
        this.categoryGateway = categoryGateway;
        this.placeGateway = placeGateway;
    }

    public EventShortDto toShortDto(Event event) {
        UserShortDto initiator = userGateway.getShort(event.getInitiatorId());
        CategoryDto category = categoryGateway.get(event.getCategoryId());

        EventShortDto dto = EventMapper.toShortDto(event, category, initiator);

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(requestGateway.getConfirmedCount(event.getId()));

        return dto;
    }

    public EventFullDto toFullDto(Event event) {
        UserShortDto initiator = userGateway.getShort(event.getInitiatorId());
        CategoryDto category = categoryGateway.get(event.getCategoryId());
        PlaceDto place = event.getPlaceId() != null ? placeGateway.get(event.getPlaceId()) : null;

        EventFullDto dto = EventMapper.toFullDto(event, category, initiator, place);

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(requestGateway.getConfirmedCount(event.getId()));

        return dto;
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
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
            dto.setViews(getViewsForEvent(event, viewsByEventId));
            dto.setConfirmedRequests(confirmedRequestsByEventId.getOrDefault(event.getId(), 0L));
            result.add(dto);
        }

        return result;
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
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
            dto.setViews(getViewsForEvent(event, viewsByEventId));
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

    /**
     * Аналогично — один батч-запрос к additional-service (категории) на весь список событий.
     */
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

    private Long getViews(Event event) {
        if (event.getPublishedOn() == null) {
            return null;
        }

        String uri = getEventUri(event);

        GetStatsParams params = GetStatsParams.builder()
                .start(event.getPublishedOn())
                .end(LocalDateTime.now())
                .uris(List.of(uri))
                .unique(UNIQUE_VIEWS)
                .build();

        Map<String, Long> viewsByUri = statService.getViews(params);

        if (viewsByUri == null) {
            return null;
        }

        return viewsByUri.getOrDefault(uri, 0L);
    }

    private Map<Long, Long> getViewsByEventId(List<Event> events) {
        List<Event> eventsWithPublishedOn = getEventsWithPublishedOn(events);

        if (eventsWithPublishedOn.isEmpty()) {
            return Map.of();
        }

        GetStatsParams params = GetStatsParams.builder()
                .start(getMinPublishedOn(eventsWithPublishedOn))
                .end(LocalDateTime.now())
                .uris(getEventUris(eventsWithPublishedOn))
                .unique(UNIQUE_VIEWS)
                .build();

        Map<String, Long> viewsByUri = statService.getViews(params);

        if (viewsByUri == null) {
            return null;
        }

        Map<Long, Long> viewsByEventId = new HashMap<>();

        for (Event event : eventsWithPublishedOn) {
            String uri = getEventUri(event);
            Long views = viewsByUri.getOrDefault(uri, 0L);

            viewsByEventId.put(event.getId(), views);
        }

        return viewsByEventId;
    }

    private Long getViewsForEvent(Event event, Map<Long, Long> viewsByEventId) {
        if (viewsByEventId == null) {
            return null;
        }

        return viewsByEventId.get(event.getId());
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

    private List<String> getEventUris(List<Event> events) {
        Set<String> uniqueUris = new LinkedHashSet<>();

        for (Event event : events) {
            uniqueUris.add(getEventUri(event));
        }

        return new ArrayList<>(uniqueUris);
    }

    private LocalDateTime getMinPublishedOn(List<Event> events) {
        LocalDateTime minPublishedOn = events.get(0).getPublishedOn();

        for (Event event : events) {
            if (event.getPublishedOn().isBefore(minPublishedOn)) {
                minPublishedOn = event.getPublishedOn();
            }
        }

        return minPublishedOn;
    }

    private String getEventUri(Event event) {
        return "/events/" + event.getId();
    }
}