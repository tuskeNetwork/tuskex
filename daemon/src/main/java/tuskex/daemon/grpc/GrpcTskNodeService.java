/*
 * This file is part of Tuskex.
 *
 * Tuskex is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Tuskex is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Tuskex. If not, see <http://www.gnu.org/licenses/>.
 */

package tuskex.daemon.grpc;

import com.google.inject.Inject;
import tuskex.core.api.CoreApi;
import tuskex.core.tsk.TskNodeSettings;
import tuskex.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import tuskex.daemon.grpc.interceptor.GrpcCallRateMeter;
import static tuskex.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import tuskex.proto.grpc.GetTskNodeSettingsReply;
import tuskex.proto.grpc.GetTskNodeSettingsRequest;
import tuskex.proto.grpc.IsTskNodeOnlineReply;
import tuskex.proto.grpc.IsTskNodeOnlineRequest;
import tuskex.proto.grpc.StartTskNodeReply;
import tuskex.proto.grpc.StartTskNodeRequest;
import tuskex.proto.grpc.StopTskNodeReply;
import tuskex.proto.grpc.StopTskNodeRequest;
import tuskex.proto.grpc.TskNodeGrpc.TskNodeImplBase;
import static tuskex.proto.grpc.TskNodeGrpc.getGetTskNodeSettingsMethod;
import static tuskex.proto.grpc.TskNodeGrpc.getIsTskNodeOnlineMethod;
import static tuskex.proto.grpc.TskNodeGrpc.getStartTskNodeMethod;
import static tuskex.proto.grpc.TskNodeGrpc.getStopTskNodeMethod;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Optional;
import static java.util.concurrent.TimeUnit.SECONDS;
import lombok.extern.slf4j.Slf4j;
import monero.common.MoneroError;

@Slf4j
public class GrpcTskNodeService extends TskNodeImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcTskNodeService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void isTskNodeOnline(IsTskNodeOnlineRequest request,
                                    StreamObserver<IsTskNodeOnlineReply> responseObserver) {
        try {
            var reply = IsTskNodeOnlineReply.newBuilder()
                    .setIsRunning(coreApi.isTskNodeOnline())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getTskNodeSettings(GetTskNodeSettingsRequest request,
                                      StreamObserver<GetTskNodeSettingsReply> responseObserver) {
        try {
            var settings = coreApi.getTskNodeSettings();
            var builder = GetTskNodeSettingsReply.newBuilder();
            if (settings != null) {
                builder.setSettings(settings.toProtoMessage());
            }
            var reply = builder.build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void startTskNode(StartTskNodeRequest request,
                                StreamObserver<StartTskNodeReply> responseObserver) {
        try {
            var settings = request.getSettings();
            coreApi.startTskNode(TskNodeSettings.fromProto(settings));
            var reply = StartTskNodeReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (MoneroError me) {
            handleMoneroError(me, responseObserver);
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void stopTskNode(StopTskNodeRequest request,
                               StreamObserver<StopTskNodeReply> responseObserver) {
        try {
            coreApi.stopTskNode();
            var reply = StopTskNodeReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (MoneroError me) {
            handleMoneroError(me, responseObserver);
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    private void handleMoneroError(MoneroError me, StreamObserver<?> responseObserver) {
        // MoneroError is caused by the node startup failing, don't treat as unknown server error
        // by wrapping with a handled exception type.
        var headerLengthLimit = 8192; // MoneroErrors may print the entire monerod help text which causes a header overflow in grpc
        if (me.getMessage().length() > headerLengthLimit) {
            exceptionHandler.handleException(log, new IllegalStateException(me.getMessage().substring(0, headerLengthLimit - 1)), responseObserver);
        } else {
            exceptionHandler.handleException(log, new IllegalStateException(me), responseObserver);
        }
    }

    final ServerInterceptor[] interceptors() {
        Optional<ServerInterceptor> rateMeteringInterceptor = rateMeteringInterceptor();
        return rateMeteringInterceptor.map(serverInterceptor ->
                new ServerInterceptor[]{serverInterceptor}).orElseGet(() -> new ServerInterceptor[0]);
    }

    private Optional<ServerInterceptor> rateMeteringInterceptor() {
        return getCustomRateMeteringInterceptor(coreApi.getConfig().appDataDir, this.getClass())
                .or(() -> Optional.of(CallRateMeteringInterceptor.valueOf(
                        new HashMap<>() {{
                            int allowedCallsPerTimeWindow = 10;
                            put(getIsTskNodeOnlineMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getGetTskNodeSettingsMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getStartTskNodeMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getStopTskNodeMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                        }}
                )));
    }
}
