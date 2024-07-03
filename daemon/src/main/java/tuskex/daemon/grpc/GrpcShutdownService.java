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

package tuskex.daemon.grpc;

import com.google.inject.Inject;
import tuskex.common.UserThread;
import tuskex.core.app.TuskexHeadlessApp;
import tuskex.proto.grpc.ShutdownServerGrpc;
import tuskex.proto.grpc.StopReply;
import tuskex.proto.grpc.StopRequest;
import io.grpc.stub.StreamObserver;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class GrpcShutdownService extends ShutdownServerGrpc.ShutdownServerImplBase {

    private final GrpcExceptionHandler exceptionHandler;

    @Inject
    public GrpcShutdownService(GrpcExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    public void stop(StopRequest req,
                     StreamObserver<StopReply> responseObserver) {
        try {
            log.info("Shutdown request received.");
            TuskexHeadlessApp.setOnGracefulShutDownHandler(new Runnable() {
                @Override
                public void run() {
                    var reply = StopReply.newBuilder().build();
                    responseObserver.onNext(reply);
                    responseObserver.onCompleted();
                }
            });
            UserThread.runAfter(TuskexHeadlessApp.getShutDownHandler(), 500, MILLISECONDS);
        } catch (Throwable cause) {
            exceptionHandler.handleException(log, cause, responseObserver);
        }
    }
}
