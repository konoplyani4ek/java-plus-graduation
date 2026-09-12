package ewm.request.client;

import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.request.dto.EventRequestStatusUpdateResultDto;
import ewm.request.dto.InternalUpdateRequestStatusDto;
import ewm.request.dto.ParticipationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestClient {

    @GetMapping("/event/{eventId}")
    List<ParticipationRequestDto> getRequestsForEvent(@PathVariable("eventId") long eventId);

    @GetMapping("/event/{eventId}/confirmed-count")
    Long getConfirmedCount(@PathVariable("eventId") long eventId);

    @GetMapping("/confirmed-counts")
    List<EventConfirmedRequestsCountDto> getConfirmedCounts(@RequestParam("eventIds") List<Long> eventIds);

    @PatchMapping("/event/{eventId}/status")
    EventRequestStatusUpdateResultDto updateStatus(@PathVariable("eventId") long eventId,
                                                     @RequestBody InternalUpdateRequestStatusDto dto);
}