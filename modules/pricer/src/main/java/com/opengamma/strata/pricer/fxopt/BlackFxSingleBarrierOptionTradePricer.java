/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountingPaymentPricer;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.fxopt.ResolvedFxOption;
import com.opengamma.strata.product.fxopt.ResolvedFxOptionTrade;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOption;
import com.opengamma.strata.product.fxopt.ResolvedFxSingleBarrierOptionTrade;

import java.time.LocalDate;

/**
 * Pricer for FX barrier option trades in Black-Scholes world.
 * <p>
 * This function provides the ability to price an {@link ResolvedFxSingleBarrierOptionTrade}.
 */
public class BlackFxSingleBarrierOptionTradePricer {

  /**
   * Default implementation.
   */
  public static final BlackFxSingleBarrierOptionTradePricer DEFAULT = new BlackFxSingleBarrierOptionTradePricer(
      BlackFxSingleBarrierOptionProductPricer.DEFAULT,
      DiscountingPaymentPricer.DEFAULT);

  /**
   * Pricer for {@link ResolvedFxSingleBarrierOption}.
   */
  private final BlackFxSingleBarrierOptionProductPricer productPricer;
  /**
   * Pricer for {@link Payment}.
   */
  private final DiscountingPaymentPricer paymentPricer;

  /**
   * Creates an instance.
   * 
   * @param productPricer  the pricer for {@link ResolvedFxSingleBarrierOption}
   * @param paymentPricer  the pricer for {@link Payment}
   */
  public BlackFxSingleBarrierOptionTradePricer(
      BlackFxSingleBarrierOptionProductPricer productPricer,
      DiscountingPaymentPricer paymentPricer) {
    this.productPricer = ArgChecker.notNull(productPricer, "productPricer");
    this.paymentPricer = ArgChecker.notNull(paymentPricer, "paymentPricer");
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value of the FX barrier option trade.
   * <p>
   * The present value of the trade is the value on the valuation date.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value of the trade
   */
  public MultiCurrencyAmount presentValue(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxOption product = trade.getProduct();
    CurrencyAmount pvProduct = this.productPricer.presentValue(product, ratesProvider, volatilities);
    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = this.paymentPricer.presentValue(premium, ratesProvider);
    return MultiCurrencyAmount.of(pvProduct, pvPremium);
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the present value sensitivity of the FX barrier option trade.
   * <p>
   * The present value sensitivity of the trade is the sensitivity of the present value to
   * the underlying curves.
   * <p>
   * The volatility is fixed in this sensitivity computation, i.e., sticky-strike.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value curve sensitivity of the trade
   */
  public PointSensitivities presentValueSensitivityRatesStickyStrike(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxOption product = trade.getProduct();
    PointSensitivityBuilder pvcsProduct =
        this.productPricer.presentValueSensitivityRatesStickyStrike(product, ratesProvider, volatilities);
    Payment premium = trade.getPremium();
    PointSensitivityBuilder pvcsPremium = this.paymentPricer.presentValueSensitivity(premium, ratesProvider);
    return pvcsProduct.combinedWith(pvcsPremium).build();
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the present value sensitivity to the black volatility used in the pricing.
   * <p>
   * The result is a single sensitivity to the volatility used.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the present value sensitivity
   */
  public PointSensitivities presentValueSensitivityModelParamsVolatility(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    ResolvedFxOption product = trade.getProduct();
    return this.productPricer.presentValueSensitivityModelParamsVolatility(product, ratesProvider, volatilities)
        .build();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the currency exposure of the FX barrier option trade.
   * 
   * @param trade  the option trade
   * @param ratesProvider  the rates provider
   * @param volatilities  the Black volatility provider
   * @return the currency exposure
   */
  public MultiCurrencyAmount currencyExposure(
      ResolvedFxOptionTrade trade,
      RatesProvider ratesProvider,
      BlackFxOptionVolatilities volatilities) {

    Payment premium = trade.getPremium();
    CurrencyAmount pvPremium = this.paymentPricer.presentValue(premium, ratesProvider);
    ResolvedFxOption product = trade.getProduct();
    return this.productPricer.currencyExposure(product, ratesProvider, volatilities).plus(pvPremium);
  }

  //-------------------------------------------------------------------------

  /**
   * Calculates the current of the FX barrier option trade.
   *
   * @param trade  the option trade
   * @param valuationDate  the valuation date
   * @return the current cash amount
   */
  public CurrencyAmount currentCash(ResolvedFxOptionTrade trade, LocalDate valuationDate) {
    Payment premium = trade.getPremium();
    if (premium.getDate().equals(valuationDate)) {
      return CurrencyAmount.of(premium.getCurrency(), premium.getAmount());
    }
    return CurrencyAmount.of(premium.getCurrency(), 0d);
  }

}
