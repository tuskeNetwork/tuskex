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

/**
 * Tuskex's family of abstractions representing different ("crypto")
 * {@link tuskex.asset.Asset} types such as {@link tuskex.asset.Coin},
 * {@link tuskex.asset.Token} and {@link tuskex.asset.Erc20Token}, as well as concrete
 * implementations of each, such as {@link tuskex.asset.coins.Bitcoin} itself, cryptos like
 * {@link tuskex.asset.coins.Litecoin} and {@link tuskex.asset.coins.Ether} and tokens like
 * {@link tuskex.asset.tokens.DaiStablecoin}.
 * <p>
 * The purpose of this package is to provide everything necessary for registering
 * ("listing") new assets and managing / accessing those assets within, e.g. the Tuskex
 * Desktop UI.
 * <p>
 * Note that everything within this package is intentionally designed to be simple and
 * low-level with no dependencies on any other Tuskex packages or components.
 *
 * @author Chris Beams
 * @since 0.7.0
 */

package tuskex.asset;
