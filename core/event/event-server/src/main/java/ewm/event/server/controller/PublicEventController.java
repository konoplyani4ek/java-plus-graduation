package ewm.event.server.controller;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.dto.search.PageParam;
import ewm.event.server.dto.search.PublicEventSearchParam;
import ewm.event.server.service.PublicEventService;
import ewm.stat.client.grpc.CollectorClient;
import ewm.stat.client.model.ActionType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/events")
@Slf4j
@RequiredArgsConstructor
public class PublicEventController {

    private static final String USER_ID_HEADER = "X-EWM-USER-ID";

    private final PublicEventService publicEventService;
    private final CollectorClient collectorClient;

    @Value("${event.recommendations.default-size:10}")
    private int defaultRecommendationsSize;

    @GetMapping
    public List<EventShortDto> getEvents(@Valid @ModelAttribute PublicEventSearchParam searchParam,
                                         @Valid @ModelAttribute PageParam pageParam) {
        return publicEventService.getEvents(searchParam, pageParam);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(
            @RequestHeader(USER_ID_HEADER) long userId,
            @RequestParam(required = false) Integer size) {

        int maxResults = size != null ? size : defaultRecommendationsSize;
        return publicEventService.getRecommendations(userId, maxResults);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable Long id,
                                     @RequestHeader(value = USER_ID_HEADER, required = false) Long userId) {

        EventFullDto dto = publicEventService.getEventById(id);

        if (userId != null) {
            collectorClient.collectUserAction(userId, id, ActionType.VIEW, Instant.now());
        }

        return dto;
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId, @RequestHeader(USER_ID_HEADER) long userId) {
        publicEventService.likeEvent(userId, eventId);
    }
}