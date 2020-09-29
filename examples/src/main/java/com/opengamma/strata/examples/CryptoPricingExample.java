/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.runner.CalculationFunctions;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.examples.marketdata.ExampleData;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.examples.marketdata.ExampleMarketDataBuilder;
import com.opengamma.strata.market.curve.ConstantCurve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.measure.Measures;
import com.opengamma.strata.measure.StandardComponents;
import com.opengamma.strata.measure.fxopt.FxOptionMarketDataLookup;
import com.opengamma.strata.measure.fxopt.FxSingleBarrierOptionMethod;
import com.opengamma.strata.pricer.fxopt.BlackFxOptionFlatVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilities;
import com.opengamma.strata.pricer.fxopt.FxOptionVolatilitiesId;
import com.opengamma.strata.product.AttributeType;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.fxopt.FxDigitalOption;
import com.opengamma.strata.product.fxopt.FxDigitalOptionTrade;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.report.ReportCalculationResults;
import com.opengamma.strata.report.trade.TradeReport;
import com.opengamma.strata.report.trade.TradeReportTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Example to illustrate using the engine to price FX trades.
 * <p>
 * This makes use of the example engine and the example market data environment.
 * A bullet payment trade is also included.
 */
public class CryptoPricingExample {

  /**
   * Runs the example, pricing the instruments, producing the output as an ASCII table.
   *
   * @param args  ignored
   */
  public static void main(String[] args) {
    // setup calculation runner component, which needs life-cycle management
    // a typical application might use dependency injection to obtain the instance
    try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
      calculate(runner);
    }
  }

  // obtains the data and calculates the grid of results
  private static void calculate(CalculationRunner runner) {
    // the trades that will have measures calculated
    List<Trade> trades = ImmutableList.of(createTrade1(), createTrade2(), createTrade3(), createTrade4());

    // the columns, specifying the measures to be calculated
    List<Column> columns = ImmutableList.of(
        Column.of(Measures.PRESENT_VALUE),
        Column.of(Measures.PV01_CALIBRATED_SUM),
        Column.of(Measures.PV01_CALIBRATED_BUCKETED),
        Column.of(Measures.PV01_MARKET_QUOTE_SUM),
        Column.of(Measures.PV01_MARKET_QUOTE_BUCKETED),
        Column.of(Measures.CURRENCY_EXPOSURE),
        Column.of(Measures.CURRENT_CASH),
        Column.of(Measures.RESOLVED_TARGET)
    );

    // use the built-in example market data
    LocalDate valuationDate = LocalDate.of(2014, 1, 22);
    ExampleMarketDataBuilder marketDataBuilder = ExampleMarketData.builder();
    MarketData marketData0 = marketDataBuilder.buildSnapshot(valuationDate);
    MarketData marketData = marketData0.withValue(VOL_ID1, FX_VOLS);

    // the complete set of rules for calculating measures
    CalculationFunctions functions = StandardComponents.calculationFunctions();
    final FxOptionMarketDataLookup volLookup =
        FxOptionMarketDataLookup.of(fxIndex.getCurrencyPair(), VOL_ID1);

    CalculationRules rules = CalculationRules.of(functions,
        marketDataBuilder.ratesLookup(valuationDate),
        volLookup,
        FxSingleBarrierOptionMethod.TRINOMIAL_TREE
    );

    // the reference data, such as holidays and securities
    ReferenceData refData = ReferenceData.standard();

    // calculate the results
    Results results = runner.calculate(rules, trades, columns, marketData, refData);

    // use the report runner to transform the engine results into a trade report
    ReportCalculationResults calculationResults =
        ReportCalculationResults.of(valuationDate, trades, columns, results, functions, refData);

    TradeReportTemplate reportTemplate = ExampleData.loadTradeReportTemplate("crypto-report-template");
    TradeReport tradeReport = TradeReport.of(calculationResults, reportTemplate);
    tradeReport.writeAsciiTable(System.out);
  }

  //-----------------------------------------------------------------------
  private static final FxIndex fxIndex
      = FxIndices.BTC_USD_HXRO;
  private static final FxOptionVolatilitiesId VOL_ID1 = FxOptionVolatilitiesId.of("VolId1");
  private static final FxOptionVolatilities FX_VOLS;

  private static final LocalDate valuationDate = LocalDate.of(2014, 1, 22);
  private static final LocalDate tradeSettleDate = LocalDate.of(2014, 1, 22);
  private static final LocalDate premiumSettleDate = LocalDate.of(2014, 1, 22);
  private static final LocalDate expiryDate0 = LocalDate.of(2015, 1, 22);

  static {
    final CurveMetadata curveMetadata = Curves.blackVolatilityByExpiry(CurveName.of("flatVol"), DayCounts.ACT_365F);
    FX_VOLS = BlackFxOptionFlatVolatilities.builder()
        .currencyPair(fxIndex.getCurrencyPair())
        .curve(ConstantCurve.of(curveMetadata, 0.70))
        .valuationDateTime(valuationDate.atStartOfDay(ZoneId.of("UTC")))
        .build();
  }

  // create an FX Forward trade
  private static Trade createTrade1() {
    return FxDigitalOptionTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "1"))
            .addAttribute(AttributeType.DESCRIPTION, "example 1")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(tradeSettleDate)
            .build())
        .product(FxDigitalOption.builder()
            .longShort(LongShort.LONG)
            .barrierType(BarrierType.UP)
            .index(fxIndex)
            .strikePrice(11000)
            .expiryDate(expiryDate0)
            .expiryTime(LocalTime.of(12, 0))
            .expiryZone(ZoneId.of("UTC"))
            .payment(CurrencyAmount.of(Currency.USD, 10_000))
            .build())
        .premium(AdjustablePayment.of(Currency.USD, 5000, premiumSettleDate))
        .build();
  }

  // create an FX Forward trade
  private static Trade createTrade2() {
    return FxDigitalOptionTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "2"))
            .addAttribute(AttributeType.DESCRIPTION, "example 2")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(tradeSettleDate)
            .build())
        .product(FxDigitalOption.builder()
            .longShort(LongShort.LONG)
            .barrierType(BarrierType.DOWN)
            .index(fxIndex)
            .strikePrice(11000)
            .expiryDate(expiryDate0)
            .expiryTime(LocalTime.of(12, 0))
            .expiryZone(ZoneId.of("UTC"))
            .payment(CurrencyAmount.of(Currency.USD, 10_000))
            .build())
        .premium(AdjustablePayment.of(Currency.USD, 5000, premiumSettleDate))
        .build();
  }

  // create an FX Swap trade
  private static Trade createTrade3() {
    return FxDigitalOptionTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "3"))
            .addAttribute(AttributeType.DESCRIPTION, "example 3")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(tradeSettleDate)
            .build())
        .product(FxDigitalOption.builder()
            .longShort(LongShort.LONG)
            .barrierType(BarrierType.DOWN)
            .index(FxIndices.BTC_USD_HXRO)
            .strikePrice(8000)
            .expiryDate(expiryDate0)
            .expiryTime(LocalTime.of(12, 0))
            .payment(CurrencyAmount.of(Currency.USD, 10_000))
            .expiryZone(ZoneId.of("UTC"))
            .build())
        .premium(AdjustablePayment.of(Currency.USD, 5000, premiumSettleDate))
        .build();
  }

  // create a Bullet Payment trade
  private static Trade createTrade4() {
    return FxDigitalOptionTrade.builder()
        .info(TradeInfo.builder()
            .id(StandardId.of("example", "4"))
            .addAttribute(AttributeType.DESCRIPTION, "example 4")
            .counterparty(StandardId.of("example", "BigBankA"))
            .settlementDate(tradeSettleDate)
            .build())
        .product(FxDigitalOption.builder()
            .longShort(LongShort.LONG)
            .barrierType(BarrierType.UP)
            .index(fxIndex)
            .strikePrice(8000)
            .expiryDate(expiryDate0)
            .expiryTime(LocalTime.of(12, 0))
            .payment(CurrencyAmount.of(Currency.USD, 10_000))
            .expiryZone(ZoneId.of("UTC"))
            .build())
        .premium(AdjustablePayment.of(Currency.USD, 5000, premiumSettleDate))
        .build();
  }

}
