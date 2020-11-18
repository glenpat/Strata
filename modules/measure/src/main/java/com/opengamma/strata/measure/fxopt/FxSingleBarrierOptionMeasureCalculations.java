/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.scenario.CurrencyScenarioArray;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.MultiCurrencyScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.BlackFxSingleBarrierOptionTradePricer;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.MarketQuoteSensitivityCalculator;
import com.opengamma.strata.product.fxopt.ResolvedFxOptionTrade;

import java.time.LocalDate;

/**
 * <PF>updated w/ generic support for FxOption (vanillas, digitals, barrier, one-touch)</PF>
 * Multi-scenario measure calculations for FX single barrier option trades.
 * <p>
 * Each method corresponds to a measure, typically calculated by one or more calls to the pricer.
 */
final class FxSingleBarrierOptionMeasureCalculations {

  /**
   * Default implementation.
   */
  public static final FxSingleBarrierOptionMeasureCalculations DEFAULT = new FxSingleBarrierOptionMeasureCalculations(
      BlackFxSingleBarrierOptionTradePricer.DEFAULT,
      ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer.DEFAULT);
  /**
   * The market quote sensitivity calculator.
   */
  private static final MarketQuoteSensitivityCalculator MARKET_QUOTE_SENS = MarketQuoteSensitivityCalculator.DEFAULT;
  /**
   * One basis point, expressed as a {@code double}.
   */
  private static final double ONE_BASIS_POINT = 1e-4;

  /**
   * Pricer for {@link ResolvedFxOptionTrade}.
   */
  private final BlackFxSingleBarrierOptionTradePricer blackPricer;
  /**
   * Pricer for {@link ResolvedFxOptionTrade}.
   */
  private final ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer trinomialTreePricer;

  /**
   * Creates an instance.
   *
   * @param blackPricer  the pricer for {@link ResolvedFxOptionTrade}
   * @param trinomialTreePricer  the pricer for {@link ResolvedFxOptionTrade} SABR
   */
  FxSingleBarrierOptionMeasureCalculations(
      BlackFxSingleBarrierOptionTradePricer blackPricer,
      ImpliedTrinomialTreeFxSingleBarrierOptionTradePricer trinomialTreePricer) {
    this.blackPricer = ArgChecker.notNull(blackPricer, "blackPricer");
    this.trinomialTreePricer = ArgChecker.notNull(trinomialTreePricer, "trinomialTreePricer");
  }

