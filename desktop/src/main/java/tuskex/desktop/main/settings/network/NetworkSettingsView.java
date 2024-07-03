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

package tuskex.desktop.main.settings.network;

import com.google.inject.Inject;
import tuskex.common.ClockWatcher;
import tuskex.common.UserThread;
import tuskex.core.api.TskConnectionService;
import tuskex.core.api.TskLocalNode;
import tuskex.core.filter.Filter;
import tuskex.core.filter.FilterManager;
import tuskex.core.locale.Res;
import tuskex.core.trade.TuskexUtils;
import tuskex.core.user.Preferences;
import tuskex.core.util.FormattingUtils;
import tuskex.core.util.validation.RegexValidator;
import tuskex.core.util.validation.RegexValidatorFactory;
import tuskex.core.tsk.nodes.TskNodes;
import tuskex.core.tsk.setup.WalletsSetup;
import tuskex.desktop.app.TuskexApp;
import tuskex.desktop.common.view.ActivatableView;
import tuskex.desktop.common.view.FxmlView;
import tuskex.desktop.components.AutoTooltipButton;
import tuskex.desktop.components.AutoTooltipLabel;
import tuskex.desktop.components.InputTextField;
import tuskex.desktop.components.TitledGroupBg;
import tuskex.desktop.main.overlays.popups.Popup;
import tuskex.desktop.main.overlays.windows.TorNetworkSettingsWindow;
import tuskex.desktop.util.GUIUtil;
import tuskex.network.p2p.P2PService;
import tuskex.network.p2p.network.Statistic;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static javafx.beans.binding.Bindings.createStringBinding;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import monero.daemon.model.MoneroPeer;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@FxmlView
public class NetworkSettingsView extends ActivatableView<GridPane, Void> {

    @FXML
    TitledGroupBg p2pHeader, btcHeader;
    @FXML
    Label useTorForTskLabel, tskNodesLabel, moneroNodesLabel, localhostTskNodeInfoLabel;
    @FXML
    InputTextField tskNodesInputTextField;
    @FXML
    TextField onionAddress, sentDataTextField, receivedDataTextField, chainHeightTextField;
    @FXML
    Label p2PPeersLabel, moneroPeersLabel;
    @FXML
    RadioButton useTorForTskAfterSyncRadio, useTorForTskOffRadio, useTorForTskOnRadio;
    @FXML
    RadioButton useProvidedNodesRadio, useCustomNodesRadio, usePublicNodesRadio;
    @FXML
    TableView<P2pNetworkListItem> p2pPeersTableView;
    @FXML
    TableView<MoneroNetworkListItem> moneroPeersTableView;
    @FXML
    TableColumn<P2pNetworkListItem, String> onionAddressColumn, connectionTypeColumn, creationDateColumn,
            roundTripTimeColumn, sentBytesColumn, receivedBytesColumn, peerTypeColumn;
    @FXML
    TableColumn<MoneroNetworkListItem, String> moneroPeerAddressColumn, moneroPeerVersionColumn,
            moneroPeerSubVersionColumn, moneroPeerHeightColumn;
    @FXML
    Label rescanOutputsLabel;
    @FXML
    AutoTooltipButton rescanOutputsButton, openTorSettingsButton;

    private final Preferences preferences;
    private final TskNodes tskNodes;
    private final FilterManager filterManager;
    private final TskLocalNode tskLocalNode;
    private final TorNetworkSettingsWindow torNetworkSettingsWindow;
    private final ClockWatcher clockWatcher;
    private final WalletsSetup walletsSetup;
    private final P2PService p2PService;
    private final TskConnectionService connectionService;

    private final ObservableList<P2pNetworkListItem> p2pNetworkListItems = FXCollections.observableArrayList();
    private final SortedList<P2pNetworkListItem> p2pSortedList = new SortedList<>(p2pNetworkListItems);

    private final ObservableList<MoneroNetworkListItem> moneroNetworkListItems = FXCollections.observableArrayList();
    private final SortedList<MoneroNetworkListItem> moneroSortedList = new SortedList<>(moneroNetworkListItems);

