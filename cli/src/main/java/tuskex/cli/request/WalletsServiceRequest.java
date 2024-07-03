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

package tuskex.cli.request;

import tuskex.cli.GrpcStubs;
import tuskex.proto.grpc.AddressBalanceInfo;
import tuskex.proto.grpc.BalancesInfo;
import tuskex.proto.grpc.BtcBalanceInfo;
import tuskex.proto.grpc.GetAddressBalanceRequest;
import tuskex.proto.grpc.GetBalancesRequest;
import tuskex.proto.grpc.GetFundingAddressesRequest;
import tuskex.proto.grpc.LockWalletRequest;
import tuskex.proto.grpc.MarketPriceRequest;
import tuskex.proto.grpc.RemoveWalletPasswordRequest;
import tuskex.proto.grpc.SetWalletPasswordRequest;
import tuskex.proto.grpc.UnlockWalletRequest;

import java.util.List;

public class WalletsServiceRequest {

    private final GrpcStubs grpcStubs;

    public WalletsServiceRequest(GrpcStubs grpcStubs) {
        this.grpcStubs = grpcStubs;
    }

    public BalancesInfo getBalances() {
        return getBalances("");
    }

    public BtcBalanceInfo getBtcBalances() {
        return getBalances("BTC").getBtc();
    }

    public BalancesInfo getBalances(String currencyCode) {
        var request = GetBalancesRequest.newBuilder()
                .setCurrencyCode(currencyCode)
                .build();
        return grpcStubs.walletsService.getBalances(request).getBalances();
    }

    public AddressBalanceInfo getAddressBalance(String address) {
        var request = GetAddressBalanceRequest.newBuilder()
                .setAddress(address).build();
        return grpcStubs.walletsService.getAddressBalance(request).getAddressBalanceInfo();
    }

    public double getBtcPrice(String currencyCode) {
        var request = MarketPriceRequest.newBuilder()
                .setCurrencyCode(currencyCode)
                .build();
        return grpcStubs.priceService.getMarketPrice(request).getPrice();
    }

    public List<AddressBalanceInfo> getFundingAddresses() {
        var request = GetFundingAddressesRequest.newBuilder().build();
        return grpcStubs.walletsService.getFundingAddresses(request).getAddressBalanceInfoList();
    }

    public String getUnusedBtcAddress() {
        var request = GetFundingAddressesRequest.newBuilder().build();
        var addressBalances = grpcStubs.walletsService.getFundingAddresses(request)
                .getAddressBalanceInfoList();
        //noinspection OptionalGetWithoutIsPresent
        return addressBalances.stream()
                .filter(AddressBalanceInfo::getIsAddressUnused)
                .findFirst()
                .get()
                .getAddress();
    }

    public void lockWallet() {
        var request = LockWalletRequest.newBuilder().build();
        //noinspection ResultOfMethodCallIgnored
        grpcStubs.walletsService.lockWallet(request);
    }

    public void unlockWallet(String walletPassword, long timeout) {
        var request = UnlockWalletRequest.newBuilder()
                .setPassword(walletPassword)
                .setTimeout(timeout).build();
        //noinspection ResultOfMethodCallIgnored
        grpcStubs.walletsService.unlockWallet(request);
    }

    public void removeWalletPassword(String walletPassword) {
        var request = RemoveWalletPasswordRequest.newBuilder()
                .setPassword(walletPassword).build();
        //noinspection ResultOfMethodCallIgnored
        grpcStubs.walletsService.removeWalletPassword(request);
    }

    public void setWalletPassword(String walletPassword) {
        var request = SetWalletPasswordRequest.newBuilder()
                .setPassword(walletPassword).build();
        //noinspection ResultOfMethodCallIgnored
        grpcStubs.walletsService.setWalletPassword(request);
    }

    public void setWalletPassword(String oldWalletPassword, String newWalletPassword) {
        var request = SetWalletPasswordRequest.newBuilder()
                .setPassword(oldWalletPassword)
                .setNewPassword(newWalletPassword).build();
        //noinspection ResultOfMethodCallIgnored
        grpcStubs.walletsService.setWalletPassword(request);
    }
}
