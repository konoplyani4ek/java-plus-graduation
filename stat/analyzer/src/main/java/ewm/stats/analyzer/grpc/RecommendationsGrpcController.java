package ewm.stats.analyzer.grpc;

import ewm.stats.analyzer.service.RecommendedEvent;
import ewm.stats.analyzer.service.RecommendationService;
import ewm.stats.proto.InteractionsCountRequestProto;
import ewm.stats.proto.RecommendationsControllerGrpc;
import ewm.stats.proto.RecommendedEventProto;
import ewm.stats.proto.SimilarEventsRequestProto;
import ewm.stats.proto.UserPredictionsRequestProto;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsGrpcController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                           StreamObserver<RecommendedEventProto> responseObserver) {
        handle(responseObserver, () -> recommendationService.getRecommendationsForUser(
                request.getUserId(), request.getMaxResults()));
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                  StreamObserver<RecommendedEventProto> responseObserver) {
        handle(responseObserver, () -> recommendationService.getSimilarEvents(
                request.getEventId(), request.getUserId(), request.getMaxResults()));
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                      StreamObserver<RecommendedEventProto> responseObserver) {
        handle(responseObserver, () -> recommendationService.getInteractionsCount(request.getEventIdList()));
    }

    private void handle(StreamObserver<RecommendedEventProto> responseObserver,
                         java.util.function.Supplier<List<RecommendedEvent>> action) {
        try {
            for (RecommendedEvent event : action.get()) {
                responseObserver.onNext(toProto(event));
            }
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при формировании рекомендаций: {}", e.getMessage(), e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Не удалось сформировать рекомендации")
                            .withCause(e)
                            .asRuntimeException());
        }
    }

    private RecommendedEventProto toProto(RecommendedEvent event) {
        return RecommendedEventProto.newBuilder()
                .setEventId(event.eventId())
                .setScore(event.score())
                .build();
    }
}