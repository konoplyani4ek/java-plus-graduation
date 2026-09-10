package ewm.event.server.service;

import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.dto.NewEventDto;
import ewm.event.server.dto.UpdateEventUserRequestDto;
import ewm.event.server.dto.search.PageParam;
import ewm.request.dto.EventRequestStatusUpdateRequestDto;
import ewm.request.dto.EventRequestStatusUpdateResultDto;
import ewm.request.dto.ParticipationRequestDto;

import java.util.List;

public interface PrivateEventService {

    EventFullDto createEvent(long userId, NewEventDto dto);

    List<EventShortDto> getAllByUserId(long userId, PageParam pageParam);

    EventFullDto getEventOfUserById(long userId, long eventId);

    EventFullDto updateEventOfUser(long userId, long eventId, UpdateEventUserRequestDto dto);

    List<ParticipationRequestDto> getRequestsForEvent(long userId, long eventId);

    EventRequestStatusUpdateResultDto setRequestsStatus(long userId, long eventId, EventRequestStatusUpdateRequestDto dto);

    EventFullDto setPlace(long userId, long eventId, long placeId);

    void removePlace(long userId, long eventId);
}