    private Subscription numP2PPeersSubscription;
    private Subscription moneroPeersSubscription;
    private Subscription moneroBlockHeightSubscription;
    private Subscription nodeAddressSubscription;
    private ChangeListener<Boolean> tskNodesInputTextFieldFocusListener;
    private ToggleGroup useTorForTskToggleGroup;
    private ToggleGroup moneroPeersToggleGroup;
    private Preferences.UseTorForTsk selectedUseTorForTsk;
    private TskNodes.MoneroNodesOption selectedMoneroNodesOption;
    private ChangeListener<Toggle> useTorForTskToggleGroupListener;
    private ChangeListener<Toggle> moneroPeersToggleGroupListener;
    private ChangeListener<Filter> filterPropertyListener;

    @Inject
    public NetworkSettingsView(WalletsSetup walletsSetup,
                               P2PService p2PService,
                               TskConnectionService connectionService,
                               Preferences preferences,
                               TskNodes tskNodes,
                               FilterManager filterManager,
                               TskLocalNode tskLocalNode,
                               TorNetworkSettingsWindow torNetworkSettingsWindow,
                               ClockWatcher clockWatcher) {
        super();
        this.walletsSetup = walletsSetup;
        this.p2PService = p2PService;
        this.connectionService = connectionService;
        this.preferences = preferences;
        this.tskNodes = tskNodes;
        this.filterManager = filterManager;
        this.tskLocalNode = tskLocalNode;
        this.torNetworkSettingsWindow = torNetworkSettingsWindow;
        this.clockWatcher = clockWatcher;
    }

