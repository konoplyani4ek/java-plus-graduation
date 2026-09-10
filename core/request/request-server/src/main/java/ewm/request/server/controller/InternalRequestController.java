package ewm.request.server.controller;

import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.request.dto.EventRequestStatusUpdateResultDto;
import ewm.request.dto.InternalUpdateRequestStatusDto;
import ewm.request.dto.ParticipationRequestDto;
import ewm.request.server.service.ParticipationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class InternalRequestController {

    private final ParticipationRequestService requestService;

    @GetMapping("/event/{eventId}")
    public List<ParticipationRequestDto> getRequestsForEvent(@PathVariable long eventId) {
        return requestService.getRequestsForEvent(eventId);
    }

    @GetMapping("/event/{eventId}/confirmed-count")
    public Long getConfirmedCount(@PathVariable long eventId) {
        return requestService.getConfirmedCount(eventId);
    }

    @GetMapping("/confirmed-counts")
    public List<EventConfirmedRequestsCountDto> getConfirmedCounts(@RequestParam List<Long> eventIds) {
        return requestService.getConfirmedCounts(eventIds);
    }

    @PatchMapping("/event/{eventId}/status")
    public EventRequestStatusUpdateResultDto updateStatus(@PathVariable long eventId,
                                                            @RequestBody InternalUpdateRequestStatusDto dto) {
        return requestService.updateStatus(eventId, dto);
    }
}