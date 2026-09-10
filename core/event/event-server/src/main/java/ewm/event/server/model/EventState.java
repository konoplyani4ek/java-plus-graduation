package ewm.event.server.model;

import ewm.event.server.exception.ValidationException;

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