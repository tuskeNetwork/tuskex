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
import tuskex.common.UserThread;
import tuskex.common.config.Config;
import tuskex.core.api.CoreApi;
import tuskex.core.api.model.AddressBalanceInfo;
import static tuskex.core.api.model.TskTx.toTskTx;
import tuskex.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import tuskex.daemon.grpc.interceptor.GrpcCallRateMeter;
import static tuskex.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import tuskex.proto.grpc.CreateTskTxReply;
import tuskex.proto.grpc.CreateTskTxRequest;
import tuskex.proto.grpc.GetAddressBalanceReply;
import tuskex.proto.grpc.GetAddressBalanceRequest;
import tuskex.proto.grpc.GetBalancesReply;
import tuskex.proto.grpc.GetBalancesRequest;
import tuskex.proto.grpc.GetFundingAddressesReply;
import tuskex.proto.grpc.GetFundingAddressesRequest;
import tuskex.proto.grpc.GetTskNewSubaddressReply;
import tuskex.proto.grpc.GetTskNewSubaddressRequest;
import tuskex.proto.grpc.GetTskPrimaryAddressReply;
import tuskex.proto.grpc.GetTskPrimaryAddressRequest;
import tuskex.proto.grpc.GetTskSeedReply;
import tuskex.proto.grpc.GetTskSeedRequest;
import tuskex.proto.grpc.GetTskTxsReply;
import tuskex.proto.grpc.GetTskTxsRequest;
import tuskex.proto.grpc.LockWalletReply;
import tuskex.proto.grpc.LockWalletRequest;
import tuskex.proto.grpc.RelayTskTxReply;
import tuskex.proto.grpc.RelayTskTxRequest;
import tuskex.proto.grpc.RemoveWalletPasswordReply;
import tuskex.proto.grpc.RemoveWalletPasswordRequest;
import tuskex.proto.grpc.SetWalletPasswordReply;
import tuskex.proto.grpc.SetWalletPasswordRequest;
import tuskex.proto.grpc.UnlockWalletReply;
import tuskex.proto.grpc.UnlockWalletRequest;
import tuskex.proto.grpc.WalletsGrpc.WalletsImplBase;
import static tuskex.proto.grpc.WalletsGrpc.getGetAddressBalanceMethod;
import static tuskex.proto.grpc.WalletsGrpc.getGetBalancesMethod;
import static tuskex.proto.grpc.WalletsGrpc.getGetFundingAddressesMethod;
import static tuskex.proto.grpc.WalletsGrpc.getLockWalletMethod;
import static tuskex.proto.grpc.WalletsGrpc.getRemoveWalletPasswordMethod;
import static tuskex.proto.grpc.WalletsGrpc.getSetWalletPasswordMethod;
import static tuskex.proto.grpc.WalletsGrpc.getUnlockWalletMethod;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import monero.wallet.model.MoneroDestination;
import monero.wallet.model.MoneroTxWallet;

