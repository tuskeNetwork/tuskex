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

package tuskex.core.proto;

import tuskex.common.proto.ProtoResolver;
import tuskex.common.proto.ProtobufferRuntimeException;
import tuskex.common.proto.persistable.PersistablePayload;
import tuskex.core.account.sign.SignedWitness;
import tuskex.core.account.witness.AccountAgeWitness;
import tuskex.core.payment.payload.AchTransferAccountPayload;
import tuskex.core.payment.payload.AdvancedCashAccountPayload;
import tuskex.core.payment.payload.AliPayAccountPayload;
import tuskex.core.payment.payload.AmazonGiftCardAccountPayload;
import tuskex.core.payment.payload.AustraliaPayidAccountPayload;
import tuskex.core.payment.payload.BizumAccountPayload;
import tuskex.core.payment.payload.CapitualAccountPayload;
import tuskex.core.payment.payload.CashAppAccountPayload;
import tuskex.core.payment.payload.CashAtAtmAccountPayload;
import tuskex.core.payment.payload.PayByMailAccountPayload;
import tuskex.core.payment.payload.CashDepositAccountPayload;
import tuskex.core.payment.payload.CelPayAccountPayload;
import tuskex.core.payment.payload.ChaseQuickPayAccountPayload;
import tuskex.core.payment.payload.ZelleAccountPayload;
import tuskex.core.payment.payload.CryptoCurrencyAccountPayload;
import tuskex.core.payment.payload.DomesticWireTransferAccountPayload;
import tuskex.core.payment.payload.F2FAccountPayload;
import tuskex.core.payment.payload.FasterPaymentsAccountPayload;
import tuskex.core.payment.payload.HalCashAccountPayload;
import tuskex.core.payment.payload.ImpsAccountPayload;
import tuskex.core.payment.payload.InstantCryptoCurrencyPayload;
import tuskex.core.payment.payload.InteracETransferAccountPayload;
import tuskex.core.payment.payload.JapanBankAccountPayload;
import tuskex.core.payment.payload.MoneseAccountPayload;
import tuskex.core.payment.payload.MoneyBeamAccountPayload;
import tuskex.core.payment.payload.MoneyGramAccountPayload;
import tuskex.core.payment.payload.NationalBankAccountPayload;
import tuskex.core.payment.payload.NeftAccountPayload;
import tuskex.core.payment.payload.NequiAccountPayload;
import tuskex.core.payment.payload.OKPayAccountPayload;
import tuskex.core.payment.payload.PaxumAccountPayload;
import tuskex.core.payment.payload.PaymentAccountPayload;
import tuskex.core.payment.payload.PayPalAccountPayload;
import tuskex.core.payment.payload.PayseraAccountPayload;
import tuskex.core.payment.payload.PaytmAccountPayload;
import tuskex.core.payment.payload.PerfectMoneyAccountPayload;
import tuskex.core.payment.payload.PixAccountPayload;
import tuskex.core.payment.payload.PopmoneyAccountPayload;
import tuskex.core.payment.payload.PromptPayAccountPayload;
import tuskex.core.payment.payload.RevolutAccountPayload;
import tuskex.core.payment.payload.RtgsAccountPayload;
import tuskex.core.payment.payload.SameBankAccountPayload;
import tuskex.core.payment.payload.SatispayAccountPayload;
import tuskex.core.payment.payload.SepaAccountPayload;
import tuskex.core.payment.payload.SepaInstantAccountPayload;
import tuskex.core.payment.payload.SpecificBanksAccountPayload;
import tuskex.core.payment.payload.StrikeAccountPayload;
import tuskex.core.payment.payload.SwiftAccountPayload;
import tuskex.core.payment.payload.SwishAccountPayload;
import tuskex.core.payment.payload.TikkieAccountPayload;
import tuskex.core.payment.payload.TransferwiseAccountPayload;
import tuskex.core.payment.payload.TransferwiseUsdAccountPayload;
import tuskex.core.payment.payload.USPostalMoneyOrderAccountPayload;
import tuskex.core.payment.payload.UpholdAccountPayload;
import tuskex.core.payment.payload.UpiAccountPayload;
import tuskex.core.payment.payload.VenmoAccountPayload;
import tuskex.core.payment.payload.VerseAccountPayload;
import tuskex.core.payment.payload.WeChatPayAccountPayload;
import tuskex.core.payment.payload.WesternUnionAccountPayload;
import tuskex.core.trade.statistics.TradeStatistics3;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;

