/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

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
import tuskex.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import tuskex.daemon.grpc.interceptor.GrpcCallRateMeter;
import static tuskex.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import tuskex.proto.grpc.AddConnectionReply;
import tuskex.proto.grpc.AddConnectionRequest;
import tuskex.proto.grpc.CheckConnectionReply;
import tuskex.proto.grpc.CheckConnectionRequest;
import tuskex.proto.grpc.CheckConnectionsReply;
import tuskex.proto.grpc.CheckConnectionsRequest;
import tuskex.proto.grpc.GetBestAvailableConnectionReply;
import tuskex.proto.grpc.GetBestAvailableConnectionRequest;
import tuskex.proto.grpc.GetConnectionReply;
import tuskex.proto.grpc.GetConnectionRequest;
import tuskex.proto.grpc.GetConnectionsReply;
import tuskex.proto.grpc.GetConnectionsRequest;
import tuskex.proto.grpc.RemoveConnectionReply;
import tuskex.proto.grpc.RemoveConnectionRequest;
import tuskex.proto.grpc.SetAutoSwitchReply;
import tuskex.proto.grpc.SetAutoSwitchRequest;
import tuskex.proto.grpc.SetConnectionReply;
import tuskex.proto.grpc.SetConnectionRequest;
import tuskex.proto.grpc.StartCheckingConnectionReply;
import tuskex.proto.grpc.StartCheckingConnectionRequest;
import tuskex.proto.grpc.StopCheckingConnectionReply;
import tuskex.proto.grpc.StopCheckingConnectionRequest;
import tuskex.proto.grpc.UrlConnection;
import static tuskex.proto.grpc.TskConnectionsGrpc.TskConnectionsImplBase;
import static tuskex.proto.grpc.TskConnectionsGrpc.getAddConnectionMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getCheckConnectionMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getCheckConnectionsMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getGetBestAvailableConnectionMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getGetConnectionMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getGetConnectionsMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getRemoveConnectionMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getSetAutoSwitchMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getSetConnectionMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getStartCheckingConnectionMethod;
import static tuskex.proto.grpc.TskConnectionsGrpc.getStopCheckingConnectionMethod;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import monero.common.MoneroRpcConnection;

