package ewm.main.request.service;

import ewm.main.dto.ParticipationRequestDto;

import java.util.List;

public interface ParticipationRequestService {

    List<ParticipationRequestDto> getRequests(long userId);

    ParticipationRequestDto addRequest(long userId, long eventId);

    ParticipationRequestDto cancelRequest(long userId, long requestId);
}