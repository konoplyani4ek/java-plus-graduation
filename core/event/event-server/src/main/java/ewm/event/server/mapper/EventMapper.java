package ewm.event.server.mapper;

import ewm.category.dto.CategoryDto;
import ewm.event.server.dto.EventFullDto;
import ewm.event.server.dto.EventShortDto;
import ewm.event.server.dto.NewEventDto;
import ewm.event.server.dto.UpdateEventAdminRequestDto;
import ewm.event.server.dto.UpdateEventUserRequestDto;
import ewm.event.server.model.Event;
import ewm.place.dto.PlaceDto;
import ewm.user.dto.UserShortDto;

public class EventMapper {

    private EventMapper() {
    }

    public static Event toEntity(NewEventDto dto, long initiatorId) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setCategoryId(dto.getCategory());
        event.setEventDate(dto.getEventDate());
        event.setLocation(LocationMapper.toLocation(dto.getLocation()));
        event.setPaid(dto.getPaid());
        event.setParticipantLimit(dto.getParticipantLimit());
        event.setRequestModeration(dto.getRequestModeration());
        event.setInitiatorId(initiatorId);

        return event;
    }

    /**
     * category/initiator/place приходят снаружи (из чужих сервисов через Feign),
     * а не читаются из JPA-связей — их здесь больше нет.
     */
    public static EventFullDto toFullDto(Event event, CategoryDto category, UserShortDto initiator, PlaceDto place) {
        EventFullDto dto = new EventFullDto();
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(category);
        dto.setCreatedOn(event.getCreatedOn());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setId(event.getId());
        dto.setInitiator(initiator);
        dto.setLocation(LocationMapper.toLocationDto(event.getLocation()));
        dto.setPaid(event.isPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setRequestModeration(event.isRequestModeration());
        dto.setState(event.getState().name());
        dto.setTitle(event.getTitle());
        dto.setPlace(place);

        return dto;
    }

    public static EventShortDto toShortDto(Event event, CategoryDto category, UserShortDto initiator) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(category);
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(initiator);
        dto.setPaid(event.isPaid());

        return dto;
    }

    public static void updateEntity(Event event, UpdateEventUserRequestDto dto, Long newCategoryId) {
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (newCategoryId != null) {
            event.setCategoryId(newCategoryId);
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null)
            event.setLocation(LocationMapper.toLocation(dto.getLocation()));
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
    }

    public static void updateEntity(Event event, UpdateEventAdminRequestDto dto, Long newCategoryId) {
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (newCategoryId != null) {
            event.setCategoryId(newCategoryId);
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null)
            event.setLocation(LocationMapper.toLocation(dto.getLocation()));
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
    }

}