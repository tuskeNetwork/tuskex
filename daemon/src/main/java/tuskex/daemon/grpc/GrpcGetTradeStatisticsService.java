package tuskex.daemon.grpc;

import com.google.inject.Inject;
import tuskex.core.api.CoreApi;
import tuskex.core.trade.statistics.TradeStatistics3;
import tuskex.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import tuskex.daemon.grpc.interceptor.GrpcCallRateMeter;
import static tuskex.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import static tuskex.proto.grpc.GetTradeStatisticsGrpc.GetTradeStatisticsImplBase;
import static tuskex.proto.grpc.GetTradeStatisticsGrpc.getGetTradeStatisticsMethod;
import tuskex.proto.grpc.GetTradeStatisticsReply;
import tuskex.proto.grpc.GetTradeStatisticsRequest;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Optional;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GrpcGetTradeStatisticsService extends GetTradeStatisticsImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcGetTradeStatisticsService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void getTradeStatistics(GetTradeStatisticsRequest req,
                                   StreamObserver<GetTradeStatisticsReply> responseObserver) {
        try {
            var tradeStatistics = coreApi.getTradeStatistics().stream()
                    .map(TradeStatistics3::toProtoTradeStatistics3)
                    .collect(Collectors.toList());

            var reply = GetTradeStatisticsReply.newBuilder().addAllTradeStatistics(tradeStatistics).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    final ServerInterceptor[] interceptors() {
        Optional<ServerInterceptor> rateMeteringInterceptor = rateMeteringInterceptor();
        return rateMeteringInterceptor.map(serverInterceptor ->
                new ServerInterceptor[]{serverInterceptor}).orElseGet(() -> new ServerInterceptor[0]);
    }

    final Optional<ServerInterceptor> rateMeteringInterceptor() {
        return getCustomRateMeteringInterceptor(coreApi.getConfig().appDataDir, this.getClass())
                .or(() -> Optional.of(CallRateMeteringInterceptor.valueOf(
                        new HashMap<>() {{
                            put(getGetTradeStatisticsMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                        }}
                )));
    }
}
