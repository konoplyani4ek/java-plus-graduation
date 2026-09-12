package ewm.event.server.service;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.UpdateEventAdminRequestDto;
import ewm.event.server.dto.search.AdminEventSearchParam;
import ewm.event.server.dto.search.PageParam;

import java.util.List;

public interface AdminEventService {
    List<EventFullDto> searchEvents(AdminEventSearchParam searchParam, PageParam pageParam);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequestDto request);

    EventFullDto setPlace(long eventId, long placeId);

    void removePlace(long eventId);
}