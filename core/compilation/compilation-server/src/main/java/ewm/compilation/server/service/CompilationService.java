package ewm.compilation.server.service;

import ewm.compilation.server.mapper.CompilationMapper;
import ewm.compilation.server.model.Compilation;
import ewm.compilation.server.repository.CompilationRepository;
import ewm.compilation.server.dto.CompilationDto;
import ewm.compilation.server.dto.NewCompilationDto;
import ewm.compilation.server.dto.UpdateCompilationRequestDto;
import ewm.compilation.server.exception.ConflictException;
import ewm.compilation.server.exception.NotFoundException;
import ewm.compilation.server.gateway.EventGateway;
import ewm.event.dto.EventSummaryDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventGateway eventGateway;

    @Transactional
    public CompilationDto add(NewCompilationDto dto) {
        if (isTitleTaken(dto.getTitle())) {
            throw new ConflictException("Compilation title already taken");
        }

        Set<Long> eventIds = Objects.requireNonNullElse(dto.getEvents(), Set.of());

        Compilation newCompilation = compilationRepository.save(CompilationMapper.toEntity(dto, eventIds));
        return CompilationMapper.toDto(newCompilation, getSummaries(eventIds));
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

        if (!Objects.isNull(dto.getEvents())) {
            existing.setEventIds(dto.getEvents());
        }

        return CompilationMapper.toDto(existing, getSummaries(existing.getEventIds()));
    }

    @Transactional
    public List<CompilationDto> get(Integer from, Integer size, Boolean pinned) {
        List<Compilation> list;

        if (Objects.isNull(pinned)) {
            list = compilationRepository.findAllWithLimitOffset(from, size);
        } else {
            list = compilationRepository.findAllWithLimitOffset(from, size, pinned);
        }

        List<CompilationDto> result = new ArrayList<>();

        for (Compilation compilation : list) {
            result.add(CompilationMapper.toDto(compilation, getSummaries(compilation.getEventIds())));
        }

        return result;
    }

    @Transactional
    public CompilationDto get(Long id) {
        Compilation compilation = compilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compilation not found"));

        return CompilationMapper.toDto(compilation, getSummaries(compilation.getEventIds()));
    }

    private List<EventSummaryDto> getSummaries(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return List.of();
        }
        return eventGateway.getSummaries(List.copyOf(eventIds));
    }

    private Boolean isTitleTaken(String title) {
        return compilationRepository.countByTitle(title) > 0;
    }

    private boolean isTitleTaken(String title, Long id) {
        return compilationRepository.countByTitleExcludingId(id, title) > 0;
    }
}