@Slf4j
class GrpcWalletsService extends WalletsImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcWalletsService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void getBalances(GetBalancesRequest req, StreamObserver<GetBalancesReply> responseObserver) {
        UserThread.execute(() -> { // TODO (woodser): Balances.updateBalances() runs on UserThread for JFX components, so call from user thread, else the properties may not be updated. remove JFX properties or push delay into CoreWalletsService.getTskBalances()?
            try {
                var balances = coreApi.getBalances(req.getCurrencyCode());
                var reply = GetBalancesReply.newBuilder()
                        .setBalances(balances.toProtoMessage())
                        .build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();
            } catch (Throwable cause) {
                exceptionHandler.handleException(log, cause, responseObserver);
            }
        });
    }

    @Override
    public void getTskSeed(GetTskSeedRequest req,
                                    StreamObserver<GetTskSeedReply> responseObserver) {
        try {
            var reply = GetTskSeedReply.newBuilder()
                    .setSeed(coreApi.getTskSeed())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getTskPrimaryAddress(GetTskPrimaryAddressRequest req,
                                     StreamObserver<GetTskPrimaryAddressReply> responseObserver) {
        try {
            var reply = GetTskPrimaryAddressReply.newBuilder()
                    .setPrimaryAddress(coreApi.getTskPrimaryAddress())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getTskNewSubaddress(GetTskNewSubaddressRequest req,
                                    StreamObserver<GetTskNewSubaddressReply> responseObserver) {
        try {
            String subaddress = coreApi.getTskNewSubaddress();
            var reply = GetTskNewSubaddressReply.newBuilder()
                    .setSubaddress(subaddress)
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getTskTxs(GetTskTxsRequest req, StreamObserver<GetTskTxsReply> responseObserver) {
        try {
            List<MoneroTxWallet> tskTxs = coreApi.getTskTxs();
            var reply = GetTskTxsReply.newBuilder()
                    .addAllTxs(tskTxs.stream()
                            .map(s -> toTskTx(s).toProtoMessage())
                            .collect(Collectors.toList()))
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void createTskTx(CreateTskTxRequest req,
                            StreamObserver<CreateTskTxReply> responseObserver) {
        try {
            MoneroTxWallet tx = coreApi.createTskTx(
                    req.getDestinationsList()
                    .stream()
                    .map(s -> new MoneroDestination(s.getAddress(), new BigInteger(s.getAmount())))
                    .collect(Collectors.toList()));
            log.info("Successfully created TSK tx: hash {}", tx.getHash());
            var reply = CreateTskTxReply.newBuilder()
                    .setTx(toTskTx(tx).toProtoMessage())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void relayTskTx(RelayTskTxRequest req,
                            StreamObserver<RelayTskTxReply> responseObserver) {
        try {
            String txHash = coreApi.relayTskTx(req.getMetadata());
            var reply = RelayTskTxReply.newBuilder()
                    .setHash(txHash)
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getAddressBalance(GetAddressBalanceRequest req,
                                  StreamObserver<GetAddressBalanceReply> responseObserver) {
        try {
            AddressBalanceInfo balanceInfo = coreApi.getAddressBalanceInfo(req.getAddress());
            var reply = GetAddressBalanceReply.newBuilder()
                    .setAddressBalanceInfo(balanceInfo.toProtoMessage()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getFundingAddresses(GetFundingAddressesRequest req,
                                    StreamObserver<GetFundingAddressesReply> responseObserver) {
        try {
            List<AddressBalanceInfo> balanceInfo = coreApi.getFundingAddresses();
            var reply = GetFundingAddressesReply.newBuilder()
                    .addAllAddressBalanceInfo(
                            balanceInfo.stream()
                                    .map(AddressBalanceInfo::toProtoMessage)
                                    .collect(Collectors.toList()))
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void setWalletPassword(SetWalletPasswordRequest req,
                                  StreamObserver<SetWalletPasswordReply> responseObserver) {
        try {
            coreApi.setWalletPassword(req.getPassword(), req.getNewPassword());
            var reply = SetWalletPasswordReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void removeWalletPassword(RemoveWalletPasswordRequest req,
                                     StreamObserver<RemoveWalletPasswordReply> responseObserver) {
        try {
            coreApi.removeWalletPassword(req.getPassword());
            var reply = RemoveWalletPasswordReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void lockWallet(LockWalletRequest req,
                           StreamObserver<LockWalletReply> responseObserver) {
        try {
            coreApi.lockWallet();
            var reply = LockWalletReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void unlockWallet(UnlockWalletRequest req,
                             StreamObserver<UnlockWalletReply> responseObserver) {
        try {
            coreApi.unlockWallet(req.getPassword(), req.getTimeout());
            var reply = UnlockWalletReply.newBuilder().build();
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
                            put(getGetBalancesMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 100 : 1, SECONDS)); // TODO: why do tests make so many calls to get balances?
                            put(getGetAddressBalanceMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getGetFundingAddressesMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));

                            // Trying to set or remove a wallet password several times before the 1st attempt has time to
                            // persist the change to disk may corrupt the wallet, so allow only 1 attempt per 5 seconds.
                            put(getSetWalletPasswordMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS, 5));
                            put(getRemoveWalletPasswordMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS, 5));

                            put(getLockWalletMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                            put(getUnlockWalletMethod().getFullMethodName(), new GrpcCallRateMeter(1, SECONDS));
                        }}
                )));
    }
}
