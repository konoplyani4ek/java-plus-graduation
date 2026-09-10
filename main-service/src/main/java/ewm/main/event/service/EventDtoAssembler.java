package ewm.main.event.service;

import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.event.mapper.EventMapper;
import ewm.main.event.model.Event;
import ewm.main.request.model.RequestStatus;
import ewm.main.request.repository.EventConfirmedRequestsCount;
import ewm.main.request.repository.ParticipationRequestRepository;
import ewm.main.stat.StatService;
import ewm.stat.client.model.GetStatsParams;
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
    private final ParticipationRequestRepository participationRequestRepository;

    public EventDtoAssembler(StatService statService,
                             ParticipationRequestRepository participationRequestRepository) {
        this.statService = statService;
        this.participationRequestRepository = participationRequestRepository;
    }

    public EventShortDto toShortDto(Event event) {
        EventShortDto dto = EventMapper.toShortDto(event);

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(getConfirmedRequests(event));

        return dto;
    }

    public EventFullDto toFullDto(Event event) {
        EventFullDto dto = EventMapper.toFullDto(event);

        dto.setViews(getViews(event));
        dto.setConfirmedRequests(getConfirmedRequests(event));

        return dto;
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);

        List<EventShortDto> result = new ArrayList<>();

        for (Event event : events) {
            EventShortDto dto = EventMapper.toShortDto(event);
            dto.setViews(getViewsForEvent(event, viewsByEventId));
            dto.setConfirmedRequests(getConfirmedRequestsForEvent(event, confirmedRequestsByEventId));
            result.add(dto);
        }

        return result;
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        Map<Long, Long> viewsByEventId = getViewsByEventId(events);
        Map<Long, Long> confirmedRequestsByEventId = getConfirmedRequestsByEventId(events);

        List<EventFullDto> result = new ArrayList<>();

        for (Event event : events) {
            EventFullDto dto = EventMapper.toFullDto(event);
            dto.setViews(getViewsForEvent(event, viewsByEventId));
            dto.setConfirmedRequests(getConfirmedRequestsForEvent(event, confirmedRequestsByEventId));
            result.add(dto);
        }

        return result;
    }

    private Long getConfirmedRequests(Event event) {
        return participationRequestRepository.countByEventIdAndStatus(
                event.getId(),
                RequestStatus.CONFIRMED
        );
    }

    private Map<Long, Long> getConfirmedRequestsByEventId(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = getEventIds(events);

        List<EventConfirmedRequestsCount> counts =
                participationRequestRepository.countConfirmedRequestsByEventIds(
                        eventIds,
                        RequestStatus.CONFIRMED
                );

        Map<Long, Long> result = new HashMap<>();

        for (EventConfirmedRequestsCount count : counts) {
            result.put(count.getEventId(), count.getConfirmedRequests());
        }

        return result;
    }

    private Long getConfirmedRequestsForEvent(Event event, Map<Long, Long> confirmedRequestsByEventId) {
        return confirmedRequestsByEventId.getOrDefault(event.getId(), 0L);
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