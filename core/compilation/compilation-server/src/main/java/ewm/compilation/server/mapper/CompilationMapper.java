package ewm.compilation.server.mapper;

import ewm.compilation.server.model.Compilation;
import ewm.compilation.server.dto.CompilationDto;
import ewm.compilation.server.dto.NewCompilationDto;
import ewm.event.dto.EventSummaryDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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