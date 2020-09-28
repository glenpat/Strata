package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.impl.tree.DigitalOptionFunction;
import com.opengamma.strata.pricer.impl.tree.EuropeanVanillaOptionFunction;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxDigitalOption;
import com.opengamma.strata.product.fxopt.ResolvedFxVanillaOption;

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

    DigitalOptionFunction digitalFunction = DigitalOptionFunction.of(
        option.getStrikePrice(), timeToExpiry, option.getBarrierType(), nSteps);
    return TREE.optionPriceAdjoint(digitalFunction, data);
  }

  private static void validate(ResolvedFxDigitalOption option,
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
