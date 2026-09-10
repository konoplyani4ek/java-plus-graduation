package ewm.request.server.service;

import ewm.request.dto.EventConfirmedRequestsCountDto;
import ewm.request.dto.EventRequestStatusUpdateResultDto;
import ewm.request.dto.InternalUpdateRequestStatusDto;
import ewm.request.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    // --- публичное API (через Gateway) ---
    List<ParticipationRequestDto> getRequests(long userId);

    ParticipationRequestDto addRequest(long userId, long eventId);

    ParticipationRequestDto cancelRequest(long userId, long requestId);

    // --- внутреннее API (только Feign, со стороны событий) ---
    List<ParticipationRequestDto> getRequestsForEvent(long eventId);

    Long getConfirmedCount(long eventId);

    List<EventConfirmedRequestsCountDto> getConfirmedCounts(List<Long> eventIds);

    EventRequestStatusUpdateResultDto updateStatus(long eventId, InternalUpdateRequestStatusDto dto);
}