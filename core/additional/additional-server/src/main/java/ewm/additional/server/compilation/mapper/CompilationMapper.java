package ewm.additional.server.compilation.mapper;

import ewm.additional.server.compilation.model.Compilation;
import ewm.additional.server.dto.CompilationDto;
import ewm.additional.server.dto.NewCompilationDto;
import ewm.event.dto.EventSummaryDto;

import java.util.List;
import java.util.Set;

public class CompilationMapper {
    public static CompilationDto toDto(Compilation entity, List<EventSummaryDto> events) {
        return CompilationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .pinned(entity.getPinned())
                .events(events)
                .build();
    }

    public static Compilation toEntity(NewCompilationDto dto, Set<Long> eventIds) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned())
                .eventIds(eventIds)
                .build();
    }
}