package com.opengamma.strata.product.fx;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.product.PortfolioItemInfo;
import com.opengamma.strata.product.fxopt.ResolvedFxOptionTrade;

public interface FxOptionTrade extends FxTrade {
  @Override
  FxOptionTrade withInfo(PortfolioItemInfo info);

  @Override
  FxOption getProduct();

  ResolvedFxOptionTrade resolve(ReferenceData refData);
}
