package ewm.main.event.controller;

import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.search.PageParam;
import ewm.main.dto.search.PublicEventSearchParam;
import ewm.main.event.service.PublicEventService;
import ewm.main.stat.StatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@Slf4j
@AllArgsConstructor
public class PublicEventController {
    private final PublicEventService publicEventService;
    private final StatService statService;

    @GetMapping
    public List<EventShortDto> getEvents(@Valid @ModelAttribute PublicEventSearchParam searchParam,
                                         @Valid @ModelAttribute PageParam pageParam,
                                         HttpServletRequest request) {

        statService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return publicEventService.getEvents(searchParam, pageParam);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable Long id, HttpServletRequest request) {

        statService.saveHit(request.getRequestURI(), request.getRemoteAddr());

        return publicEventService.getEventById(id);
    }
}
