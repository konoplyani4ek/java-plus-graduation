package ewm.request.server.repository;

public interface EventConfirmedRequestsCount {
    Long getEventId();

    Long getConfirmedRequests();
}