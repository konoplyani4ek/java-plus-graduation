package ewm.stats.collector.grpc;

import com.google.protobuf.Empty;
import ewm.stats.collector.service.UserActionProducerService;
import ewm.stats.proto.UserActionControllerGrpc;
import ewm.stats.proto.UserActionProto;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UserActionGrpcController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionProducerService userActionProducerService;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            userActionProducerService.collect(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка обработки действия пользователя: {}", e.getMessage(), e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Не удалось обработать действие пользователя")
                            .withCause(e)
                            .asRuntimeException());
        }
    }
}