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

package tuskex.core.user;

import com.google.common.collect.Lists;
import tuskex.core.alert.Alert;
import tuskex.core.arbitration.ArbitratorTest;
import tuskex.core.arbitration.MediatorTest;
import tuskex.core.filter.Filter;
import tuskex.core.proto.CoreProtoResolver;
import org.junit.jupiter.api.Disabled;

import java.util.HashSet;

public class UserPayloadModelVOTest {
    @Disabled("TODO InvalidKeySpecException at tuskex.common.crypto.Sig.getPublicKeyFromBytes(Sig.java:135)")
    public void testRoundtrip() {
        UserPayload vo = new UserPayload();
        vo.setAccountId("accountId");
        UserPayload newVo = UserPayload.fromProto(vo.toProtoMessage().getUserPayload(), new CoreProtoResolver());
    }

    @Disabled("TODO InvalidKeySpecException at tuskex.common.crypto.Sig.getPublicKeyFromBytes(Sig.java:135)")
    public void testRoundtripFull() {
        UserPayload vo = new UserPayload();
        vo.setAccountId("accountId");
        vo.setDisplayedAlert(new Alert("message", true, false, "version", new byte[]{12, -64, 12}, "string", null));
        vo.setDevelopersFilter(new Filter(Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                false,
                Lists.newArrayList(),
                null,
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                Lists.newArrayList(),
                null,
                0,
                null,
                null,
                null,
                null,
                false,
                Lists.newArrayList(),
                new HashSet<>(),
                false,
                false));

        vo.setRegisteredArbitrator(ArbitratorTest.getArbitratorMock());
        vo.setRegisteredMediator(MediatorTest.getMediatorMock());
        vo.setAcceptedArbitrators(Lists.newArrayList(ArbitratorTest.getArbitratorMock()));
        vo.setAcceptedMediators(Lists.newArrayList(MediatorTest.getMediatorMock()));
        UserPayload newVo = UserPayload.fromProto(vo.toProtoMessage().getUserPayload(), new CoreProtoResolver());
    }
}
