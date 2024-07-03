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

import tuskex.common.config.Config;
import com.google.inject.Inject;
import tuskex.core.api.CoreApi;
import tuskex.core.api.model.MarketDepthInfo;
import tuskex.core.api.model.MarketPriceInfo;
import tuskex.daemon.grpc.interceptor.CallRateMeteringInterceptor;
import tuskex.daemon.grpc.interceptor.GrpcCallRateMeter;
import static tuskex.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import tuskex.proto.grpc.MarketDepthReply;
import tuskex.proto.grpc.MarketDepthRequest;
import tuskex.proto.grpc.MarketPriceReply;
import tuskex.proto.grpc.MarketPriceRequest;
import tuskex.proto.grpc.MarketPricesReply;
import tuskex.proto.grpc.MarketPricesRequest;
import static tuskex.proto.grpc.PriceGrpc.PriceImplBase;
import static tuskex.proto.grpc.PriceGrpc.getGetMarketPriceMethod;
import tuskex.proto.grpc.PriceGrpc.PriceImplBase;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static tuskex.daemon.grpc.interceptor.GrpcServiceRateMeteringConfig.getCustomRateMeteringInterceptor;
import static tuskex.proto.grpc.PriceGrpc.getGetMarketPriceMethod;
import static java.util.concurrent.TimeUnit.SECONDS;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GrpcPriceService extends PriceImplBase {

    private final CoreApi coreApi;
    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcPriceService(CoreApi coreApi, GrpcExceptionHandler exceptionHandler) {
        this.coreApi = coreApi;
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void getMarketPrice(MarketPriceRequest req,
                               StreamObserver<MarketPriceReply> responseObserver) {
        try {
            double marketPrice = coreApi.getMarketPrice(req.getCurrencyCode());
            responseObserver.onNext(MarketPriceReply.newBuilder().setPrice(marketPrice).build());
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getMarketPrices(MarketPricesRequest request,
                                StreamObserver<MarketPricesReply> responseObserver) {
        try {
            responseObserver.onNext(mapMarketPricesReply(coreApi.getMarketPrices()));
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    @Override
    public void getMarketDepth(MarketDepthRequest req,
                               StreamObserver<MarketDepthReply> responseObserver) {
        try {
            responseObserver.onNext(mapMarketDepthReply(coreApi.getMarketDepth(req.getCurrencyCode())));
            responseObserver.onCompleted();
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }

    private MarketPricesReply mapMarketPricesReply(List<MarketPriceInfo> marketPrices) {
        MarketPricesReply.Builder builder = MarketPricesReply.newBuilder();
        marketPrices.stream()
                .map(MarketPriceInfo::toProtoMessage)
                .forEach(builder::addMarketPrice);
        return builder.build();
    }

    private MarketDepthReply mapMarketDepthReply(MarketDepthInfo marketDepth) {
        return MarketDepthReply.newBuilder().setMarketDepth(marketDepth.toProtoMessage()).build();
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
                            put(getGetMarketPriceMethod().getFullMethodName(), new GrpcCallRateMeter(Config.baseCurrencyNetwork().isTestnet() ? 20 : 1, SECONDS));
                        }}
                )));
    }
}
