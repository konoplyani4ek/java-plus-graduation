package ewm.event.server.repository;

import ewm.event.server.exception.ValidationException;
import ewm.event.server.model.Event;
import ewm.event.server.model.EventState;
import ewm.place.dto.PlaceDto;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> withoutConditions() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static Specification<Event> paid(Boolean paid) {
        if (paid == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> searchByTextInAnnotationAndDescription(String text) {
        if (text == null) {
            return null;
        }

        String pattern = "%" + text.toLowerCase().trim() + "%";

        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("annotation")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                );
    }

    public static Specification<Event> stateEqual(EventState state) {
        if (state == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Event> stateIn(List<EventState> states) {
        if (states == null) {
            return null;
        }

        return (root, query, cb) -> root.get("state").in(states);
    }

    public static Specification<Event> initiatorIdIn(List<Long> ids) {
        if (ids == null) {
            return null;
        }

        return (root, query, cb) -> root.get("initiatorId").in(ids);
    }

    public static Specification<Event> categoryIdIn(List<Long> ids) {
        if (ids == null) {
            return null;
        }

        return (root, query, cb) -> root.get("categoryId").in(ids);
    }

    public static Specification<Event> eventDateAfter(LocalDateTime start) {
        if (start == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("eventDate"), start);
    }

    public static Specification<Event> eventDateBefore(LocalDateTime end) {
        if (end == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("eventDate"), end);
    }

    /**
     * place теперь приходит из PlaceClient (Feign в main-service), а не из локальной
     * JPA-связи — сравниваем по id.
     */
    public static Specification<Event> placeEquals(PlaceDto place) {
        if (place == null) {
            return null;
        }

        return (root, query, cb) -> cb.equal(root.get("placeId"), place.getId());
    }

    public static Specification<Event> inRadius(PlaceDto place, Double radius) {
        if (place == null || radius == null) {
            return null;
        }

        return (root, query, cb) -> {
            Expression<Double> distance = cb.function(
                    "distance",
                    Double.class,
                    root.get("location").get("lat"),
                    root.get("location").get("lon"),
                    cb.literal(place.getLat()),
                    cb.literal(place.getLon())
            );

            return cb.lessThanOrEqualTo(distance, radius);
        };
    }

    public static Specification<Event> placeSearch(PlaceDto place, Double radius) {
        if (radius != null && place == null) {
            throw new ValidationException("Нельзя указывать радиус без указания места");
        }

        if (place == null) {
            return null;
        }

        if (radius != null) {
            return inRadius(place, radius);
        }

        return placeEquals(place);
    }

}