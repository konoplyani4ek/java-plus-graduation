package ewm.event.server.service;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.dto.search.PageParam;
import ewm.event.server.dto.search.PublicEventSearchParam;

import java.util.List;

public interface PublicEventService {
    List<EventShortDto> getEvents(PublicEventSearchParam searchParam, PageParam pageParam);

    EventFullDto getEventById(Long id);
}