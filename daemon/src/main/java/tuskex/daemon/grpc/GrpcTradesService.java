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
import tuskex.common.config.Config;
import tuskex.core.api.CoreApi;
import tuskex.core.api.model.TradeInfo;
import static tuskex.core.api.model.TradeInfo.toTradeInfo;
import tuskex.core.trade.Trade;
import tuskex.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import tuskex.daemon.grpc.interceptor.GrpcCallRateMeter;
import static tuskex.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import tuskex.proto.grpc.CompleteTradeReply;
import tuskex.proto.grpc.CompleteTradeRequest;
import tuskex.proto.grpc.ConfirmPaymentReceivedReply;
import tuskex.proto.grpc.ConfirmPaymentReceivedRequest;
import tuskex.proto.grpc.ConfirmPaymentSentReply;
import tuskex.proto.grpc.ConfirmPaymentSentRequest;
import tuskex.proto.grpc.GetChatMessagesReply;
import tuskex.proto.grpc.GetChatMessagesRequest;
import tuskex.proto.grpc.GetTradeReply;
import tuskex.proto.grpc.GetTradeRequest;
import tuskex.proto.grpc.GetTradesReply;
import tuskex.proto.grpc.GetTradesRequest;
import tuskex.proto.grpc.SendChatMessageReply;
import tuskex.proto.grpc.SendChatMessageRequest;
import tuskex.proto.grpc.TakeOfferReply;
import tuskex.proto.grpc.TakeOfferRequest;
import tuskex.proto.grpc.TradesGrpc.TradesImplBase;
import static tuskex.proto.grpc.TradesGrpc.getCompleteTradeMethod;
import static tuskex.proto.grpc.TradesGrpc.getConfirmPaymentReceivedMethod;
import static tuskex.proto.grpc.TradesGrpc.getConfirmPaymentSentMethod;
import static tuskex.proto.grpc.TradesGrpc.getGetChatMessagesMethod;
import static tuskex.proto.grpc.TradesGrpc.getGetTradeMethod;
import static tuskex.proto.grpc.TradesGrpc.getGetTradesMethod;
import static tuskex.proto.grpc.TradesGrpc.getSendChatMessageMethod;
import static tuskex.proto.grpc.TradesGrpc.getTakeOfferMethod;
import static tuskex.proto.grpc.TradesGrpc.getWithdrawFundsMethod;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GrpcTradesService extends TradesImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcTradesService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void getTrade(GetTradeRequest req,
                         StreamObserver<GetTradeReply> responseObserver) {
        try {
            Trade trade = coreApi.getTrade(req.getTradeId());
            String role = coreApi.getTradeRole(req.getTradeId());
            var reply = GetTradeReply.newBuilder()
                    .setTrade(toTradeInfo(trade, role).toProtoMessage())
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException cause) {
            // Offer makers may call 'gettrade' many times before a trade exists.
            // Log a 'trade not found' warning instead of a full stack trace.
            cause.printStackTrace();
            exceptionHandler.handleExceptionAsWarning(log, "getTrade", cause, responseObserver);
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getTrades(GetTradesRequest req,
                         StreamObserver<GetTradesReply> responseObserver) {
        try {
            List<TradeInfo> trades = coreApi.getTrades()
                    .stream().map(TradeInfo::toTradeInfo)
                    .collect(Collectors.toList());
            var reply = GetTradesReply.newBuilder()
                    .addAllTrades(trades.stream()
                            .map(TradeInfo::toProtoMessage)
                            .collect(Collectors.toList()))
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void takeOffer(TakeOfferRequest req,
                          StreamObserver<TakeOfferReply> responseObserver) {
        GrpcErrorMessageHandler errorMessageHandler = new GrpcErrorMessageHandler(getTakeOfferMethod().getFullMethodName(), responseObserver, exceptionHandler, log);
        try {
            coreApi.takeOffer(req.getOfferId(),
                    req.getPaymentAccountId(),
                    req.getAmount(),
                    trade -> {
                        TradeInfo tradeInfo = toTradeInfo(trade);
                        var reply = TakeOfferReply.newBuilder()
                                .setTrade(tradeInfo.toProtoMessage())
                                .build();
                        responseObserver.onNext(reply);
                        responseObserver.onCompleted();
                    },
                    errorMessage -> {
                        if (!errorMessageHandler.isErrorHandled()) errorMessageHandler.handleErrorMessage(errorMessage);
                    });
        } catch (Throwable cause) {
            cause.printStackTrace();
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void confirmPaymentSent(ConfirmPaymentSentRequest req,
                                      StreamObserver<ConfirmPaymentSentReply> responseObserver) {
        GrpcErrorMessageHandler errorMessageHandler = new GrpcErrorMessageHandler(getConfirmPaymentSentMethod().getFullMethodName(), responseObserver, exceptionHandler, log);
        try {
            coreApi.confirmPaymentSent(req.getTradeId(),
                    () -> {
                        var reply = ConfirmPaymentSentReply.newBuilder().build();
                        responseObserver.onNext(reply);
                        responseObserver.onCompleted();
                    },
                    errorMessage -> {
                        if (!errorMessageHandler.isErrorHandled()) errorMessageHandler.handleErrorMessage(errorMessage);
                    });
        } catch (Throwable cause) {
            cause.printStackTrace();
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void confirmPaymentReceived(ConfirmPaymentReceivedRequest req,
                                       StreamObserver<ConfirmPaymentReceivedReply> responseObserver) {
        GrpcErrorMessageHandler errorMessageHandler = new GrpcErrorMessageHandler(getConfirmPaymentReceivedMethod().getFullMethodName(), responseObserver, exceptionHandler, log);
        try {
            coreApi.confirmPaymentReceived(req.getTradeId(),
                    () -> {
                        var reply = ConfirmPaymentReceivedReply.newBuilder().build();
                        responseObserver.onNext(reply);
                        responseObserver.onCompleted();
                    },
                    errorMessage -> {
                        if (!errorMessageHandler.isErrorHandled()) errorMessageHandler.handleErrorMessage(errorMessage);
                    });
        } catch (Throwable cause) {
            cause.printStackTrace();
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    // TODO: rename CompleteTradeRequest to CloseTradeRequest
    @Override
    public void completeTrade(CompleteTradeRequest req,
                          StreamObserver<CompleteTradeReply> responseObserver) {
        try {
            coreApi.closeTrade(req.getTradeId());
            var reply = CompleteTradeReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getChatMessages(GetChatMessagesRequest req,
                                StreamObserver<GetChatMessagesReply> responseObserver) {
        try {
            var tradeChats = coreApi.getChatMessages(req.getTradeId())
                    .stream()
                    .map(msg -> msg.toProtoNetworkEnvelope().getChatMessage())
                    .collect(Collectors.toList());
            var reply = GetChatMessagesReply.newBuilder()
                    .addAllMessage(tradeChats)
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void sendChatMessage(SendChatMessageRequest req,
                                StreamObserver<SendChatMessageReply> responseObserver) {
        try {
            coreApi.sendChatMessage(req.getTradeId(), req.getMessage());
            var reply = SendChatMessageReply.newBuilder().build();
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
                            put(getGetTradeMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 30 : 1, SECONDS));
                            put(getGetTradesMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 10 : 1, SECONDS));
                            put(getTakeOfferMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 20 : 3, Config.baseCurrencyNetwork().isTestnet() ? SECONDS : MINUTES));
                            put(getConfirmPaymentSentMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 10 : 3, Config.baseCurrencyNetwork().isTestnet() ? SECONDS : MINUTES));
                            put(getConfirmPaymentReceivedMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 10 : 3, Config.baseCurrencyNetwork().isTestnet() ? SECONDS : MINUTES));
                            put(getCompleteTradeMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 10 : 3, Config.baseCurrencyNetwork().isTestnet() ? SECONDS : MINUTES));
                            put(getWithdrawFundsMethod().getFullMethodName(), new GrpcCallRateMeter(3, MINUTES));
                            put(getGetChatMessagesMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 10 : 1, Config.baseCurrencyNetwork().isTestnet() ? SECONDS : MINUTES));
                            put(getSendChatMessageMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 10 : 1, Config.baseCurrencyNetwork().isTestnet() ? SECONDS : MINUTES));
                        }}
                )));
    }
}
