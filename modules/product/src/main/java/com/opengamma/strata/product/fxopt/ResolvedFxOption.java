package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.product.ResolvedProduct;

import java.time.ZonedDateTime;

public interface ResolvedFxOption extends ResolvedProduct {

  ZonedDateTime getExpiry();

  CurrencyPair getCurrencyPair();
}
