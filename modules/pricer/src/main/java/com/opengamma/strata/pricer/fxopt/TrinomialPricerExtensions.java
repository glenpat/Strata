package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
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
//    int nSteps = data.getNumberOfSteps();
//    ResolvedFxVanillaOption underlyingOption = option.getUnderlyingOption();
//    double timeToExpiry = data.getTime(nSteps);
//    ResolvedFxSingle underlyingFx = underlyingOption.getUnderlying();
//    Currency ccyBase = underlyingFx.getCounterCurrencyPayment().getCurrency();
//    Currency ccyCounter = underlyingFx.getCounterCurrencyPayment().getCurrency();
//    DiscountFactors baseDiscountFactors = ratesProvider.discountFactors(ccyBase);
//    DiscountFactors counterDiscountFactors = ratesProvider.discountFactors(ccyCounter);
//    double rebateAtExpiry = 0d; // used to price knock-in option
//    double rebateAtExpiryDerivative = 0d; // used to price knock-in option
//    double notional = Math.abs(underlyingFx.getBaseCurrencyPayment().getAmount());
//    double[] rebateArray = new double[nSteps + 1];
//    SimpleConstantContinuousBarrier barrier = (SimpleConstantContinuousBarrier) option.getBarrier();
//    if (option.getRebate().isPresent()) {
//      CurrencyAmount rebateCurrencyAmount = option.getRebate().get();
//      double rebatePerUnit = rebateCurrencyAmount.getAmount() / notional;
//      boolean isCounter = rebateCurrencyAmount.getCurrency().equals(ccyCounter);
//      double rebate = isCounter ? rebatePerUnit : rebatePerUnit * barrier.getBarrierLevel();
//      if (barrier.getKnockType().isKnockIn()) { // use in-out parity
//        double dfCounterAtExpiry = counterDiscountFactors.discountFactor(timeToExpiry);
//        double dfBaseAtExpiry = baseDiscountFactors.discountFactor(timeToExpiry);
//        for (int i = 0; i < nSteps + 1; ++i) {
//          rebateArray[i] = isCounter ?
//              rebate * dfCounterAtExpiry / counterDiscountFactors.discountFactor(data.getTime(i)) :
//              rebate * dfBaseAtExpiry / baseDiscountFactors.discountFactor(data.getTime(i));
//        }
//        if (isCounter) {
//          rebateAtExpiry = rebatePerUnit * dfCounterAtExpiry;
//        } else {
//          rebateAtExpiry = rebatePerUnit * data.getSpot() * dfBaseAtExpiry;
//          rebateAtExpiryDerivative = rebatePerUnit * dfBaseAtExpiry;
//        }
//      } else {
//        Arrays.fill(rebateArray, rebate);
//      }
//    }
//    ConstantContinuousSingleBarrierKnockoutFunction barrierFunction =
//        ConstantContinuousSingleBarrierKnockoutFunction.of(
//            underlyingOption.getStrike(),
//            timeToExpiry,
//            underlyingOption.getPutCall(),
//            nSteps,
//            barrier.getBarrierType(),
//            barrier.getBarrierLevel(),
//            DoubleArray.ofUnsafe(rebateArray));
//    ValueDerivatives barrierPrice = TREE.optionPriceAdjoint(barrierFunction, data);
//    if (barrier.getKnockType().isKnockIn()) {  // use in-out parity
//      EuropeanVanillaOptionFunction vanillaFunction = EuropeanVanillaOptionFunction.of(
//          underlyingOption.getStrike(), timeToExpiry, underlyingOption.getPutCall(), nSteps);
//      ValueDerivatives vanillaPrice = TREE.optionPriceAdjoint(vanillaFunction, data);
//      return ValueDerivatives.of(vanillaPrice.getValue() + rebateAtExpiry - barrierPrice.getValue(),
//          DoubleArray.of(vanillaPrice.getDerivative(0) + rebateAtExpiryDerivative - barrierPrice.getDerivative(0)));
//    }
//    return barrierPrice;
    throw new RuntimeException("not implemented");
  }

  private static void validate(ResolvedFxDigitalOption option,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {
    //  TODO: any validation required?
//    ArgChecker.isTrue(option.getBarrier() instanceof SimpleConstantContinuousBarrier,
//        "barrier should be SimpleConstantContinuousBarrier");
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
