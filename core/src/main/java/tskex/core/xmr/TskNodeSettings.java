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
package tuskex.core.tsk;

import tuskex.common.proto.persistable.PersistableEnvelope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

@Slf4j
@Data
@AllArgsConstructor
public class TskNodeSettings implements PersistableEnvelope {

    @Nullable
    String blockchainPath;
    @Nullable
    String bootstrapUrl;
    @Nullable
    List<String> startupFlags;

    public TskNodeSettings() {
    }

    public static TskNodeSettings fromProto(protobuf.TskNodeSettings proto) {
        return new TskNodeSettings(
                proto.getBlockchainPath(),
                proto.getBootstrapUrl(),
                proto.getStartupFlagsList());
    }

    @Override
    public protobuf.TskNodeSettings toProtoMessage() {
        protobuf.TskNodeSettings.Builder builder = protobuf.TskNodeSettings.newBuilder();
        Optional.ofNullable(blockchainPath).ifPresent(e -> builder.setBlockchainPath(blockchainPath));
        Optional.ofNullable(bootstrapUrl).ifPresent(e -> builder.setBootstrapUrl(bootstrapUrl));
        Optional.ofNullable(startupFlags).ifPresent(e -> builder.addAllStartupFlags(startupFlags));
        return builder.build();
    }
}
