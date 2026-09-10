package ewm.main.event.mapper;

import ewm.main.category.Category;
import ewm.main.category.mapper.CategoryMapper;
import ewm.main.dto.EventFullDto;
import ewm.main.dto.EventShortDto;
import ewm.main.dto.NewEventDto;
import ewm.main.dto.UpdateEventAdminRequestDto;
import ewm.main.dto.UpdateEventUserRequestDto;
import ewm.main.event.model.Event;
import ewm.main.place.mapper.PlaceMapper;
import ewm.main.user.User;
import ewm.main.user.UserMapper;

public class EventMapper {

    private EventMapper() {
    }

    public static Event toEntity(NewEventDto dto, Category category, User initiator) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());
        event.setCategory(category);
        event.setEventDate(dto.getEventDate());
        event.setLocation(LocationMapper.toLocation(dto.getLocation()));
        event.setPaid(dto.getPaid());
        event.setParticipantLimit(dto.getParticipantLimit());
        event.setRequestModeration(dto.getRequestModeration());
        event.setInitiator(initiator);

        return event;
    }

    public static EventFullDto toFullDto(Event event) {
        EventFullDto dto = new EventFullDto();
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(CategoryMapper.toDto(event.getCategory()));
        dto.setCreatedOn(event.getCreatedOn());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setId(event.getId());
        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator())); // обязательное поле
        dto.setLocation(LocationMapper.toLocationDto(event.getLocation()));
        dto.setPaid(event.isPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setRequestModeration(event.isRequestModeration());
        dto.setState(event.getState().name()); // обязательное поле
        dto.setTitle(event.getTitle());
        dto.setPlace(PlaceMapper.toShortDto(event.getPlace()));

        return dto;
    }

    public static EventShortDto toShortDto(Event event) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(CategoryMapper.toDto(event.getCategory()));
        dto.setEventDate(event.getEventDate());
        dto.setInitiator(UserMapper.toUserShortDto(event.getInitiator())); // обязательное поле
        dto.setPaid(event.isPaid());

        return dto;
    }

    public static void updateEntity(Event event, UpdateEventUserRequestDto dto, Category category) {
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (category != null) {
            event.setCategory(category);
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null)
            event.setLocation(LocationMapper.toLocation(dto.getLocation()));
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
    }

    public static void updateEntity(Event event, UpdateEventAdminRequestDto dto, Category category) {
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (category != null) {
            event.setCategory(category);
        }
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null)
            event.setLocation(LocationMapper.toLocation(dto.getLocation()));
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
    }

}
