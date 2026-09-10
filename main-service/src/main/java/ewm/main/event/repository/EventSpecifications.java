package ewm.main.event.repository;

import ewm.main.event.model.Event;
import ewm.main.event.model.EventState;
import ewm.main.exception.ValidationException;
import ewm.main.place.Place;
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

        return (root, query, cb) -> root.get("initiator").get("id").in(ids);
    }

    public static Specification<Event> categoryIdIn(List<Long> ids) {
        if (ids == null) {
            return null;
        }

        return (root, query, cb) ->
                root.get("category").get("id").in(ids);
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

    public static Specification<Event> placeEquals(Place place) {
        if (place == null) {
            return null;
        }

        return (root, query, cb) ->
                cb.equal(root.get("place"), place);
    }

    public static Specification<Event> inRadius(Place place, Double radius) {
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

    public static Specification<Event> placeSearch(Place place, Double radius) {
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