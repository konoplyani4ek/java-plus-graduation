package ewm.event.server.controller;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.dto.search.PageParam;
import ewm.event.server.dto.search.PublicEventSearchParam;
import ewm.event.server.service.PublicEventService;
import ewm.event.server.stat.StatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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