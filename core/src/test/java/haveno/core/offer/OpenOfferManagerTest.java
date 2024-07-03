package tuskex.core.offer;

import tuskex.common.crypto.KeyRing;
import tuskex.common.crypto.KeyStorage;
import tuskex.common.file.CorruptedStorageFileHandler;
import tuskex.common.handlers.ErrorMessageHandler;
import tuskex.common.handlers.ResultHandler;
import tuskex.common.persistence.PersistenceManager;
import tuskex.core.api.CoreContext;
import tuskex.core.api.TskConnectionService;
import tuskex.core.trade.TradableList;
import tuskex.network.p2p.P2PService;
import tuskex.network.p2p.peers.PeerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.natpryce.makeiteasy.MakeItEasy.make;
import static tuskex.core.offer.OfferMaker.btcUsdOffer;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenOfferManagerTest {
    private PersistenceManager<TradableList<OpenOffer>> persistenceManager;
    private PersistenceManager<SignedOfferList> signedOfferPersistenceManager;
    private CoreContext coreContext;

    @BeforeEach
    public void setUp() throws Exception {
        var corruptedStorageFileHandler = mock(CorruptedStorageFileHandler.class);
        var storageDir = Files.createTempDirectory("storage").toFile();
        var keyRing = new KeyRing(new KeyStorage(storageDir));
        persistenceManager = new PersistenceManager<>(storageDir, null, corruptedStorageFileHandler, keyRing);
        signedOfferPersistenceManager = new PersistenceManager<>(storageDir, null, corruptedStorageFileHandler, keyRing);
        coreContext = new CoreContext();
    }

    @AfterEach
    public void tearDown() {
        persistenceManager.shutdown();
        signedOfferPersistenceManager.shutdown();
    }

    @Test
    public void testStartEditOfferForActiveOffer() {
        P2PService p2PService = mock(P2PService.class);
        OfferBookService offerBookService = mock(OfferBookService.class);
        TskConnectionService tskConnectionService = mock(TskConnectionService.class);

        when(p2PService.getPeerManager()).thenReturn(mock(PeerManager.class));

        final OpenOfferManager manager = new OpenOfferManager(coreContext,
                null,
                null,
                p2PService,
                tskConnectionService,
                null,
                null,
                null,
                offerBookService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                persistenceManager,
                signedOfferPersistenceManager,
                null);

        AtomicBoolean startEditOfferSuccessful = new AtomicBoolean(false);


        doAnswer(invocation -> {
            ((ResultHandler) invocation.getArgument(1)).handleResult();
            return null;
        }).when(offerBookService).deactivateOffer(any(OfferPayload.class), any(ResultHandler.class), any(ErrorMessageHandler.class));

        final OpenOffer openOffer = new OpenOffer(make(btcUsdOffer));
        openOffer.setState(OpenOffer.State.AVAILABLE);

        ResultHandler resultHandler = () -> startEditOfferSuccessful.set(true);

        manager.editOpenOfferStart(openOffer, resultHandler, null);

        verify(offerBookService, times(1)).deactivateOffer(any(OfferPayload.class), any(ResultHandler.class), any(ErrorMessageHandler.class));

        assertTrue(startEditOfferSuccessful.get());
    }

    @Test
    public void testStartEditOfferForDeactivatedOffer() {
        P2PService p2PService = mock(P2PService.class);
        OfferBookService offerBookService = mock(OfferBookService.class);
        TskConnectionService tskConnectionService = mock(TskConnectionService.class);
        when(p2PService.getPeerManager()).thenReturn(mock(PeerManager.class));

        final OpenOfferManager manager = new OpenOfferManager(coreContext,
                null,
                null,
                p2PService,
                tskConnectionService,
                null,
                null,
                null,
                offerBookService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                persistenceManager,
                signedOfferPersistenceManager,
                null);

        AtomicBoolean startEditOfferSuccessful = new AtomicBoolean(false);

        ResultHandler resultHandler = () -> startEditOfferSuccessful.set(true);

        final OpenOffer openOffer = new OpenOffer(make(btcUsdOffer));
        openOffer.setState(OpenOffer.State.DEACTIVATED);

        manager.editOpenOfferStart(openOffer, resultHandler, null);
        assertTrue(startEditOfferSuccessful.get());

    }

    @Test
    public void testStartEditOfferForOfferThatIsCurrentlyEdited() {
        P2PService p2PService = mock(P2PService.class);
        OfferBookService offerBookService = mock(OfferBookService.class);
        TskConnectionService tskConnectionService = mock(TskConnectionService.class);

        when(p2PService.getPeerManager()).thenReturn(mock(PeerManager.class));


        final OpenOfferManager manager = new OpenOfferManager(coreContext,
                null,
                null,
                p2PService,
                tskConnectionService,
                null,
                null,
                null,
                offerBookService,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                persistenceManager,
                signedOfferPersistenceManager,
                null);

        AtomicBoolean startEditOfferSuccessful = new AtomicBoolean(false);

        ResultHandler resultHandler = () -> startEditOfferSuccessful.set(true);

        final OpenOffer openOffer = new OpenOffer(make(btcUsdOffer));
        openOffer.setState(OpenOffer.State.DEACTIVATED);

        manager.editOpenOfferStart(openOffer, resultHandler, null);
        assertTrue(startEditOfferSuccessful.get());

        startEditOfferSuccessful.set(false);

        manager.editOpenOfferStart(openOffer, resultHandler, null);
        assertTrue(startEditOfferSuccessful.get());
    }

}
