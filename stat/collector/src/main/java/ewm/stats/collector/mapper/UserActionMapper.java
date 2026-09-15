package ewm.stats.collector.mapper;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ewm.stats.proto.ActionTypeProto;
import ewm.stats.proto.UserActionProto;

import java.time.Instant;

public final class UserActionMapper {

    private UserActionMapper() {
    }

    public static UserActionAvro toAvro(UserActionProto proto) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(toAvro(proto.getActionType()))
                .setTimestamp(toInstant(proto))
                .build();
    }

    private static Instant toInstant(UserActionProto proto) {
        return Instant.ofEpochSecond(
                proto.getTimestamp().getSeconds(),
                proto.getTimestamp().getNanos());
    }

    private static ActionTypeAvro toAvro(ActionTypeProto type) {
        return switch (type) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException("Неизвестный тип действия: " + type);
        };
    }
}