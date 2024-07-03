package tuskex.seednode;

import com.google.inject.Guice;
import tuskex.common.config.Config;
import tuskex.core.app.misc.AppSetupWithP2P;
import tuskex.core.app.misc.ModuleForAppWithP2p;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.Res;
import org.junit.jupiter.api.Test;

public class GuiceSetupTest {
    @Test
    public void testGuiceSetup() {
        Res.setup();
        CurrencyUtil.setup();

        ModuleForAppWithP2p module = new ModuleForAppWithP2p(new Config());
        Guice.createInjector(module).getInstance(AppSetupWithP2P.class);
    }
}
