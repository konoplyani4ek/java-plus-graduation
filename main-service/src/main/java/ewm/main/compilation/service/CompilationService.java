package ewm.main.compilation.service;

import ewm.main.compilation.mapper.CompilationMapper;
import ewm.main.compilation.model.Compilation;
import ewm.main.compilation.repository.CompilationRepository;
import ewm.main.dto.CompilationDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.NewCompilationDto;
import ewm.main.dto.UpdateCompilationRequestDto;
import ewm.main.event.model.Event;
import ewm.main.event.repository.EventRepository;
import ewm.main.event.service.EventDtoAssembler;
import ewm.main.exception.ConflictException;
import ewm.main.exception.NotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventDtoAssembler eventDtoAssembler;

    @Transactional
    public CompilationDto add(NewCompilationDto dto) {
        if (isTitleTaken(dto.getTitle())) {
            throw new ConflictException("Compilation title already taken");
        }
        List<Event> events = List.of();
        if (!Objects.isNull(dto.getEvents()) && !dto.getEvents().isEmpty()) {
            events = eventRepository.findAllById(dto.getEvents());
        }

        Compilation newCompilation = compilationRepository.save(CompilationMapper.toEntity(dto, events));
        return CompilationMapper.toDto(newCompilation, eventDtoAssembler.toShortDtoList(events));
    }

    @Transactional
    public void delete(Long id) {
        Compilation existing = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("no compilations found"));

        compilationRepository.delete(existing);
    }

    @Transactional
    public CompilationDto update(Long id, UpdateCompilationRequestDto dto) {
        Compilation existing = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("no compilations found"));

        if (isTitleTaken(dto.getTitle(), id)) {
            throw new ConflictException("Compilation title already taken");
        }

        if (!Objects.isNull(dto.getTitle())) {
            existing.setTitle(dto.getTitle());
        }

        if (!Objects.isNull(dto.getPinned())) {
            existing.setPinned(dto.getPinned());
        }

        List<Event> events = List.of();
        if (!Objects.isNull(dto.getEvents())) {
            events = eventRepository.findAllById(dto.getEvents());
            existing.setEvents(events);
        }
        return CompilationMapper.toDto(existing, eventDtoAssembler.toShortDtoList(events));
    }

    @Transactional
    public List<CompilationDto> get(Integer from, Integer size, Boolean pinned) {
        List<Compilation> list;

        if (Objects.isNull(pinned)) {
            list = compilationRepository.findAllWithLimitOffset(from, size);
        } else {
            list = compilationRepository.findAllWithLimitOffset(from, size, pinned);
        }

        if (list.isEmpty()) {
            return List.of();
        }

        // Получить выборку с присоединенными событиями
        List<Long> ids = list.stream()
                .map(Compilation::getId)
                .toList();

        List<Compilation> compilations =
                compilationRepository.findByIdInOrderByIdAsc(ids);

        // Получить список всех событий, чтобы одним запросом получить список сформированных dto событий
        Set<Long> eventIds = new HashSet<>();
        List<Event> allEvents = new ArrayList<>();

        for (Compilation compilation : compilations) {
            for (Event event : compilation.getEvents()) {
                if (eventIds.add(event.getId())) {
                    allEvents.add(event);
                }
            }
        }

        // Соответствие идентификатора события -> Dto события
        Map<Long, EventShortDto> eventDtoById = eventDtoAssembler.toShortDtoList(allEvents).stream()
                .collect(Collectors.toMap(
                        (d) -> d.getId(),
                        Function.identity()
                ));


        List<CompilationDto> result = new ArrayList<>();

        // По списку подборок формируем список dto, заполняем в каждую dto компиляции список dto событий
        for (Compilation compilation : compilations) {
            List<EventShortDto> eventsDto = new ArrayList<>();

            for (Event event : compilation.getEvents()) {
                eventsDto.add(eventDtoById.get(event.getId()));
            }

            CompilationDto dto = CompilationMapper.toDto(compilation, eventsDto);
            result.add(dto);
        }

        return result;
    }

    @Transactional
    public CompilationDto get(Long id) {
        Compilation compilation = compilationRepository.findById(id).orElseThrow(() -> new NotFoundException("Compilation not found"));
        return CompilationMapper.toDto(compilation, eventDtoAssembler.toShortDtoList(compilation.getEvents()));
    }

    private Boolean isTitleTaken(String title) {
        return compilationRepository.countByTitle(title) > 0;
    }

    private boolean isTitleTaken(String title, Long id) {
        return compilationRepository.countByTitleExcludingId(id, title) > 0;
    }
}
