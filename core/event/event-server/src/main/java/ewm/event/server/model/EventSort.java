package ewm.event.server.model;

import ewm.event.server.exception.ValidationException;

public enum EventSort {
    EVENT_DATE,
    VIEWS;

    public static EventSort parse(String value) {
        try {
            return EventSort.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Неизвестный вариант EventSort: " + value);
        }
    }
}