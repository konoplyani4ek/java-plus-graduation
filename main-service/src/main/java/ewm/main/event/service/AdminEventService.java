package ewm.main.event.service;

import ewm.main.dto.UpdateEventAdminRequestDto;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.search.AdminEventSearchParam;
import ewm.main.dto.search.PageParam;

import java.util.List;

public interface AdminEventService {
    List<EventFullDto> searchEvents(AdminEventSearchParam searchParam, PageParam pageParam);

    EventFullDto updateEvent(Long eventId, UpdateEventAdminRequestDto request);

    EventFullDto setPlace(long eventId, long placeId);

    void removePlace(long eventId);
}
