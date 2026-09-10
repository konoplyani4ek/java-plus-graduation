package ewm.request.server.mapper;

import ewm.request.dto.ParticipationRequestDto;
import ewm.request.server.model.ParticipationRequest;

public class ParticipationRequestMapper {

    private ParticipationRequestMapper() {
    }

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus().name())
                .created(request.getCreated())
                .build();
    }
}