@Slf4j
class GrpcTskConnectionService extends TskConnectionsImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcTskConnectionService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void addConnection(AddConnectionRequest request,
                              StreamObserver<AddConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.addTskConnection(toMoneroRpcConnection(request.getConnection()));
            return AddConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void removeConnection(RemoveConnectionRequest request,
                                 StreamObserver<RemoveConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.removeTskConnection(validateUri(request.getUrl()));
            return RemoveConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void getConnection(GetConnectionRequest request,
                              StreamObserver<GetConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            UrlConnection replyConnection = toUrlConnection(coreApi.getTskConnection());
            GetConnectionReply.Builder builder = GetConnectionReply.newBuilder();
            if (replyConnection != null) {
                builder.setConnection(replyConnection);
            }
            return builder.build();
        });
    }

    @Override
    public void getConnections(GetConnectionsRequest request,
                               StreamObserver<GetConnectionsReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            List<MoneroRpcConnection> connections = coreApi.getTskConnections();
            List<UrlConnection> replyConnections = connections.stream()
                    .map(GrpcTskConnectionService::toUrlConnection).collect(Collectors.toList());
            return GetConnectionsReply.newBuilder().addAllConnections(replyConnections).build();
        });
    }

    @Override
    public void setConnection(SetConnectionRequest request,
                              StreamObserver<SetConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            if (request.getUrl() != null && !request.getUrl().isEmpty())
                coreApi.setTskConnection(validateUri(request.getUrl()));
            else if (request.hasConnection())
                coreApi.setTskConnection(toMoneroRpcConnection(request.getConnection()));
            else coreApi.setTskConnection((MoneroRpcConnection) null); // disconnect from client
            return SetConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void checkConnection(CheckConnectionRequest request,
                                StreamObserver<CheckConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            MoneroRpcConnection connection = coreApi.checkTskConnection();
            UrlConnection replyConnection = toUrlConnection(connection);
            CheckConnectionReply.Builder builder = CheckConnectionReply.newBuilder();
            if (replyConnection != null) {
                builder.setConnection(replyConnection);
            }
            return builder.build();
        });
    }

    @Override
    public void checkConnections(CheckConnectionsRequest request,
                                 StreamObserver<CheckConnectionsReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            List<MoneroRpcConnection> connections = coreApi.checkTskConnections();
            List<UrlConnection> replyConnections = connections.stream()
                    .map(GrpcTskConnectionService::toUrlConnection).collect(Collectors.toList());
            return CheckConnectionsReply.newBuilder().addAllConnections(replyConnections).build();
        });
    }

    @Override
    public void startCheckingConnection(StartCheckingConnectionRequest request,
                                         StreamObserver<StartCheckingConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            int refreshMillis = request.getRefreshPeriod();
            Long refreshPeriod = refreshMillis == 0 ? null : (long) refreshMillis;
            coreApi.startCheckingTskConnection(refreshPeriod);
            return StartCheckingConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void stopCheckingConnection(StopCheckingConnectionRequest request,
                                        StreamObserver<StopCheckingConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.stopCheckingTskConnection();
            return StopCheckingConnectionReply.newBuilder().build();
        });
    }

    @Override
    public void getBestAvailableConnection(GetBestAvailableConnectionRequest request,
                                           StreamObserver<GetBestAvailableConnectionReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            MoneroRpcConnection connection = coreApi.getBestAvailableTskConnection();
            UrlConnection replyConnection = toUrlConnection(connection);
            GetBestAvailableConnectionReply.Builder builder = GetBestAvailableConnectionReply.newBuilder();
            if (replyConnection != null) {
                builder.setConnection(replyConnection);
            }
            return builder.build();
        });
    }

    @Override
    public void setAutoSwitch(SetAutoSwitchRequest request,
                              StreamObserver<SetAutoSwitchReply> responseObserver) {
        handleRequest(responseObserver, () -> {
            coreApi.setTskConnectionAutoSwitch(request.getAutoSwitch());
            return SetAutoSwitchReply.newBuilder().build();
        });
    }

    private <_Reply> void handleRequest(StreamObserver<_Reply> responseObserver,
                                        RpcRequestHandler<_Reply> handler) {
        try {
            _Reply reply = handler.handleRequest();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @FunctionalInterface
    private interface RpcRequestHandler<_Reply> {
        _Reply handleRequest() throws Exception;
    }


    private static UrlConnection toUrlConnection(MoneroRpcConnection rpcConnection) {
        if (rpcConnection == null) return null;
        return UrlConnection.newBuilder()
                .setUrl(rpcConnection.getUri())
                .setPriority(rpcConnection.getPriority())
                .setOnlineStatus(toOnlineStatus(rpcConnection.isOnline()))
                .setAuthenticationStatus(toAuthenticationStatus(rpcConnection.isAuthenticated()))
                .build();
    }

    private static UrlConnection.AuthenticationStatus toAuthenticationStatus(Boolean authenticated) {
        if (authenticated == null) return UrlConnection.AuthenticationStatus.NO_AUTHENTICATION;
        else if (authenticated) return UrlConnection.AuthenticationStatus.AUTHENTICATED;
        else return UrlConnection.AuthenticationStatus.NOT_AUTHENTICATED;
    }

    private static UrlConnection.OnlineStatus toOnlineStatus(Boolean online) {
        if (online == null) return UrlConnection.OnlineStatus.UNKNOWN;
        else if (online) return UrlConnection.OnlineStatus.ONLINE;
        else return UrlConnection.OnlineStatus.OFFLINE;
    }

    private static MoneroRpcConnection toMoneroRpcConnection(UrlConnection uriConnection) throws MalformedURLException {
        if (uriConnection == null) return null;
        return new MoneroRpcConnection(
                validateUri(uriConnection.getUrl()),
                nullIfEmpty(uriConnection.getUsername()),
                nullIfEmpty(uriConnection.getPassword()))
                .setPriority(uriConnection.getPriority());
    }

    private static String validateUri(String url) throws MalformedURLException {
        if (url.isEmpty()) throw new IllegalArgumentException("URL is required");
        return new URL(url).toString(); // validate and return
    }

    private static String nullIfEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value;
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
                            put(getAddConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getRemoveConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getGetConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getGetConnectionsMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getSetConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getCheckConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getCheckConnectionsMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getStartCheckingConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getStopCheckingConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getGetBestAvailableConnectionMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                            put(getSetAutoSwitchMethod().getFullMethodName(), new GrpcCallRateMeter(allowedCallsPerTimeWindow, SECONDS));
                        }}
                )));
    }
}
