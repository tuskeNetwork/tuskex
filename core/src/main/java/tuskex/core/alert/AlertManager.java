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

package tuskex.core.alert;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import tuskex.common.app.DevEnv;
import tuskex.common.config.Config;
import tuskex.common.crypto.KeyRing;
import tuskex.core.user.User;
import tuskex.network.p2p.P2PService;
import tuskex.network.p2p.storage.HashMapChangedListener;
import tuskex.network.p2p.storage.payload.ProtectedStorageEntry;
import tuskex.network.p2p.storage.payload.ProtectedStoragePayload;
import java.math.BigInteger;
import java.security.SignatureException;
import java.util.Collection;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Utils;
import static org.bitcoinj.core.Utils.HEX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertManager {
    private static final Logger log = LoggerFactory.getLogger(AlertManager.class);

    private final P2PService p2PService;
    private final KeyRing keyRing;
    private final User user;
    private final ObjectProperty<Alert> alertMessageProperty = new SimpleObjectProperty<>();
    private final boolean useDevPrivilegeKeys;

    private ECKey alertSigningKey;


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor, Initialization
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    public AlertManager(P2PService p2PService,
                        KeyRing keyRing,
                        User user,
                        @Named(Config.IGNORE_DEV_MSG) boolean ignoreDevMsg,
                        @Named(Config.USE_DEV_PRIVILEGE_KEYS) boolean useDevPrivilegeKeys) {
        this.p2PService = p2PService;
        this.keyRing = keyRing;
        this.user = user;
        this.useDevPrivilegeKeys = useDevPrivilegeKeys;

        if (!ignoreDevMsg) {
            p2PService.addHashSetChangedListener(new HashMapChangedListener() {
                @Override
                public void onAdded(Collection<ProtectedStorageEntry> protectedStorageEntries) {
                    protectedStorageEntries.forEach(protectedStorageEntry -> {
                        final ProtectedStoragePayload protectedStoragePayload = protectedStorageEntry.getProtectedStoragePayload();
                        if (protectedStoragePayload instanceof Alert) {
                            Alert alert = (Alert) protectedStoragePayload;
                            if (verifySignature(alert))
                                alertMessageProperty.set(alert);
                        }
                    });
                }

                @Override
                public void onRemoved(Collection<ProtectedStorageEntry> protectedStorageEntries) {
                    protectedStorageEntries.forEach(protectedStorageEntry -> {
                        final ProtectedStoragePayload protectedStoragePayload = protectedStorageEntry.getProtectedStoragePayload();
                        if (protectedStoragePayload instanceof Alert) {
                            if (verifySignature((Alert) protectedStoragePayload))
                                alertMessageProperty.set(null);
                        }
                    });
                }
            });
        }
    }

    protected List<String> getPubKeyList() {
        if (useDevPrivilegeKeys) return List.of(DevEnv.DEV_PRIVILEGE_PUB_KEY);
        switch (Config.baseCurrencyNetwork()) {
        case TSK_LOCAL:
            return List.of(
                    "0280c207a23a361cdab1d6f34ec65d39bbd681da7372358b0404a2a26717829bc5",
                    "02ee9dd4be096ece5ccc7d4dd546302178e0ba3b9720062e7ba68dfacf5bfacb13",
                    "0347fc95e1d445523e909c1717cf1208721d68af945da628a66b9db7c6c9f10b23");
        case TSK_STAGENET:
            return List.of(
                    "0280c207a23a361cdab1d6f34ec65d39bbd681da7372358b0404a2a26717829bc5",
                    "02ee9dd4be096ece5ccc7d4dd546302178e0ba3b9720062e7ba68dfacf5bfacb13",
                    "0347fc95e1d445523e909c1717cf1208721d68af945da628a66b9db7c6c9f10b23");
        case TSK_MAINNET:
            return List.of();
        default:
            throw new RuntimeException("Unhandled base currency network: " + Config.baseCurrencyNetwork());
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////

    public ReadOnlyObjectProperty<Alert> alertMessageProperty() {
        return alertMessageProperty;
    }

    public boolean addAlertMessageIfKeyIsValid(Alert alert, String privKeyString) {
        // if there is a previous message we remove that first
        if (user.getDevelopersAlert() != null)
            removeAlertMessageIfKeyIsValid(privKeyString);

        boolean isKeyValid = isKeyValid(privKeyString);
        if (isKeyValid) {
            signAndAddSignatureToAlertMessage(alert);
            user.setDevelopersAlert(alert);
            boolean result = p2PService.addProtectedStorageEntry(alert);
            if (result) {
                log.trace("Add alertMessage to network was successful. AlertMessage={}", alert);
            }

        }
        return isKeyValid;
    }

    public boolean removeAlertMessageIfKeyIsValid(String privKeyString) {
        Alert alert = user.getDevelopersAlert();
        if (isKeyValid(privKeyString) && alert != null) {
            if (p2PService.removeData(alert))
                log.trace("Remove alertMessage from network was successful. AlertMessage={}", alert);

            user.setDevelopersAlert(null);
            return true;
        } else {
            return false;
        }
    }

    private boolean isKeyValid(String privKeyString) {
        try {
            alertSigningKey = ECKey.fromPrivate(new BigInteger(1, HEX.decode(privKeyString)));
            return getPubKeyList().contains(Utils.HEX.encode(alertSigningKey.getPubKey()));
        } catch (Throwable t) {
            return false;
        }
    }

    private void signAndAddSignatureToAlertMessage(Alert alert) {
        String alertMessageAsHex = Utils.HEX.encode(alert.getMessage().getBytes(Charsets.UTF_8));
        String signatureAsBase64 = alertSigningKey.signMessage(alertMessageAsHex);
        alert.setSigAndPubKey(signatureAsBase64, keyRing.getSignatureKeyPair().getPublic());
    }

    private boolean verifySignature(Alert alert) {
        String alertMessageAsHex = Utils.HEX.encode(alert.getMessage().getBytes(Charsets.UTF_8));
        for (String pubKeyAsHex : getPubKeyList()) {
            try {
                ECKey.fromPublicOnly(HEX.decode(pubKeyAsHex)).verifyMessage(alertMessageAsHex, alert.getSignatureAsBase64());
                return true;
            } catch (SignatureException e) {
                // ignore
            }
        }
        log.warn("verifySignature failed");
        return false;
    }
}
