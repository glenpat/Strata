/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.option.DeltaStrike;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.product.common.PutCall;
import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * <PF>based off BlackFxOptionSmileVolatilities</PF>
 * Data provider of volatility for FX options in the log-normal or Black-Scholes model.
 * <p>
 * The volatility is represented by a term structure of interpolated smile, 
 * {@link SmileDeltaTermStructure}, which represents expiry dependent smile formed of
 * ATM, risk reversal and strangle as used in FX market.
 */
@BeanDefinition
public final class BlackFxOptionSsviVolatilities
    implements BlackFxOptionVolatilities, ImmutableBean, Serializable {

  /**
   * The name of the volatilities.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FxOptionVolatilitiesName name;
  /**
   * The currency pair that the volatilities are for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final CurrencyPair currencyPair;
  /**
   * The valuation date-time.
   * All data items in this provider is calibrated for this date-time.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /**
   * The volatility model.
   * <p>
   * This represents expiry dependent smile which consists of ATM, risk reversal
   * and strangle as used in FX market.
   */
  @PropertyDefinition(validate = "notNull")
  private final SsviTermStructure smile;

  //-------------------------------------------------------------------------

  /**
   * Obtains an instance based on a ssvi term structure.
   *
   * @param name  the name of the volatilities
   * @param currencyPair  the currency pair
   * @param valuationTime  the valuation date-time
   * @param ssvi  the term structure of smile
   * @return the provider
   */
  public static BlackFxOptionSsviVolatilities of(
      final FxOptionVolatilitiesName name,
      final CurrencyPair currencyPair,
      final ZonedDateTime valuationTime,
      final SsviTermStructure ssvi) {

    return new BlackFxOptionSsviVolatilities(name, currencyPair, valuationTime, ssvi);
  }

  //-------------------------------------------------------------------------
  @Override
  public <T> Optional<T> findData(final MarketDataName<T> name) {
    if (this.name.equals(name)) {
      return Optional.of(name.getMarketDataType().cast(this));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return this.smile.getParameterCount();
  }

  @Override
  public double getParameter(final int parameterIndex) {
    return this.smile.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(final int parameterIndex) {
    return this.smile.getParameterMetadata(parameterIndex);
  }

  @Override
  public BlackFxOptionSsviVolatilities withParameter(final int parameterIndex, final double newValue) {
    return new BlackFxOptionSsviVolatilities(
        this.name, this.currencyPair, this.valuationDateTime, this.smile.withParameter(parameterIndex, newValue));
  }

  @Override
  public BlackFxOptionSsviVolatilities withPerturbation(final ParameterPerturbation perturbation) {
    return new BlackFxOptionSsviVolatilities(
        this.name, this.currencyPair, this.valuationDateTime, this.smile.withPerturbation(perturbation));
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(final CurrencyPair currencyPair, final double expiryTime, final double strike,
      final double forward) {
    if (currencyPair.isInverse(this.currencyPair)) {
      return this.smile.volatility(expiryTime, 1d / strike, 1d / forward);
    }
    return this.smile.volatility(expiryTime, strike, forward);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(final PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (final PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof FxOptionSensitivity) {
        final FxOptionSensitivity pt = (FxOptionSensitivity) point;
        if (pt.getVolatilitiesName().equals(getName())) {
          sens = sens.combinedWith(parameterSensitivity(pt));
        }
      }
    }
    return sens;
  }

  private CurrencyParameterSensitivity parameterSensitivity(final FxOptionSensitivity point) {
    final double expiryTime = point.getExpiry();
    final double strike =
        this.currencyPair.isInverse(point.getCurrencyPair()) ? 1d / point.getStrike() : point.getStrike();
    final double forward =
        this.currencyPair.isInverse(point.getCurrencyPair()) ? 1d / point.getForward() : point.getForward();
    final double pointValue = point.getSensitivity();
    final DoubleMatrix bucketedSensi =
        this.smile.volatilityAndSensitivities(expiryTime, strike, forward).getSensitivities();
    final DoubleArray smileExpiries = this.smile.getExpiries();
    final List<Optional<Tenor>> smileExpiryTenors = this.smile.getExpiryTenors();
    final int nTimes = smileExpiries.size();
    final List<Double> sensiList = new ArrayList<>();
    final List<ParameterMetadata> paramList = new ArrayList<>();
    final DoubleArray deltas = this.smile.getDelta();
    final int nDeltas = deltas.size();
    // convert sensitivity
    for (int i = 0; i < nTimes; ++i) {
      final double smileExpiry = smileExpiries.get(i);
      final Optional<Tenor> tenorOpt = smileExpiryTenors.get(i);
      // calculate absolute delta
      final int nDeltasTotal = 2 * nDeltas + 1;
      final double[] deltasTotal = new double[nDeltasTotal];
      deltasTotal[nDeltas] = 0.5d;
      for (int j = 0; j < nDeltas; ++j) {
        deltasTotal[j] = 1d - deltas.get(j);
        deltasTotal[2 * nDeltas - j] = deltas.get(j);
      }
      // convert sensitivities
      for (int j = 0; j < nDeltasTotal; ++j) {
        sensiList.add(bucketedSensi.get(i, j) * pointValue);
        final DeltaStrike absoluteDelta = DeltaStrike.of(deltasTotal[j]);
        final ParameterMetadata parameterMetadata = tenorOpt
            .map(tenor -> FxVolatilitySurfaceYearFractionParameterMetadata
                .of(smileExpiry, tenor, absoluteDelta, this.currencyPair))
            .orElseGet(
                () -> FxVolatilitySurfaceYearFractionParameterMetadata
                    .of(smileExpiry, absoluteDelta, this.currencyPair));
        paramList.add(parameterMetadata);
      }
    }
    return CurrencyParameterSensitivity.of(this.name, paramList, point.getCurrency(), DoubleArray.copyOf(sensiList));
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(
      final double expiry, final PutCall putCall, final double strike, final double forward, final double volatility) {
    return BlackFormulaRepository.price(forward, strike, expiry, volatility, putCall.isCall());
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(final ZonedDateTime dateTime) {
    ArgChecker.notNull(dateTime, "dateTime");
    final LocalDate valuationDate = this.valuationDateTime.toLocalDate();
    final LocalDate date = dateTime.toLocalDate();
    return this.smile.getDayCount().relativeYearFraction(valuationDate, date);
  }

  //------------------------- AUTOGENERATED START -------------------------

  /**
   * The meta-bean for {@code BlackFxOptionSsviVolatilities}.
   * @return the meta-bean, not null
   */
  public static BlackFxOptionSsviVolatilities.Meta meta() {
    return BlackFxOptionSsviVolatilities.Meta.INSTANCE;
  }

  static {
    MetaBean.register(BlackFxOptionSsviVolatilities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static BlackFxOptionSsviVolatilities.Builder builder() {
    return new BlackFxOptionSsviVolatilities.Builder();
  }

  private BlackFxOptionSsviVolatilities(
      FxOptionVolatilitiesName name,
      CurrencyPair currencyPair,
      ZonedDateTime valuationDateTime,
      SsviTermStructure smile) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(currencyPair, "currencyPair");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    JodaBeanUtils.notNull(smile, "smile");
    this.name = name;
    this.currencyPair = currencyPair;
    this.valuationDateTime = valuationDateTime;
    this.smile = smile;
  }

  @Override
  public BlackFxOptionSsviVolatilities.Meta metaBean() {
    return BlackFxOptionSsviVolatilities.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the name of the volatilities.
   * @return the value of the property, not null
   */
  @Override
  public FxOptionVolatilitiesName getName() {
    return this.name;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the currency pair that the volatilities are for.
   * @return the value of the property, not null
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return this.currencyPair;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the valuation date-time.
   * All data items in this provider is calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return this.valuationDateTime;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the volatility model.
   * <p>
   * This represents expiry dependent smile which consists of ATM, risk reversal
   * and strangle as used in FX market.
   * @return the value of the property, not null
   */
  public SsviTermStructure getSmile() {
    return this.smile;
  }

  //-----------------------------------------------------------------------

  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      BlackFxOptionSsviVolatilities other = (BlackFxOptionSsviVolatilities) obj;
      return JodaBeanUtils.equal(this.name, other.name) &&
          JodaBeanUtils.equal(this.currencyPair, other.currencyPair) &&
          JodaBeanUtils.equal(this.valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(this.smile, other.smile);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(this.name);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.currencyPair);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.smile);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("BlackFxOptionSsviVolatilities{");
    buf.append("name").append('=').append(JodaBeanUtils.toString(this.name)).append(',').append(' ');
    buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(this.currencyPair)).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(this.valuationDateTime)).append(',')
        .append(' ');
    buf.append("smile").append('=').append(JodaBeanUtils.toString(this.smile));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------

  /**
   * The meta-bean for {@code BlackFxOptionSsviVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<FxOptionVolatilitiesName> name = DirectMetaProperty.ofImmutable(
        this, "name", BlackFxOptionSsviVolatilities.class, FxOptionVolatilitiesName.class);
    /**
     * The meta-property for the {@code currencyPair} property.
     */
    private final MetaProperty<CurrencyPair> currencyPair = DirectMetaProperty.ofImmutable(
        this, "currencyPair", BlackFxOptionSsviVolatilities.class, CurrencyPair.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", BlackFxOptionSsviVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code smile} property.
     */
    private final MetaProperty<SsviTermStructure> smile = DirectMetaProperty.ofImmutable(
        this, "smile", BlackFxOptionSsviVolatilities.class, SsviTermStructure.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "currencyPair",
        "valuationDateTime",
        "smile");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return this.name;
        case 1005147787:  // currencyPair
          return this.currencyPair;
        case -949589828:  // valuationDateTime
          return this.valuationDateTime;
        case 109556488:  // smile
          return this.smile;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BlackFxOptionSsviVolatilities.Builder builder() {
      return new BlackFxOptionSsviVolatilities.Builder();
    }

    @Override
    public Class<? extends BlackFxOptionSsviVolatilities> beanType() {
      return BlackFxOptionSsviVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return this.metaPropertyMap$;
    }

    //-----------------------------------------------------------------------

    /**
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxOptionVolatilitiesName> name() {
      return this.name;
    }

    /**
     * The meta-property for the {@code currencyPair} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyPair> currencyPair() {
      return this.currencyPair;
    }

    /**
     * The meta-property for the {@code valuationDateTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> valuationDateTime() {
      return this.valuationDateTime;
    }

    /**
     * The meta-property for the {@code smile} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SsviTermStructure> smile() {
      return this.smile;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((BlackFxOptionSsviVolatilities) bean).getName();
        case 1005147787:  // currencyPair
          return ((BlackFxOptionSsviVolatilities) bean).getCurrencyPair();
        case -949589828:  // valuationDateTime
          return ((BlackFxOptionSsviVolatilities) bean).getValuationDateTime();
        case 109556488:  // smile
          return ((BlackFxOptionSsviVolatilities) bean).getSmile();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------

  /**
   * The bean-builder for {@code BlackFxOptionSsviVolatilities}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<BlackFxOptionSsviVolatilities> {

    private FxOptionVolatilitiesName name;
    private CurrencyPair currencyPair;
    private ZonedDateTime valuationDateTime;
    private SsviTermStructure smile;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(BlackFxOptionSsviVolatilities beanToCopy) {
      this.name = beanToCopy.getName();
      this.currencyPair = beanToCopy.getCurrencyPair();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
      this.smile = beanToCopy.getSmile();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return this.name;
        case 1005147787:  // currencyPair
          return this.currencyPair;
        case -949589828:  // valuationDateTime
          return this.valuationDateTime;
        case 109556488:  // smile
          return this.smile;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (FxOptionVolatilitiesName) newValue;
          break;
        case 1005147787:  // currencyPair
          this.currencyPair = (CurrencyPair) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
          break;
        case 109556488:  // smile
          this.smile = (SsviTermStructure) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public BlackFxOptionSsviVolatilities build() {
      return new BlackFxOptionSsviVolatilities(
          this.name,
          this.currencyPair,
          this.valuationDateTime,
          this.smile);
    }

    //-----------------------------------------------------------------------

    /**
     * Sets the name of the volatilities.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(FxOptionVolatilitiesName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the currency pair that the volatilities are for.
     * @param currencyPair  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currencyPair(CurrencyPair currencyPair) {
      JodaBeanUtils.notNull(currencyPair, "currencyPair");
      this.currencyPair = currencyPair;
      return this;
    }

    /**
     * Sets the valuation date-time.
     * All data items in this provider is calibrated for this date-time.
     * @param valuationDateTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDateTime(ZonedDateTime valuationDateTime) {
      JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
      this.valuationDateTime = valuationDateTime;
      return this;
    }

    /**
     * Sets the volatility model.
     * <p>
     * This represents expiry dependent smile which consists of ATM, risk reversal
     * and strangle as used in FX market.
     * @param smile  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder smile(SsviTermStructure smile) {
      JodaBeanUtils.notNull(smile, "smile");
      this.smile = smile;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("BlackFxOptionSsviVolatilities.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(this.name)).append(',').append(' ');
      buf.append("currencyPair").append('=').append(JodaBeanUtils.toString(this.currencyPair)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(this.valuationDateTime)).append(',')
          .append(' ');
      buf.append("smile").append('=').append(JodaBeanUtils.toString(this.smile));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
