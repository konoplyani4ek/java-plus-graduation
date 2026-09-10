package ewm.main.event.controller;

import ewm.main.dto.UpdateEventAdminRequestDto;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.search.AdminEventSearchParam;
import ewm.main.dto.search.PageParam;
import ewm.main.event.service.AdminEventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    @GetMapping
    public List<EventFullDto> searchEvents(
            @Valid @ModelAttribute AdminEventSearchParam searchParam,
            @Valid @ModelAttribute PageParam pageParam
    ) {
        return adminEventService.searchEvents(searchParam, pageParam);
    }

    @PatchMapping("/{eventId}")
    public EventFullDto updateEvent(
            @PathVariable
            @Positive
            Long eventId,
            @RequestBody
            @Valid
            UpdateEventAdminRequestDto request
    ) {
        return adminEventService.updateEvent(eventId, request);
    }

    @PutMapping("/{eventId}/place/{placeId}")
    public EventFullDto setPlace(@PathVariable long eventId,
                                 @PathVariable long placeId) {
        return adminEventService.setPlace(eventId, placeId);
    }

    @DeleteMapping("/{eventId}/place")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePlace(@PathVariable long eventId) {
        adminEventService.removePlace(eventId);
    }
}