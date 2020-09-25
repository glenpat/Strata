package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.product.ResolvedProduct;
import org.joda.beans.ImmutableBean;

import java.time.ZonedDateTime;

public interface ResolvedFxOption extends ResolvedProduct, ImmutableBean {

  ZonedDateTime getExpiry();

  CurrencyPair getCurrencyPair();
}
