/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.fxopt;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.runner.CalculationFunction;
import com.opengamma.strata.calc.runner.CalculationParameters;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.scenario.DoubleScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.rate.RatesMarketDataLookup;
import com.opengamma.strata.measure.rate.RatesScenarioMarketData;
import com.opengamma.strata.product.fx.FxOption;
import com.opengamma.strata.product.fx.FxOptionTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxOptionTrade;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * <PF>direct copy of {@link FxSingleBarrierOptionTradeCalculationFunction}</PF>
 * Perform calculations on an FX single barrier option trade for each of a set of scenarios.
 * <p>
 * This uses Black FX option volatilities, which must be specified using {@link FxOptionMarketDataLookup}.
 * An instance of {@link RatesMarketDataLookup} must also be specified.
 * <p>
 * Two pricing methods are available, 'Black' and 'TrinomialTree'.
 * By default, 'Black' will be used. To control the method, pass an instance of
 * {@link FxSingleBarrierOptionMethod} in the calculation parameters.
 * <p>
 * The supported built-in measures are:
 * <ul>
 *   <li>{@linkplain Measures#PRESENT_VALUE Present value}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_SUM PV01 calibrated sum on rate curves}
 *   <li>{@linkplain Measures#PV01_CALIBRATED_BUCKETED PV01 calibrated bucketed on rate curves}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_SUM PV01 market quote sum on rate curves}
 *   <li>{@linkplain Measures#PV01_MARKET_QUOTE_BUCKETED PV01 market quote bucketed on rate curves}
 *   <li>{@linkplain Measures#CURRENCY_EXPOSURE Currency exposure}
 *   <li>{@linkplain Measures#CURRENT_CASH Current cash}
 *   <li>{@linkplain Measures#RESOLVED_TARGET Resolved trade}
 * </ul>
 * <p>
 * The "natural" currency is the market convention base currency of the underlying FX.
 */
public class FxOptionTradeCalculationFunction<T extends FxOptionTrade>
    implements CalculationFunction<T> {

  /**
   * Calculate more multiple measures at a time. Note: might be problem if overlapping
   */
  private static final ImmutableSet<MultiMeasure<?>> MULTI_CALCULATORS =
      ImmutableSet.of(
          new MultiMeasure<>(
              FxSingleBarrierOptionMeasureCalculations.DEFAULT::unitPriceVolVega,
              ImmutableMap.of(
                  Measures.UNIT_PRICE, s ->
                      DoubleScenarioArray.of(DoubleArray.of(s.stream().mapToDouble(v -> v[0]))),
                  Measures.IMPLIED_VOLATILITY, s ->
                      DoubleScenarioArray.of(DoubleArray.of(s.stream().mapToDouble(v -> v[1]))),
                  Measures.BS_VEGA, s ->
                      DoubleScenarioArray.of(DoubleArray.of(s.stream().mapToDouble(v -> v[2])))
              )
          )
      );

  /**
   * The calculations by measure.
   */
  private static final ImmutableMap<Measure, SingleMeasureCalculation> CALCULATORS =
      ImmutableMap.<Measure, SingleMeasureCalculation>builder()
          .put(Measures.UNIT_PRICE, FxSingleBarrierOptionMeasureCalculations.DEFAULT::unitPrice)
          .put(Measures.IMPLIED_VOLATILITY, FxSingleBarrierOptionMeasureCalculations.DEFAULT::impliedVolatility)
          .put(Measures.BS_VEGA, FxSingleBarrierOptionMeasureCalculations.DEFAULT::bsVega)
          .put(Measures.PRESENT_VALUE, FxSingleBarrierOptionMeasureCalculations.DEFAULT::presentValue)
          .put(Measures.PV01_CALIBRATED_SUM, FxSingleBarrierOptionMeasureCalculations.DEFAULT::pv01RatesCalibratedSum)
          .put(Measures.PV01_CALIBRATED_BUCKETED,
              FxSingleBarrierOptionMeasureCalculations.DEFAULT::pv01RatesCalibratedBucketed)
          .put(Measures.PV01_MARKET_QUOTE_SUM,
              FxSingleBarrierOptionMeasureCalculations.DEFAULT::pv01RatesMarketQuoteSum)
          .put(Measures.PV01_MARKET_QUOTE_BUCKETED,
              FxSingleBarrierOptionMeasureCalculations.DEFAULT::pv01RatesMarketQuoteBucketed)
          .put(Measures.CURRENCY_EXPOSURE, FxSingleBarrierOptionMeasureCalculations.DEFAULT::currencyExposure)
          .put(Measures.CURRENT_CASH, FxSingleBarrierOptionMeasureCalculations.DEFAULT::currentCash)
          .put(Measures.RESOLVED_TARGET, (rt, smd, m, meth) -> rt)
          .build();

  private static final ImmutableSet<Measure> MEASURES = CALCULATORS.keySet();

  private final Class<T> tClass;

  /**
   * Creates an instance.
   * @param tClass
   */
  public FxOptionTradeCalculationFunction(Class<T> tClass) {
    this.tClass = tClass;
  }

  //-------------------------------------------------------------------------
  @Override
  public Class<T> targetType() {
    return this.tClass;
  }

  @Override
  public Set<Measure> supportedMeasures() {
    return MEASURES;
  }

  @Override
  public Optional<String> identifier(T target) {
    return target.getInfo().getId().map(id -> id.toString());
  }

  @Override
  public Currency naturalCurrency(T trade, ReferenceData refData) {
    return trade.getProduct().getCurrencyPair().getBase();
  }

  //-------------------------------------------------------------------------
  @Override
  public FunctionRequirements requirements(
      T trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ReferenceData refData) {

    // extract data from product
    FxOption product = trade.getProduct();
    CurrencyPair currencyPair = product.getCurrencyPair();

    // use lookup to build requirements - first try FxIndex if has one, then CurrencyPair
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    FunctionRequirements ratesReqs = product.getFxIndex()
        .map(ratesLookup::requirements)
        .orElseGet(() -> ratesLookup.requirements(currencyPair));

    FxOptionMarketDataLookup optionLookup = parameters.getParameter(FxOptionMarketDataLookup.class);
    FunctionRequirements optionReqs = optionLookup.requirements(currencyPair);
    return ratesReqs.combinedWith(optionReqs);
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<Measure, Result<?>> calculate(
      T trade,
      Set<Measure> measures,
      CalculationParameters parameters,
      ScenarioMarketData scenarioMarketData,
      ReferenceData refData) {

    // expand the trade once for all measures and all scenarios
    ResolvedFxOptionTrade resolved = trade.resolve(refData);
    RatesMarketDataLookup ratesLookup = parameters.getParameter(RatesMarketDataLookup.class);
    RatesScenarioMarketData ratesMarketData = ratesLookup.marketDataView(scenarioMarketData);
    FxOptionMarketDataLookup optionLookup = parameters.getParameter(FxOptionMarketDataLookup.class);
    FxOptionScenarioMarketData optionMarketData = optionLookup.marketDataView(scenarioMarketData);
    FxSingleBarrierOptionMethod method =
        parameters.findParameter(FxSingleBarrierOptionMethod.class).orElse(FxSingleBarrierOptionMethod.BLACK);

    //  output measure results
    Map<Measure, Result<?>> results = new HashMap<>();

    //  check for possible optimized calculation
    for (MultiMeasure<?> multiCalculator : MULTI_CALCULATORS) {

      if (multiCalculator.isMatch(measures)) {

        final Map<Measure, Result<?>> values =
            multiCalculator.calculate(resolved, ratesMarketData, optionMarketData, method);

        for (Map.Entry<Measure, Result<?>> e : values.entrySet()) {
          final Measure measure = e.getKey();
          final Result<?> result = e.getValue();
          results.put(measure, result);
        }
      }
    }

    // loop around measures, calculating all scenarios for one measure
    for (Measure measure : measures) {
      results.computeIfAbsent(measure,
          m -> calculate(m, resolved, ratesMarketData, optionMarketData, method));
    }
    return results;
  }

  // calculate one measure
  private Result<?> calculate(
      Measure measure,
      ResolvedFxOptionTrade trade,
      RatesScenarioMarketData ratesMarketData,
      FxOptionScenarioMarketData optionMarketData,
      FxSingleBarrierOptionMethod method) {

    SingleMeasureCalculation calculator = CALCULATORS.get(measure);
    if (calculator == null) {
      return Result
          .failure(FailureReason.UNSUPPORTED, "Unsupported measure for FxSingleBarrierOptionTrade: {}", measure);
    }
    return Result.of(() -> calculator.calculate(trade, ratesMarketData, optionMarketData, method));
  }

  //-------------------------------------------------------------------------
  @FunctionalInterface
  interface SingleMeasureCalculation {
    public abstract Object calculate(
        ResolvedFxOptionTrade trade,
        RatesScenarioMarketData ratesMarketData,
        FxOptionScenarioMarketData optionMarketData,
        FxSingleBarrierOptionMethod method);
  }

  @FunctionalInterface
  interface MultiMeasureCalculation<V> {
    public abstract V calculate(
        ResolvedFxOptionTrade trade,
        RatesScenarioMarketData ratesMarketData,
        FxOptionScenarioMarketData optionMarketData,
        FxSingleBarrierOptionMethod method);
  }

  static class MultiMeasure<V> {
    private final MultiMeasureCalculation<V> multiMeasureCalculation;
    private final Map<Measure, Function<V, Object>> singleMeasureExtract;

    MultiMeasure(
        MultiMeasureCalculation<V> multiMeasureCalculation,
        Map<Measure, Function<V, Object>> singleMeasureExtract) {
      this.multiMeasureCalculation = multiMeasureCalculation;
      this.singleMeasureExtract = singleMeasureExtract;
    }

    boolean isMatch(final Set<Measure> requestedMeasures) {
      for (Measure measure : singleMeasureExtract.keySet()) {
        if (!requestedMeasures.contains(measure)) {
          return false;
        }
      }
      return true;
    }

    public Map<Measure, Result<?>> calculate(
        ResolvedFxOptionTrade trade,
        RatesScenarioMarketData ratesMarketData,
        FxOptionScenarioMarketData optionMarketData,
        FxSingleBarrierOptionMethod method) {

      try {
        final V multiResult = multiMeasureCalculation.calculate(trade, ratesMarketData, optionMarketData, method);
        return Maps.transformValues(singleMeasureExtract,
            input -> {
              assert input != null;
              return Result.success(input.apply(multiResult));
            });
      } catch (final Exception e) {
        return Maps.transformValues(singleMeasureExtract,
            input -> {
              assert input != null;
              return Result.failure(e);
            });
      }
    }
  }

}
