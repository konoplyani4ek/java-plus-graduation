package ewm.event.server.controller;

import ewm.event.dto.EventInternalDto;
import ewm.event.dto.EventSummaryDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.exception.NotFoundException;
import ewm.event.server.model.Event;
import ewm.event.server.repository.EventRepository;
import ewm.event.server.service.EventDtoAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Внутренний (межсервисный) контроллер — НЕ проксируется через Gateway.
 * Потребители: request-service (EventClient), additional-service (проверка категории,
 * отображение событий в подборках).
 */
@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final EventRepository eventRepository;
    private final EventDtoAssembler eventDtoAssembler;

    @GetMapping("/{eventId}")
    public EventInternalDto getEvent(@PathVariable long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState().name())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .build();
    }

    @GetMapping("/exists-by-category/{categoryId}")
    public boolean existsByCategory(@PathVariable long categoryId) {
        return eventRepository.existsByCategoryId(categoryId);
    }

    @GetMapping("/summary")
    public List<EventSummaryDto> getSummaries(@RequestParam List<Long> ids) {
        List<Event> events = eventRepository.findAllById(ids);
        List<EventShortDto> shortDtos = eventDtoAssembler.toShortDtoList(events);

        return shortDtos.stream()
                .map(dto -> EventSummaryDto.builder()
                        .id(dto.getId())
                        .title(dto.getTitle())
                        .annotation(dto.getAnnotation())
                        .eventDate(dto.getEventDate())
                        .paid(dto.getPaid())
                        .categoryId(dto.getCategory() != null ? dto.getCategory().getId() : null)
                        .categoryName(dto.getCategory() != null ? dto.getCategory().getName() : null)
                        .views(dto.getViews())
                        .confirmedRequests(dto.getConfirmedRequests())
                        .build())
                .toList();
    }
}