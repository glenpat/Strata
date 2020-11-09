package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.impl.tree.ConstantContinuousSingleBarrierKnockoutFunction;
import com.opengamma.strata.pricer.impl.tree.DigitalOptionFunction;
import com.opengamma.strata.pricer.impl.tree.EuropeanVanillaOptionFunction;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.fxopt.ResolvedFxDigitalOption;
import com.opengamma.strata.product.fxopt.ResolvedFxOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;

import java.util.Arrays;

import static com.opengamma.strata.pricer.fxopt.ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer.TREE;
import static com.opengamma.strata.pricer.fxopt.ImpliedTrinomialTreeFxSingleBarrierOptionProductPricer.validateData;

public final class TrinomialPricerExtensions {
  private TrinomialPricerExtensions() {
  }

  //  digital option
  static ValueDerivatives priceDerivatives(
      ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData data) {

    validate(option, ratesProvider, volatilities);
    validateData(option, ratesProvider, volatilities, data);

    int nSteps = data.getNumberOfSteps();

    double timeToExpiry = data.getTime(nSteps);

    if (option.getOptionType() == EtdOptionType.EUROPEAN) {
      DigitalOptionFunction digitalFunction = DigitalOptionFunction.of(
          option.getStrikePrice(), timeToExpiry, option.getBarrierType(), option.getKnockType(), nSteps);
      return TREE.optionPriceAdjoint(digitalFunction, data);

    } else {
      final CurrencyAmount payment = option.getPayment();
      final CurrencyAmount signedNotional = option.getSignedNotional();
      return priceDerivativesOneTouch(signedNotional, option.getIndex().getCurrencyPair().getCounter(),
          option.getBarrierType(), option.getKnockType(), option.getStrikePrice(),
          payment, data);
    }
  }

  private static ValueDerivatives priceDerivativesOneTouch(
      CurrencyAmount signedNotional,
      Currency ccyCounter,
      BarrierType barrierType,
      KnockType knockType,
      double barrierLevel,
      CurrencyAmount rebateCurrencyAmount,
      RecombiningTrinomialTreeData data) {

    int nSteps = data.getNumberOfSteps();

    double timeToExpiry = data.getTime(nSteps);

    double notional = signedNotional.getAmount();
    double[] rebateArray = new double[nSteps + 1];

    //  only implemented for rebate in counter ccy
    if (!rebateCurrencyAmount.getCurrency().equals(ccyCounter)) {
      throw new IllegalArgumentException("base ccy rebate not implemented");
    }

    double rebate = rebateCurrencyAmount.getAmount() / notional;
    Arrays.fill(rebateArray, rebate);

    //  todo: need to get rid of the option part - only care about the rebate part!!

    final double strike;
    final PutCall putCall;
    if (barrierType.isDown()) {
      //  TODO: is this a problem?
      strike = Double.MIN_VALUE;
      putCall = PutCall.PUT;
    } else {
      //  TODO: is this a problem?
      strike = Double.MAX_VALUE;
      putCall = PutCall.CALL;
    }
    ConstantContinuousSingleBarrierKnockoutFunction barrierFunction =
        ConstantContinuousSingleBarrierKnockoutFunction.of(
            strike,
            timeToExpiry,
            putCall,
            nSteps,
            barrierType,
            barrierLevel,
            DoubleArray.ofUnsafe(rebateArray));

    return TREE.optionPriceAdjoint(barrierFunction, data);
  }

  private static void validate(ResolvedFxOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {
    ArgChecker.isTrue(
        ratesProvider.getValuationDate().isEqual(volatilities.getValuationDateTime().toLocalDate()),
        "Volatility and rate data must be for the same date");
  }

  //  vanilla option
  static ValueDerivatives priceDerivatives(
      ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities,
      RecombiningTrinomialTreeData data) {
    validate(option, ratesProvider, volatilities);
    validateData(option, ratesProvider, volatilities, data);

    int nSteps = data.getNumberOfSteps();

    double timeToExpiry = data.getTime(nSteps);

    EuropeanVanillaOptionFunction vanillaFunction = EuropeanVanillaOptionFunction.of(
        option.getStrike(), timeToExpiry, option.getPutCall(), nSteps);
    return TREE.optionPriceAdjoint(vanillaFunction, data);
  }

  private static void validate(ResolvedFxVanillaOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {
    ArgChecker.isTrue(
        ratesProvider.getValuationDate().isEqual(volatilities.getValuationDateTime().toLocalDate()),
        "Volatility and rate data must be for the same date");
  }
}