    @Override
    public void initialize() {
        btcHeader.setText(Res.get("settings.net.tskHeader"));
        p2pHeader.setText(Res.get("settings.net.p2pHeader"));
        onionAddress.setPromptText(Res.get("settings.net.onionAddressLabel"));
        tskNodesLabel.setText(Res.get("settings.net.tskNodesLabel"));
        moneroPeersLabel.setText(Res.get("settings.net.moneroPeersLabel"));
        useTorForTskLabel.setText(Res.get("settings.net.useTorForTskJLabel"));
        useTorForTskAfterSyncRadio.setText(Res.get("settings.net.useTorForTskAfterSyncRadio"));
        useTorForTskOffRadio.setText(Res.get("settings.net.useTorForTskOffRadio"));
        useTorForTskOnRadio.setText(Res.get("settings.net.useTorForTskOnRadio"));
        moneroNodesLabel.setText(Res.get("settings.net.moneroNodesLabel"));
        moneroPeerAddressColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.onionAddressColumn")));
        moneroPeerAddressColumn.getStyleClass().add("first-column");
        moneroPeerVersionColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.versionColumn")));
        moneroPeerSubVersionColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.subVersionColumn")));
        moneroPeerHeightColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.heightColumn")));
        localhostTskNodeInfoLabel.setText(Res.get("settings.net.localhostTskNodeInfo"));
        useProvidedNodesRadio.setText(Res.get("settings.net.useProvidedNodesRadio"));
        useCustomNodesRadio.setText(Res.get("settings.net.useCustomNodesRadio"));
        usePublicNodesRadio.setText(Res.get("settings.net.usePublicNodesRadio"));
        rescanOutputsLabel.setText(Res.get("settings.net.rescanOutputsLabel"));
        rescanOutputsButton.updateText(Res.get("settings.net.rescanOutputsButton"));
        p2PPeersLabel.setText(Res.get("settings.net.p2PPeersLabel"));
        onionAddressColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.onionAddressColumn")));
        onionAddressColumn.getStyleClass().add("first-column");
        creationDateColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.creationDateColumn")));
        connectionTypeColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.connectionTypeColumn")));
        sentDataTextField.setPromptText(Res.get("settings.net.sentDataLabel"));
        receivedDataTextField.setPromptText(Res.get("settings.net.receivedDataLabel"));
        chainHeightTextField.setPromptText(Res.get("settings.net.chainHeightLabel"));
        roundTripTimeColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.roundTripTimeColumn")));
        sentBytesColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.sentBytesColumn")));
        receivedBytesColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.receivedBytesColumn")));
        peerTypeColumn.setGraphic(new AutoTooltipLabel(Res.get("settings.net.peerTypeColumn")));
        peerTypeColumn.getStyleClass().add("last-column");
        openTorSettingsButton.updateText(Res.get("settings.net.openTorSettingsButton"));

        // TODO: hiding button to rescan outputs until supported
        rescanOutputsLabel.setVisible(false);
        rescanOutputsButton.setVisible(false);

        GridPane.setMargin(moneroPeersLabel, new Insets(4, 0, 0, 0));
        GridPane.setValignment(moneroPeersLabel, VPos.TOP);

        GridPane.setMargin(p2PPeersLabel, new Insets(4, 0, 0, 0));
        GridPane.setValignment(p2PPeersLabel, VPos.TOP);

        moneroPeersTableView.setMinHeight(180);
        moneroPeersTableView.setPrefHeight(180);
        moneroPeersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        moneroPeersTableView.setPlaceholder(new AutoTooltipLabel(Res.get("table.placeholder.noData")));
        moneroPeersTableView.getSortOrder().add(moneroPeerAddressColumn);
        moneroPeerAddressColumn.setSortType(TableColumn.SortType.ASCENDING);


        p2pPeersTableView.setMinHeight(180);
        p2pPeersTableView.setPrefHeight(180);
        p2pPeersTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        p2pPeersTableView.setPlaceholder(new AutoTooltipLabel(Res.get("table.placeholder.noData")));
        p2pPeersTableView.getSortOrder().add(creationDateColumn);
        creationDateColumn.setSortType(TableColumn.SortType.ASCENDING);

        // use tor for tsk radio buttons

        useTorForTskToggleGroup = new ToggleGroup();
        useTorForTskAfterSyncRadio.setToggleGroup(useTorForTskToggleGroup);
        useTorForTskOffRadio.setToggleGroup(useTorForTskToggleGroup);
        useTorForTskOnRadio.setToggleGroup(useTorForTskToggleGroup);

        useTorForTskAfterSyncRadio.setUserData(Preferences.UseTorForTsk.AFTER_SYNC);
        useTorForTskOffRadio.setUserData(Preferences.UseTorForTsk.OFF);
        useTorForTskOnRadio.setUserData(Preferences.UseTorForTsk.ON);

        selectedUseTorForTsk = Preferences.UseTorForTsk.values()[preferences.getUseTorForTskOrdinal()];

        selectUseTorForTskToggle();
        onUseTorForTskToggleSelected(false);

        useTorForTskToggleGroupListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedUseTorForTsk = (Preferences.UseTorForTsk) newValue.getUserData();
                onUseTorForTskToggleSelected(true);
            }
        };

        // monero nodes radio buttons

        moneroPeersToggleGroup = new ToggleGroup();
        useProvidedNodesRadio.setToggleGroup(moneroPeersToggleGroup);
        useCustomNodesRadio.setToggleGroup(moneroPeersToggleGroup);
        usePublicNodesRadio.setToggleGroup(moneroPeersToggleGroup);

        useProvidedNodesRadio.setUserData(TskNodes.MoneroNodesOption.PROVIDED);
        useCustomNodesRadio.setUserData(TskNodes.MoneroNodesOption.CUSTOM);
        usePublicNodesRadio.setUserData(TskNodes.MoneroNodesOption.PUBLIC);

        selectedMoneroNodesOption = TskNodes.MoneroNodesOption.values()[preferences.getMoneroNodesOptionOrdinal()];
        // In case CUSTOM is selected but no custom nodes are set or
        // in case PUBLIC is selected but we blocked it (B2X risk) we revert to provided nodes
        if ((selectedMoneroNodesOption == TskNodes.MoneroNodesOption.CUSTOM &&
                (preferences.getMoneroNodes() == null || preferences.getMoneroNodes().isEmpty())) ||
                (selectedMoneroNodesOption == TskNodes.MoneroNodesOption.PUBLIC && isPreventPublicTskNetwork())) {
            selectedMoneroNodesOption = TskNodes.MoneroNodesOption.PROVIDED;
            preferences.setMoneroNodesOptionOrdinal(selectedMoneroNodesOption.ordinal());
        }

        selectMoneroPeersToggle();
        onMoneroPeersToggleSelected(false);

        moneroPeersToggleGroupListener = (observable, oldValue, newValue) -> {
            if (newValue != null) {
                selectedMoneroNodesOption = (TskNodes.MoneroNodesOption) newValue.getUserData();
                onMoneroPeersToggleSelected(true);
            }
        };

        tskNodesInputTextField.setPromptText(Res.get("settings.net.ips", "" + TuskexUtils.getDefaultMoneroPort()));
        RegexValidator regexValidator = RegexValidatorFactory.addressRegexValidator();
        tskNodesInputTextField.setValidator(regexValidator);
        tskNodesInputTextField.setErrorMessage(Res.get("validation.invalidAddressList"));
        tskNodesInputTextFieldFocusListener = (observable, oldValue, newValue) -> {
            if (oldValue && !newValue
                    && !tskNodesInputTextField.getText().equals(preferences.getMoneroNodes())
                    && tskNodesInputTextField.validate()) {
                preferences.setMoneroNodes(tskNodesInputTextField.getText());
                preferences.setMoneroNodesOptionOrdinal(selectedMoneroNodesOption.ordinal());
                showShutDownPopup();
            }
        };
        filterPropertyListener = (observable, oldValue, newValue) -> applyPreventPublicTskNetwork();

        //TODO sorting needs other NetworkStatisticListItem as columns type
       /* creationDateColumn.setComparator((o1, o2) ->
                o1.statistic.getCreationDate().compareTo(o2.statistic.getCreationDate()));
        sentBytesColumn.setComparator((o1, o2) ->
                ((Integer) o1.statistic.getSentBytes()).compareTo(((Integer) o2.statistic.getSentBytes())));
        receivedBytesColumn.setComparator((o1, o2) ->
                ((Integer) o1.statistic.getReceivedBytes()).compareTo(((Integer) o2.statistic.getReceivedBytes())));*/
    }

    @Override
    public void activate() {
        useTorForTskToggleGroup.selectedToggleProperty().addListener(useTorForTskToggleGroupListener);
        moneroPeersToggleGroup.selectedToggleProperty().addListener(moneroPeersToggleGroupListener);

        if (filterManager.getFilter() != null)
            applyPreventPublicTskNetwork();

        filterManager.filterProperty().addListener(filterPropertyListener);

        rescanOutputsButton.setOnAction(event -> GUIUtil.rescanOutputs(preferences));

        moneroPeersSubscription = EasyBind.subscribe(connectionService.peerConnectionsProperty(),
                this::updateMoneroPeersTable);

        moneroBlockHeightSubscription = EasyBind.subscribe(connectionService.chainHeightProperty(),
                this::updateChainHeightTextField);

        nodeAddressSubscription = EasyBind.subscribe(p2PService.getNetworkNode().nodeAddressProperty(),
                nodeAddress -> onionAddress.setText(nodeAddress == null ?
                        Res.get("settings.net.notKnownYet") :
                        nodeAddress.getFullAddress()));
        numP2PPeersSubscription = EasyBind.subscribe(p2PService.getNumConnectedPeers(), numPeers -> updateP2PTable());

        sentDataTextField.textProperty().bind(createStringBinding(() -> Res.get("settings.net.sentData",
                FormattingUtils.formatBytes(Statistic.totalSentBytesProperty().get()),
                Statistic.numTotalSentMessagesProperty().get(),
                Statistic.numTotalSentMessagesPerSecProperty().get()),
                Statistic.numTotalSentMessagesPerSecProperty()));

        receivedDataTextField.textProperty().bind(createStringBinding(() -> Res.get("settings.net.receivedData",
                FormattingUtils.formatBytes(Statistic.totalReceivedBytesProperty().get()),
                Statistic.numTotalReceivedMessagesProperty().get(),
                Statistic.numTotalReceivedMessagesPerSecProperty().get()),
                Statistic.numTotalReceivedMessagesPerSecProperty()));

        moneroSortedList.comparatorProperty().bind(moneroPeersTableView.comparatorProperty());
        moneroPeersTableView.setItems(moneroSortedList);

        p2pSortedList.comparatorProperty().bind(p2pPeersTableView.comparatorProperty());
        p2pPeersTableView.setItems(p2pSortedList);

        tskNodesInputTextField.setText(preferences.getMoneroNodes());

        tskNodesInputTextField.focusedProperty().addListener(tskNodesInputTextFieldFocusListener);

        openTorSettingsButton.setOnAction(e -> torNetworkSettingsWindow.show());
    }

    @Override
    public void deactivate() {
        useTorForTskToggleGroup.selectedToggleProperty().removeListener(useTorForTskToggleGroupListener);
        moneroPeersToggleGroup.selectedToggleProperty().removeListener(moneroPeersToggleGroupListener);
        filterManager.filterProperty().removeListener(filterPropertyListener);

        if (nodeAddressSubscription != null)
            nodeAddressSubscription.unsubscribe();

        if (moneroPeersSubscription != null)
            moneroPeersSubscription.unsubscribe();

        if (moneroBlockHeightSubscription != null)
            moneroBlockHeightSubscription.unsubscribe();

        if (numP2PPeersSubscription != null)
            numP2PPeersSubscription.unsubscribe();

        sentDataTextField.textProperty().unbind();
        receivedDataTextField.textProperty().unbind();

        moneroSortedList.comparatorProperty().unbind();
        p2pSortedList.comparatorProperty().unbind();
        p2pPeersTableView.getItems().forEach(P2pNetworkListItem::cleanup);
        tskNodesInputTextField.focusedProperty().removeListener(tskNodesInputTextFieldFocusListener);

        openTorSettingsButton.setOnAction(null);
    }

    private boolean isPreventPublicTskNetwork() {
       return filterManager.getFilter() != null &&
               filterManager.getFilter().isPreventPublicTskNetwork();
    }

    private void selectUseTorForTskToggle() {
        switch (selectedUseTorForTsk) {
            case OFF:
                useTorForTskToggleGroup.selectToggle(useTorForTskOffRadio);
                break;
            case ON:
                useTorForTskToggleGroup.selectToggle(useTorForTskOnRadio);
                break;
            default:
            case AFTER_SYNC:
                useTorForTskToggleGroup.selectToggle(useTorForTskAfterSyncRadio);
                break;
        }
    }

    private void selectMoneroPeersToggle() {
        switch (selectedMoneroNodesOption) {
            case CUSTOM:
                moneroPeersToggleGroup.selectToggle(useCustomNodesRadio);
                break;
            case PUBLIC:
                moneroPeersToggleGroup.selectToggle(usePublicNodesRadio);
                break;
            default:
            case PROVIDED:
                moneroPeersToggleGroup.selectToggle(useProvidedNodesRadio);
                break;
        }
    }

    private void showShutDownPopup() {
        new Popup()
                .information(Res.get("settings.net.needRestart"))
                .closeButtonText(Res.get("shared.cancel"))
                .useShutDownButton()
                .show();
    }

    private void onUseTorForTskToggleSelected(boolean calledFromUser) {
        Preferences.UseTorForTsk currentUseTorForTsk = Preferences.UseTorForTsk.values()[preferences.getUseTorForTskOrdinal()];
        if (currentUseTorForTsk != selectedUseTorForTsk) {
            if (calledFromUser) {
                new Popup().information(Res.get("settings.net.needRestart"))
                    .actionButtonText(Res.get("shared.applyAndShutDown"))
                    .onAction(() -> {
                        preferences.setUseTorForTskOrdinal(selectedUseTorForTsk.ordinal());
                        UserThread.runAfter(TuskexApp.getShutDownHandler(), 500, TimeUnit.MILLISECONDS);
                    })
                    .closeButtonText(Res.get("shared.cancel"))
                    .onClose(() -> {
                        selectedUseTorForTsk = currentUseTorForTsk;
                        selectUseTorForTskToggle();
                    })
                    .show();
            }
        }
    }

    private void onMoneroPeersToggleSelected(boolean calledFromUser) {
        usePublicNodesRadio.setDisable(isPreventPublicTskNetwork());

        TskNodes.MoneroNodesOption currentMoneroNodesOption = TskNodes.MoneroNodesOption.values()[preferences.getMoneroNodesOptionOrdinal()];

        switch (selectedMoneroNodesOption) {
            case CUSTOM:
                tskNodesInputTextField.setDisable(false);
                tskNodesLabel.setDisable(false);
                if (!tskNodesInputTextField.getText().isEmpty()
                        && tskNodesInputTextField.validate()
                        && currentMoneroNodesOption != TskNodes.MoneroNodesOption.CUSTOM) {
                    preferences.setMoneroNodesOptionOrdinal(selectedMoneroNodesOption.ordinal());
                    if (calledFromUser) {
                        if (isPreventPublicTskNetwork()) {
                            new Popup().warning(Res.get("settings.net.warn.useCustomNodes.B2XWarning"))
                                    .onAction(() -> UserThread.runAfter(this::showShutDownPopup, 300, TimeUnit.MILLISECONDS)).show();
                        } else {
                            showShutDownPopup();
                        }
                    }
                }
                break;
            case PUBLIC:
                tskNodesInputTextField.setDisable(true);
                tskNodesLabel.setDisable(true);
                if (currentMoneroNodesOption != TskNodes.MoneroNodesOption.PUBLIC) {
                    preferences.setMoneroNodesOptionOrdinal(selectedMoneroNodesOption.ordinal());
                    if (calledFromUser) {
                        new Popup()
                                .warning(Res.get("settings.net.warn.usePublicNodes"))
                                .actionButtonText(Res.get("settings.net.warn.usePublicNodes.useProvided"))
                                .onAction(() -> UserThread.runAfter(() -> {
                                    selectedMoneroNodesOption = TskNodes.MoneroNodesOption.PROVIDED;
                                    preferences.setMoneroNodesOptionOrdinal(selectedMoneroNodesOption.ordinal());
                                    selectMoneroPeersToggle();
                                    onMoneroPeersToggleSelected(false);
                                }, 300, TimeUnit.MILLISECONDS))
                                .closeButtonText(Res.get("settings.net.warn.usePublicNodes.usePublic"))
                                .onClose(() -> UserThread.runAfter(this::showShutDownPopup, 300, TimeUnit.MILLISECONDS))
                                .show();
                    }
                }
                break;
            default:
            case PROVIDED:
                tskNodesInputTextField.setDisable(true);
                tskNodesLabel.setDisable(true);
                if (currentMoneroNodesOption != TskNodes.MoneroNodesOption.PROVIDED) {
                    preferences.setMoneroNodesOptionOrdinal(selectedMoneroNodesOption.ordinal());
                    if (calledFromUser) {
                        showShutDownPopup();
                    }
                }
                break;
        }
    }


    private void applyPreventPublicTskNetwork() {
        final boolean preventPublicTskNetwork = isPreventPublicTskNetwork();
        usePublicNodesRadio.setDisable(tskLocalNode.shouldBeUsed() || preventPublicTskNetwork);
        if (preventPublicTskNetwork && selectedMoneroNodesOption == TskNodes.MoneroNodesOption.PUBLIC) {
            selectedMoneroNodesOption = TskNodes.MoneroNodesOption.PROVIDED;
            preferences.setMoneroNodesOptionOrdinal(selectedMoneroNodesOption.ordinal());
            selectMoneroPeersToggle();
            onMoneroPeersToggleSelected(false);
        }
    }

    private void updateP2PTable() {
        if (connectionService.isShutDownStarted()) return; // ignore if shutting down
        p2pPeersTableView.getItems().forEach(P2pNetworkListItem::cleanup);
        p2pNetworkListItems.clear();
        p2pNetworkListItems.setAll(p2PService.getNetworkNode().getAllConnections().stream()
                .map(connection -> new P2pNetworkListItem(connection, clockWatcher))
                .collect(Collectors.toList()));
    }

    private void updateMoneroPeersTable(List<MoneroPeer> peers) {
        moneroNetworkListItems.clear();
        if (peers != null) {
            moneroNetworkListItems.setAll(peers.stream()
                    .map(MoneroNetworkListItem::new)
                    .collect(Collectors.toList()));
        }
    }

    private void updateChainHeightTextField(Number chainHeight) {
        chainHeightTextField.textProperty().setValue(Res.get("settings.net.chainHeight", chainHeight));
    }
}