@Slf4j
public class CoreProtoResolver implements ProtoResolver {
    @Getter
    protected Clock clock;

    @Override
    public PaymentAccountPayload fromProto(protobuf.PaymentAccountPayload proto) {
        if (proto != null) {
            final protobuf.PaymentAccountPayload.MessageCase messageCase = proto.getMessageCase();
            switch (messageCase) {
                case ALI_PAY_ACCOUNT_PAYLOAD:
                    return AliPayAccountPayload.fromProto(proto);
                case WE_CHAT_PAY_ACCOUNT_PAYLOAD:
                    return WeChatPayAccountPayload.fromProto(proto);
                case CHASE_QUICK_PAY_ACCOUNT_PAYLOAD:
                    return ChaseQuickPayAccountPayload.fromProto(proto);
                case ZELLE_ACCOUNT_PAYLOAD:
                    return ZelleAccountPayload.fromProto(proto);
                case COUNTRY_BASED_PAYMENT_ACCOUNT_PAYLOAD:
                    final protobuf.CountryBasedPaymentAccountPayload.MessageCase messageCaseCountry = proto.getCountryBasedPaymentAccountPayload().getMessageCase();
                    switch (messageCaseCountry) {
                        case BANK_ACCOUNT_PAYLOAD:
                            final protobuf.BankAccountPayload.MessageCase messageCaseBank = proto.getCountryBasedPaymentAccountPayload().getBankAccountPayload().getMessageCase();
                            switch (messageCaseBank) {
                                case NATIONAL_BANK_ACCOUNT_PAYLOAD:
                                    return NationalBankAccountPayload.fromProto(proto);
                                case SAME_BANK_ACCONT_PAYLOAD:
                                    return SameBankAccountPayload.fromProto(proto);
                                case SPECIFIC_BANKS_ACCOUNT_PAYLOAD:
                                    return SpecificBanksAccountPayload.fromProto(proto);
                                case ACH_TRANSFER_ACCOUNT_PAYLOAD:
                                    return AchTransferAccountPayload.fromProto(proto);
                                case DOMESTIC_WIRE_TRANSFER_ACCOUNT_PAYLOAD:
                                    return DomesticWireTransferAccountPayload.fromProto(proto);
                                default:
                                    throw new ProtobufferRuntimeException("Unknown proto message case" +
                                            "(PB.PaymentAccountPayload.CountryBasedPaymentAccountPayload.BankAccountPayload). " +
                                            "messageCase=" + messageCaseBank);
                            }
                        case WESTERN_UNION_ACCOUNT_PAYLOAD:
                            return WesternUnionAccountPayload.fromProto(proto);
                        case CASH_DEPOSIT_ACCOUNT_PAYLOAD:
                            return CashDepositAccountPayload.fromProto(proto);
                        case SEPA_ACCOUNT_PAYLOAD:
                            return SepaAccountPayload.fromProto(proto);
                        case SEPA_INSTANT_ACCOUNT_PAYLOAD:
                            return SepaInstantAccountPayload.fromProto(proto);
                        case F2F_ACCOUNT_PAYLOAD:
                            return F2FAccountPayload.fromProto(proto);
                        case UPI_ACCOUNT_PAYLOAD:
                            return UpiAccountPayload.fromProto(proto);
                        case PAYTM_ACCOUNT_PAYLOAD:
                            return PaytmAccountPayload.fromProto(proto);
                        case NEQUI_ACCOUNT_PAYLOAD:
                            return NequiAccountPayload.fromProto(proto);
                        case BIZUM_ACCOUNT_PAYLOAD:
                            return BizumAccountPayload.fromProto(proto);
                        case PIX_ACCOUNT_PAYLOAD:
                            return PixAccountPayload.fromProto(proto);
                        case SATISPAY_ACCOUNT_PAYLOAD:
                            return SatispayAccountPayload.fromProto(proto);
                        case TIKKIE_ACCOUNT_PAYLOAD:
                            return TikkieAccountPayload.fromProto(proto);
                        case STRIKE_ACCOUNT_PAYLOAD:
                            return StrikeAccountPayload.fromProto(proto);
                        case TRANSFERWISE_USD_ACCOUNT_PAYLOAD:
                            return TransferwiseUsdAccountPayload.fromProto(proto);
                        case IFSC_BASED_ACCOUNT_PAYLOAD:
                            final protobuf.IfscBasedAccountPayload.MessageCase messageCaseIfsc = proto.getCountryBasedPaymentAccountPayload().getIfscBasedAccountPayload().getMessageCase();
                            switch (messageCaseIfsc) {
                                case NEFT_ACCOUNT_PAYLOAD:
                                    return NeftAccountPayload.fromProto(proto);
                                case RTGS_ACCOUNT_PAYLOAD:
                                    return RtgsAccountPayload.fromProto(proto);
                                case IMPS_ACCOUNT_PAYLOAD:
                                    return ImpsAccountPayload.fromProto(proto);
                                default:
                                    throw new ProtobufferRuntimeException("Unknown proto message case" +
                                            "(PB.PaymentAccountPayload.CountryBasedPaymentAccountPayload.IfscBasedPaymentAccount). " +
                                            "messageCase=" + messageCaseIfsc);
                            }
                        default:
                            throw new ProtobufferRuntimeException("Unknown proto message case" +
                                    "(PB.PaymentAccountPayload.CountryBasedPaymentAccountPayload)." +
                                    " messageCase=" + messageCaseCountry);
                    }
                case CRYPTO_CURRENCY_ACCOUNT_PAYLOAD:
                    return CryptoCurrencyAccountPayload.fromProto(proto);
                case FASTER_PAYMENTS_ACCOUNT_PAYLOAD:
                    return FasterPaymentsAccountPayload.fromProto(proto);
                case INTERAC_E_TRANSFER_ACCOUNT_PAYLOAD:
                    return InteracETransferAccountPayload.fromProto(proto);
                case JAPAN_BANK_ACCOUNT_PAYLOAD:
                    return JapanBankAccountPayload.fromProto(proto);
                case AUSTRALIA_PAYID_PAYLOAD:
                    return AustraliaPayidAccountPayload.fromProto(proto);
                case UPHOLD_ACCOUNT_PAYLOAD:
                    return UpholdAccountPayload.fromProto(proto);
                case MONEY_BEAM_ACCOUNT_PAYLOAD:
                    return MoneyBeamAccountPayload.fromProto(proto);
                case MONEY_GRAM_ACCOUNT_PAYLOAD:
                    return MoneyGramAccountPayload.fromProto(proto);
                case POPMONEY_ACCOUNT_PAYLOAD:
                    return PopmoneyAccountPayload.fromProto(proto);
                case REVOLUT_ACCOUNT_PAYLOAD:
                    return RevolutAccountPayload.fromProto(proto);
                case PERFECT_MONEY_ACCOUNT_PAYLOAD:
                    return PerfectMoneyAccountPayload.fromProto(proto);
                case SWISH_ACCOUNT_PAYLOAD:
                    return SwishAccountPayload.fromProto(proto);
                case HAL_CASH_ACCOUNT_PAYLOAD:
                    return HalCashAccountPayload.fromProto(proto);
                case U_S_POSTAL_MONEY_ORDER_ACCOUNT_PAYLOAD:
                    return USPostalMoneyOrderAccountPayload.fromProto(proto);
                case PAY_BY_MAIL_ACCOUNT_PAYLOAD:
                    return PayByMailAccountPayload.fromProto(proto);
                case CASH_AT_ATM_ACCOUNT_PAYLOAD:
                    return CashAtAtmAccountPayload.fromProto(proto);
                case PROMPT_PAY_ACCOUNT_PAYLOAD:
                    return PromptPayAccountPayload.fromProto(proto);
                case ADVANCED_CASH_ACCOUNT_PAYLOAD:
                    return AdvancedCashAccountPayload.fromProto(proto);
                case TRANSFERWISE_ACCOUNT_PAYLOAD:
                    return TransferwiseAccountPayload.fromProto(proto);
                case PAYSERA_ACCOUNT_PAYLOAD:
                    return PayseraAccountPayload.fromProto(proto);
                case PAXUM_ACCOUNT_PAYLOAD:
                    return PaxumAccountPayload.fromProto(proto);
                case AMAZON_GIFT_CARD_ACCOUNT_PAYLOAD:
                    return AmazonGiftCardAccountPayload.fromProto(proto);
                case INSTANT_CRYPTO_CURRENCY_ACCOUNT_PAYLOAD:
                    return InstantCryptoCurrencyPayload.fromProto(proto);
                case CAPITUAL_ACCOUNT_PAYLOAD:
                    return CapitualAccountPayload.fromProto(proto);
                case CEL_PAY_ACCOUNT_PAYLOAD:
                    return CelPayAccountPayload.fromProto(proto);
                case MONESE_ACCOUNT_PAYLOAD:
                    return MoneseAccountPayload.fromProto(proto);
                case VERSE_ACCOUNT_PAYLOAD:
                    return VerseAccountPayload.fromProto(proto);
                case SWIFT_ACCOUNT_PAYLOAD:
                    return SwiftAccountPayload.fromProto(proto);

                // Cannot be deleted as it would break old trade history entries
                case O_K_PAY_ACCOUNT_PAYLOAD:
                    return OKPayAccountPayload.fromProto(proto);
                case CASH_APP_ACCOUNT_PAYLOAD:
                    return CashAppAccountPayload.fromProto(proto);
                case VENMO_ACCOUNT_PAYLOAD:
                    return VenmoAccountPayload.fromProto(proto);
                case PAYPAL_ACCOUNT_PAYLOAD:
                    return PayPalAccountPayload.fromProto(proto);

                default:
                    throw new ProtobufferRuntimeException("Unknown proto message case(PB.PaymentAccountPayload). messageCase=" + messageCase);
            }
        } else {
            log.error("PersistableEnvelope.fromProto: PB.PaymentAccountPayload is null");
            throw new ProtobufferRuntimeException("PB.PaymentAccountPayload is null");
        }
    }

    @Override
    public PersistablePayload fromProto(protobuf.PersistableNetworkPayload proto) {
        if (proto != null) {
            switch (proto.getMessageCase()) {
                case ACCOUNT_AGE_WITNESS:
                    return AccountAgeWitness.fromProto(proto.getAccountAgeWitness());
                case SIGNED_WITNESS:
                    return SignedWitness.fromProto(proto.getSignedWitness());
                case TRADE_STATISTICS3:
                    return TradeStatistics3.fromProto(proto.getTradeStatistics3());
                default:
                    throw new ProtobufferRuntimeException("Unknown proto message case (PB.PersistableNetworkPayload). messageCase=" + proto.getMessageCase());
            }
        } else {
            log.error("PB.PersistableNetworkPayload is null");
            throw new ProtobufferRuntimeException("PB.PersistableNetworkPayload is null");
        }
    }
}
