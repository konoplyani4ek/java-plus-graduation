package ewm.main.event.model;

import ewm.main.exception.ValidationException;

public enum EventState {
    PENDING,
    PUBLISHED,
    CANCELED;

    public static EventState parse(String state) {
        try {
            return EventState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid EventStatus: " + state);
        }
    }
}
