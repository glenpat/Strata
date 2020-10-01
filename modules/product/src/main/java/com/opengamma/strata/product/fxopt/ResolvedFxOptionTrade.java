package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.product.ResolvedTrade;
import org.joda.beans.ImmutableBean;

public interface ResolvedFxOptionTrade extends ResolvedTrade, ImmutableBean {

  @Override
  ResolvedFxOption getProduct();

  Payment getPremium();
}
