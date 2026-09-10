package ewm.main.event.model;

import ewm.main.exception.ValidationException;

public enum EventStateAction {
    PUBLISH_EVENT,
    REJECT_EVENT;

    public static EventStateAction parse(String action) {
        try {
            return EventStateAction.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Неизвестный EventStateAction: " + action);
        }
    }

}
