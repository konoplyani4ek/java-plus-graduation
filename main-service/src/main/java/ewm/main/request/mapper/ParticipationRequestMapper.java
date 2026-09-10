package ewm.main.request.mapper;

import ewm.main.dto.ParticipationRequestDto;
import ewm.main.request.model.ParticipationRequest;

public class ParticipationRequestMapper {

    private ParticipationRequestMapper() {
    }

    public static ParticipationRequestDto toDto(ParticipationRequest request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus().name())
                .created(request.getCreated())
                .build();
    }
}