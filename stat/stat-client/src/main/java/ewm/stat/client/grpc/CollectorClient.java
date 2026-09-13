package ewm.stat.client.grpc;

import ewm.stat.client.model.ActionType;

import java.time.Instant;

public interface CollectorClient {

    void collectUserAction(long userId, long eventId, ActionType actionType, Instant timestamp);
}