  //-------------------------------------------------------------------------
  // calculates present value for all scenarios
  MultiCurrencyScenarioArray presentValue(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> presentValue(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // present value for one scenario
  MultiCurrencyAmount presentValue(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return this.trinomialTreePricer.presentValue(trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities));
    } else {
      return this.blackPricer.presentValue(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // unit price for all scenarios
  DoubleScenarioArray unitPrice(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return DoubleScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> unitPrice(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // unit price for one scenario
  double unitPrice(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return trinomialTreePricer.unitPrice(trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities));
    } else {
      return this.blackPricer.unitPrice(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // unitPriceVolVega price for all scenarios
  ScenarioArray<double[]> unitPriceVolVega(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> unitPriceVolVega(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // unitPriceVolVega for one scenario
  double[] unitPriceVolVega(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return new double[] {
          trinomialTreePricer.unitPrice(trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities)),
          0.69D,
          -0.69D
      };
    } else {
      return new double[] {
          blackPricer.unitPrice(trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities)),
          0.69D,
          -0.69D
      };
    }
  }

  //-------------------------------------------------------------------------
  // unit price for all scenarios
  DoubleScenarioArray impliedVolatility(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return DoubleScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> impliedVolatility(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // unit price for one scenario
  double impliedVolatility(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    throw new IllegalArgumentException("use multi calc instead");
  }

  //-------------------------------------------------------------------------
  // bsVega for all scenarios
  DoubleScenarioArray bsVega(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return DoubleScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> bsVega(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // bsVega for one scenario
  double bsVega(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    throw new IllegalArgumentException("use multi calc instead");
  }

  //-------------------------------------------------------------------------
  // calculates calibrated sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesCalibratedSum(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // calibrated sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesCalibratedSum(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return paramSens.total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates calibrated bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesCalibratedBucketed(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesCalibratedBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // calibrated bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesCalibratedBucketed(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return paramSens.multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote sum PV01 for all scenarios
  MultiCurrencyScenarioArray pv01RatesMarketQuoteSum(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteSum(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // market quote sum PV01 for one scenario
  MultiCurrencyAmount pv01RatesMarketQuoteSum(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).total().multipliedBy(ONE_BASIS_POINT);
  }

  //-------------------------------------------------------------------------
  // calculates market quote bucketed PV01 for all scenarios
  ScenarioArray<CurrencyParameterSensitivities> pv01RatesMarketQuoteBucketed(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return ScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> pv01RatesMarketQuoteBucketed(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // market quote bucketed PV01 for one scenario
  CurrencyParameterSensitivities pv01RatesMarketQuoteBucketed(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    CurrencyParameterSensitivities paramSens = parameterSensitivities(trade, ratesProvider, volatilities, method);
    return MARKET_QUOTE_SENS.sensitivity(paramSens, ratesProvider).multipliedBy(ONE_BASIS_POINT);
  }

  // point sensitivity
  private CurrencyParameterSensitivities parameterSensitivities(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return this.trinomialTreePricer.presentValueSensitivityRates(
          trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities));
    } else {
      PointSensitivities pointSens = this.blackPricer.presentValueSensitivityRatesStickyStrike(
          trade, ratesProvider, checkBlackVolatilities(volatilities));
      return ratesProvider.parameterSensitivity(pointSens);
    }
  }

  //-------------------------------------------------------------------------
  // calculates currency exposure for all scenarios
  MultiCurrencyScenarioArray currencyExposure(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    CurrencyPair currencyPair = trade.getProduct().getCurrencyPair();
    return MultiCurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currencyExposure(
            trade,
            ratesMarketData.scenario(i).ratesProvider(),
            optionMarketData.scenario(i).volatilities(currencyPair),
            method));
  }

  // currency exposure for one scenario
  MultiCurrencyAmount currencyExposure(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      FxOptionVolatilities volatilities,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return this.trinomialTreePricer
          .currencyExposure(trade, ratesProvider, checkTrinomialTreeVolatilities(volatilities));
    } else {
      return this.blackPricer.currencyExposure(trade, ratesProvider, checkBlackVolatilities(volatilities));
    }
  }

  //-------------------------------------------------------------------------
  // calculates current cash for all scenarios
  CurrencyScenarioArray currentCash(
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    return CurrencyScenarioArray.of(
        ratesMarketData.getScenarioCount(),
        i -> currentCash(
            trade,
            ratesMarketData.scenario(i).getValuationDate(),
            method));
  }

  // current cash for one scenario
  CurrencyAmount currentCash(
      ResolvedFxOptionTrade trade,
      LocalDate valuationDate,
      FxSingleBarrierOptionMethod method) {

    if (method == FxSingleBarrierOptionMethod.TRINOMIAL_TREE) {
      return this.trinomialTreePricer.currentCash(trade, valuationDate);
    } else {
      return this.blackPricer.currentCash(trade, valuationDate);
    }
  }

  //-------------------------------------------------------------------------
  // ensures that the volatilities are correct
  private BlackFxOptionVolatilities checkBlackVolatilities(FxOptionVolatilities volatilities) {
    if (volatilities instanceof BlackFxOptionVolatilities) {
      return (BlackFxOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException("FX single barrier option Black pricing requires BlackFxOptionVolatilities");
  }

  // ensures that the volatilities are correct
  private BlackFxOptionVolatilities checkTrinomialTreeVolatilities(FxOptionVolatilities volatilities) {
    if (volatilities instanceof BlackFxOptionVolatilities) {
      return (BlackFxOptionVolatilities) volatilities;
    }
    throw new IllegalArgumentException("FX single barrier option Trinomial Tree pricing requires BlackFxOptionVolatilities");
  }

}
