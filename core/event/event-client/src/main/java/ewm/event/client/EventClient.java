package ewm.event.client;

import ewm.event.dto.EventInternalDto;
import ewm.event.dto.EventSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "event-service", path = "/internal/events")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventInternalDto getEvent(@PathVariable("eventId") long eventId);

    @GetMapping("/exists-by-category/{categoryId}")
    boolean existsByCategory(@PathVariable("categoryId") long categoryId);

    @GetMapping("/summary")
    List<EventSummaryDto> getSummaries(@RequestParam("ids") List<Long> ids);
}