package ewm.main.event.controller;

import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventRequestStatusUpdateRequestDto;
import ewm.main.dto.EventRequestStatusUpdateResultDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.NewEventDto;
import ewm.main.dto.ParticipationRequestDto;
import ewm.main.dto.UpdateEventUserRequestDto;
import ewm.main.dto.search.PageParam;
import ewm.main.event.service.PrivateEventService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
@AllArgsConstructor
public class PrivateEventController {
    private final PrivateEventService privateEventService;

    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    EventFullDto createEvent(@PathVariable long userId, @Valid @RequestBody NewEventDto dto) {
        return privateEventService.createEvent(userId, dto);
    }

    @GetMapping("/{userId}/events")
    List<EventShortDto> getAllByUserId(@PathVariable long userId,
                                       @Valid @ModelAttribute PageParam pageParam) {

        return privateEventService.getAllByUserId(userId, pageParam);
    }

    @GetMapping("/{userId}/events/{eventId}")
    EventFullDto getEventById(@PathVariable long userId, @PathVariable long eventId) {
        return privateEventService.getEventOfUserById(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto updateEvent(@PathVariable long userId,
                             @PathVariable long eventId,
                             @Valid @RequestBody UpdateEventUserRequestDto dto) {
        return privateEventService.updateEventOfUser(userId, eventId, dto);
    }

    @GetMapping("/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getRequestsForEvent(@PathVariable long userId, @PathVariable long eventId) {
        return privateEventService.getRequestsForEvent(userId, eventId);
    }

    @PatchMapping("/{userId}/events/{eventId}/requests")
    EventRequestStatusUpdateResultDto setRequestsStatus(@PathVariable long userId,
                                                        @PathVariable long eventId,
                                                        @Valid @RequestBody EventRequestStatusUpdateRequestDto dto) {
        return privateEventService.setRequestsStatus(userId, eventId, dto);
    }

    @PutMapping("/{userId}/events/{eventId}/place/{placeId}")
    public EventFullDto setPlace(@PathVariable long userId,
                                 @PathVariable long eventId,
                                 @PathVariable long placeId) {
        return privateEventService.setPlace(userId, eventId, placeId);
    }

    @DeleteMapping("/{userId}/events/{eventId}/place")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlace(@PathVariable long userId,
                            @PathVariable long eventId) {
        privateEventService.removePlace(userId, eventId);
    }

}
