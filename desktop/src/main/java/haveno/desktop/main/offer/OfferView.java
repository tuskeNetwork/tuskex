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

package tuskex.desktop.main.offer;

import tuskex.common.UserThread;
import tuskex.core.locale.CurrencyUtil;
import tuskex.core.locale.GlobalSettings;
import tuskex.core.locale.Res;
import tuskex.core.locale.TradeCurrency;
import tuskex.core.offer.Offer;
import tuskex.core.offer.OfferDirection;
import tuskex.core.payment.payload.PaymentMethod;
import tuskex.core.user.Preferences;
import tuskex.core.user.User;
import tuskex.desktop.Navigation;
import tuskex.desktop.common.view.ActivatableView;
import tuskex.desktop.common.view.View;
import tuskex.desktop.common.view.ViewLoader;
import tuskex.desktop.main.MainView;
import tuskex.desktop.main.offer.createoffer.CreateOfferView;
import tuskex.desktop.main.offer.offerbook.TskOfferBookView;
import tuskex.desktop.main.offer.offerbook.OfferBookView;
import tuskex.desktop.main.offer.offerbook.OtherOfferBookView;
import tuskex.desktop.main.offer.offerbook.TopCryptoOfferBookView;
import tuskex.desktop.main.offer.takeoffer.TakeOfferView;
import tuskex.desktop.util.GUIUtil;
import tuskex.network.p2p.P2PService;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class OfferView extends ActivatableView<TabPane, Void> {

    private OfferBookView<?, ?> tskOfferBookView, topCryptoOfferBookView, otherOfferBookView;

    private Tab tskOfferBookTab, topCryptoOfferBookTab, otherOfferBookTab;

    private final ViewLoader viewLoader;
    private final Navigation navigation;
    private final Preferences preferences;
    private final User user;
    private final P2PService p2PService;
    private final OfferDirection direction;

    private Offer offer;
    private TradeCurrency tradeCurrency;
    private Navigation.Listener navigationListener;
    private ChangeListener<Tab> tabChangeListener;
    private OfferView.OfferActionHandler offerActionHandler;

    protected OfferView(ViewLoader viewLoader,
                        Navigation navigation,
                        Preferences preferences,
                        User user,
                        P2PService p2PService,
                        OfferDirection direction) {
        this.viewLoader = viewLoader;
        this.navigation = navigation;
        this.preferences = preferences;
        this.user = user;
        this.p2PService = p2PService;
        this.direction = direction;
    }

    @Override
    protected void initialize() {
        navigationListener = (viewPath, data) -> {
            UserThread.execute(() -> {
                if (viewPath.size() == 3 && viewPath.indexOf(this.getClass()) == 1) {
                    loadView(viewPath.tip(), null, data);
                } else if (viewPath.size() == 4 && viewPath.indexOf(this.getClass()) == 1) {
                    loadView(viewPath.get(2), viewPath.tip(), data);
                }
            });
        };
        tabChangeListener = (observableValue, oldValue, newValue) -> {
            UserThread.execute(() -> {
                if (newValue != null) {
                    if (newValue.equals(tskOfferBookTab)) {
                        if (tskOfferBookView != null) {
                            tskOfferBookView.onTabSelected(true);
                        } else {
                            loadView(TskOfferBookView.class, null, null);
                        }
                    } else if (newValue.equals(topCryptoOfferBookTab)) {
                        if (topCryptoOfferBookView != null) {
                            topCryptoOfferBookView.onTabSelected(true);
                        } else {
                            loadView(TopCryptoOfferBookView.class, null, null);
                        }
                    } else if (newValue.equals(otherOfferBookTab)) {
                        if (otherOfferBookView != null) {
                            otherOfferBookView.onTabSelected(true);
                        } else {
                            loadView(OtherOfferBookView.class, null, null);
                        }
                    }
                }
                if (oldValue != null) {
                    if (oldValue.equals(tskOfferBookTab) && tskOfferBookView != null) {
                        tskOfferBookView.onTabSelected(false);
                    } else if (oldValue.equals(topCryptoOfferBookTab) && topCryptoOfferBookView != null) {
                        topCryptoOfferBookView.onTabSelected(false);
                    } else if (oldValue.equals(otherOfferBookTab) && otherOfferBookView != null) {
                        otherOfferBookView.onTabSelected(false);
                    }
                }
            });
        };

        offerActionHandler = new OfferActionHandler() {
            @Override
            public void onCreateOffer(TradeCurrency tradeCurrency, PaymentMethod paymentMethod) {
                if (canCreateOrTakeOffer(tradeCurrency)) {
                    showCreateOffer(tradeCurrency, paymentMethod);
                }
            }

            @Override
            public void onTakeOffer(Offer offer) {
                Optional<TradeCurrency> optionalTradeCurrency = CurrencyUtil.getTradeCurrency(offer.getCurrencyCode());
                if (optionalTradeCurrency.isPresent() && canCreateOrTakeOffer(optionalTradeCurrency.get())) {
                    showTakeOffer(offer);
                }
            }
        };
    }

    @Override
    protected void activate() {
        Optional<TradeCurrency> tradeCurrencyOptional = (this.direction == OfferDirection.SELL) ?
                CurrencyUtil.getTradeCurrency(preferences.getSellScreenCurrencyCode()) :
                CurrencyUtil.getTradeCurrency(preferences.getBuyScreenCurrencyCode());
        tradeCurrency = tradeCurrencyOptional.orElseGet(GlobalSettings::getDefaultTradeCurrency);

        root.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);
        navigation.addListener(navigationListener);
        if (tskOfferBookView == null) {
            navigation.navigateTo(MainView.class, this.getClass(), TskOfferBookView.class);
        }

        GUIUtil.updateTopCrypto(preferences);

        if (topCryptoOfferBookTab != null) {
            topCryptoOfferBookTab.setText(GUIUtil.TOP_CRYPTO.getName().toUpperCase());
        }
    }

    @Override
    protected void deactivate() {
        navigation.removeListener(navigationListener);
        root.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
    }

    private void loadView(Class<? extends View> viewClass,
                          Class<? extends View> childViewClass,
                          @Nullable Object data) {
        TabPane tabPane = root;
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        if (OfferBookView.class.isAssignableFrom(viewClass)) {

            if (viewClass == TskOfferBookView.class && tskOfferBookTab != null && tskOfferBookView != null) {
                if (childViewClass == null) {
                    tskOfferBookTab.setContent(tskOfferBookView.getRoot());
                } else if (childViewClass == TakeOfferView.class) {
                    loadTakeViewClass(viewClass, childViewClass, tskOfferBookTab);
                } else {
                    loadCreateViewClass(tskOfferBookView, viewClass, childViewClass, tskOfferBookTab, (PaymentMethod) data);
                }
                tabPane.getSelectionModel().select(tskOfferBookTab);
            } else if (viewClass == TopCryptoOfferBookView.class && topCryptoOfferBookTab != null && topCryptoOfferBookView != null) {
                if (childViewClass == null) {
                    topCryptoOfferBookTab.setContent(topCryptoOfferBookView.getRoot());
                } else if (childViewClass == TakeOfferView.class) {
                    loadTakeViewClass(viewClass, childViewClass, topCryptoOfferBookTab);
                } else {
                    tradeCurrency = GUIUtil.TOP_CRYPTO;
                    loadCreateViewClass(topCryptoOfferBookView, viewClass, childViewClass, topCryptoOfferBookTab, (PaymentMethod) data);
                }
                tabPane.getSelectionModel().select(topCryptoOfferBookTab);
            } else if (viewClass == OtherOfferBookView.class && otherOfferBookTab != null && otherOfferBookView != null) {
                if (childViewClass == null) {
                    otherOfferBookTab.setContent(otherOfferBookView.getRoot());
                } else if (childViewClass == TakeOfferView.class) {
                    loadTakeViewClass(viewClass, childViewClass, otherOfferBookTab);
                } else {
                    //add sanity check in case of app restart
                    if (CurrencyUtil.isTraditionalCurrency(tradeCurrency.getCode())) {
                        Optional<TradeCurrency> tradeCurrencyOptional = (this.direction == OfferDirection.SELL) ?
                                CurrencyUtil.getTradeCurrency(preferences.getSellScreenCryptoCurrencyCode()) :
                                CurrencyUtil.getTradeCurrency(preferences.getBuyScreenCryptoCurrencyCode());
                        tradeCurrency = tradeCurrencyOptional.isEmpty() ? OfferViewUtil.getAnyOfMainCryptoCurrencies() : tradeCurrencyOptional.get();
                    }
                    loadCreateViewClass(otherOfferBookView, viewClass, childViewClass, otherOfferBookTab, (PaymentMethod) data);
                }
                tabPane.getSelectionModel().select(otherOfferBookTab);
            } else {
                if (tskOfferBookTab == null) {
                    tskOfferBookTab = new Tab(Res.getBaseCurrencyName().toUpperCase());
                    tskOfferBookTab.setClosable(false);
                    topCryptoOfferBookTab = new Tab(GUIUtil.TOP_CRYPTO.getName().toUpperCase());
                    topCryptoOfferBookTab.setClosable(false);
                    otherOfferBookTab = new Tab(Res.get("shared.other").toUpperCase());
                    otherOfferBookTab.setClosable(false);

                    tabPane.getTabs().addAll(tskOfferBookTab, topCryptoOfferBookTab, otherOfferBookTab);
                }
                if (viewClass == TskOfferBookView.class) {
                    tskOfferBookView = (TskOfferBookView) viewLoader.load(TskOfferBookView.class);
                    tskOfferBookView.setOfferActionHandler(offerActionHandler);
                    tskOfferBookView.setDirection(direction);
                    tskOfferBookView.onTabSelected(true);
                    tabPane.getSelectionModel().select(tskOfferBookTab);
                    tskOfferBookTab.setContent(tskOfferBookView.getRoot());
                } else if (viewClass == TopCryptoOfferBookView.class) {
                    topCryptoOfferBookView = (TopCryptoOfferBookView) viewLoader.load(TopCryptoOfferBookView.class);
                    topCryptoOfferBookView.setOfferActionHandler(offerActionHandler);
                    topCryptoOfferBookView.setDirection(direction);
                    topCryptoOfferBookView.onTabSelected(true);
                    tabPane.getSelectionModel().select(topCryptoOfferBookTab);
                    topCryptoOfferBookTab.setContent(topCryptoOfferBookView.getRoot());
                } else if (viewClass == OtherOfferBookView.class) {
                    otherOfferBookView = (OtherOfferBookView) viewLoader.load(OtherOfferBookView.class);
                    otherOfferBookView.setOfferActionHandler(offerActionHandler);
                    otherOfferBookView.setDirection(direction);
                    otherOfferBookView.onTabSelected(true);
                    tabPane.getSelectionModel().select(otherOfferBookTab);
                    otherOfferBookTab.setContent(otherOfferBookView.getRoot());
                }
            }
        }
    }

    private void loadCreateViewClass(OfferBookView<?, ?> offerBookView,
                                     Class<? extends View> viewClass,
                                     Class<? extends View> childViewClass,
                                     Tab marketOfferBookTab,
                                     @Nullable PaymentMethod paymentMethod) {
        if (tradeCurrency == null) {
            return;
        }

        View view;
        // CreateOffer and TakeOffer must not be cached by ViewLoader as we cannot use a view multiple times
        // in different graphs
        view = viewLoader.load(childViewClass);

        // Invert direction for non-Fiat trade currencies -> BUY BCH is to SELL Monero
        OfferDirection offerDirection = CurrencyUtil.isFiatCurrency(tradeCurrency.getCode()) ? direction :
                direction == OfferDirection.BUY ? OfferDirection.SELL : OfferDirection.BUY;

        ((CreateOfferView) view).initWithData(offerDirection, tradeCurrency, offerActionHandler);

        ((SelectableView) view).onTabSelected(true);

        ((ClosableView) view).setCloseHandler(() -> {
            offerBookView.enableCreateOfferButton();
            ((SelectableView) view).onTabSelected(false);
            //reset tab
            navigation.navigateTo(MainView.class, this.getClass(), viewClass);
        });

        // close handler from close on create offer action
        marketOfferBookTab.setContent(view.getRoot());
    }

    private void loadTakeViewClass(Class<? extends View> viewClass,
                                   Class<? extends View> childViewClass,
                                   Tab marketOfferBookTab) {

        if (offer == null) {
            return;
        }

        View view = viewLoader.load(childViewClass);
        // CreateOffer and TakeOffer must not be cached by ViewLoader as we cannot use a view multiple times
        // in different graphs
        ((InitializableViewWithTakeOfferData) view).initWithData(offer);
        ((SelectableView) view).onTabSelected(true);

        // close handler from close on take offer action
        ((ClosableView) view).setCloseHandler(() -> {
            ((SelectableView) view).onTabSelected(false);
            navigation.navigateTo(MainView.class, this.getClass(), viewClass);
        });
        marketOfferBookTab.setContent(view.getRoot());
    }

    protected boolean canCreateOrTakeOffer(TradeCurrency tradeCurrency) {
        return GUIUtil.isBootstrappedOrShowPopup(p2PService) &&
                GUIUtil.canCreateOrTakeOfferOrShowPopup(user, navigation);
    }

    private void showTakeOffer(Offer offer) {
        this.offer = offer;

        Class<? extends OfferBookView<?, ?>> offerBookViewClass = getOfferBookViewClassFor(offer.getCurrencyCode());
        navigation.navigateTo(MainView.class, this.getClass(), offerBookViewClass, TakeOfferView.class);
    }

    private void showCreateOffer(TradeCurrency tradeCurrency, PaymentMethod paymentMethod) {
        this.tradeCurrency = tradeCurrency;

        Class<? extends OfferBookView<?, ?>> offerBookViewClass = getOfferBookViewClassFor(tradeCurrency.getCode());
        navigation.navigateToWithData(paymentMethod, MainView.class, this.getClass(), offerBookViewClass, CreateOfferView.class);
    }

    @NotNull
    private Class<? extends OfferBookView<?, ?>> getOfferBookViewClassFor(String currencyCode) {
        Class<? extends OfferBookView<?, ?>> offerBookViewClass;
        if (CurrencyUtil.isFiatCurrency(currencyCode)) {
            offerBookViewClass = TskOfferBookView.class;
        } else if (currencyCode.equals(GUIUtil.TOP_CRYPTO.getCode())) {
            offerBookViewClass = TopCryptoOfferBookView.class;
        } else {
            offerBookViewClass = OtherOfferBookView.class;
        }
        return offerBookViewClass;
    }

    public interface OfferActionHandler {
        void onCreateOffer(TradeCurrency tradeCurrency, PaymentMethod paymentMethod);

        void onTakeOffer(Offer offer);
    }

    public interface CloseHandler {
        void close();
    }
}
