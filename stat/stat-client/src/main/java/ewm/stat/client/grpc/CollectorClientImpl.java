package ewm.stat.client.grpc;

import com.google.protobuf.Timestamp;
import ewm.stat.client.model.ActionType;
import ewm.stats.proto.ActionTypeProto;
import ewm.stats.proto.UserActionControllerGrpc;
import ewm.stats.proto.UserActionProto;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;

import java.time.Instant;

@Slf4j
public class CollectorClientImpl implements CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    @Override
    public void collectUserAction(long userId, long eventId, ActionType actionType, Instant timestamp) {
        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(toProto(actionType))
                .setTimestamp(toProtoTimestamp(timestamp))
                .build();
        try {
            collectorStub.collectUserAction(request);
        } catch (StatusRuntimeException e) {
            log.warn("Не удалось отправить действие в Collector (userId={}, eventId={}, type={}): {}",
                    userId, eventId, actionType, e.getMessage());
        }
    }

    private Timestamp toProtoTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private ActionTypeProto toProto(ActionType type) {
        return switch (type) {
            case VIEW -> ActionTypeProto.ACTION_VIEW;
            case REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case LIKE -> ActionTypeProto.ACTION_LIKE;
        };
    }
}