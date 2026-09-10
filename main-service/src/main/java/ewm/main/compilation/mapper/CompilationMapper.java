package ewm.main.compilation.mapper;

import ewm.main.compilation.model.Compilation;
import ewm.main.dto.CompilationDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.NewCompilationDto;
import ewm.main.event.model.Event;

import java.util.List;

public class CompilationMapper {
    public static CompilationDto toDto(Compilation entity, List<EventShortDto> eventsDto) {
        return CompilationDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .pinned(entity.getPinned())
                .events(eventsDto)
                .build();
    }

    public static Compilation toEntity(NewCompilationDto dto, List<Event> eventList) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.getPinned())
                .events(eventList)
                .build();
    }
}
