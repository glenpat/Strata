/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.fx.ResolvedFxSingle;
import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.opengamma.strata.collect.ArgChecker.inOrderOrEqual;

/**
 * A vanilla FX option, resolved for pricing.
 * <p>
 * This is the resolved form of {@link FxVanillaOption} and is an input to the pricers.
 * Applications will typically create a {@code ResolvedFxVanillaOption} from a {@code FxVanillaOption}
 * using {@link FxVanillaOption#resolve(ReferenceData)}.
 * <p>
 * A {@code ResolvedFxVanillaOption} is bound to data that changes over time, such as holiday calendars.
 * If the data changes, such as the addition of a new holiday, the resolved form will not be updated.
 * Care must be taken when placing the resolved form in a cache or persistence layer.
 */
@BeanDefinition
public final class ResolvedFxVanillaOption
    implements ResolvedFxOption, ImmutableBean, Serializable {

  /**
   * Whether the option is long or short.
   * <p>
   * At expiry, the long party will have the option to enter in this transaction; 
   * the short party will, at the option of the long party, potentially enter into the inverse transaction.
   */
  @PropertyDefinition(validate = "notNull")
  private final LongShort longShort;
  /**
   * The expiry date-time of the option.
   * <p>
   * The option is European, and can only be exercised on the expiry date.
   */
  @PropertyDefinition(validate = "notNull")
  private final ZonedDateTime expiry;
  /**
   * The underlying foreign exchange transaction.
   * <p>
   * At expiry, if the option is in the money, this foreign exchange will occur.
   * A call option permits the transaction as specified to occur.
   * A put option permits the inverse transaction to occur.
   */
  @PropertyDefinition(validate = "notNull")
  private final ResolvedFxSingle underlying;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    inOrderOrEqual(this.expiry.toLocalDate(), this.underlying.getPaymentDate(), "expiry.date",
        "underlying.paymentDate");
  }

  //-------------------------------------------------------------------------

  /**
   * Gets currency pair of the base currency and counter currency.
   * <p>
   * This currency pair is conventional, thus indifferent to the direction of FX.
   *
   * @return the currency pair
   */
  @Override
  public CurrencyPair getCurrencyPair() {
    return this.underlying.getCurrencyPair();
  }

  /**
   * Gets the expiry date of the option.
   * 
   * @return the expiry date
   */
  public LocalDate getExpiryDate() {
    return this.expiry.toLocalDate();
  }

  /**
   * Gets the strike rate.
   * 
   * @return the strike
   */
  public double getStrike() {
    return Math.abs(this.underlying.getCounterCurrencyPayment().getAmount() /
        this.underlying.getBaseCurrencyPayment().getAmount());
  }

  /**
   * Returns the put/call flag.
   * <p>
   * This is the put/call for the base currency.
   * If the amount for the base currency is positive, the option is a call on the base currency (put on counter currency). 
   * If the amount for the base currency is negative, the option is a put on the base currency (call on counter currency).
   * 
   * @return the put or call
   */
  public PutCall getPutCall() {
    return this.underlying.getCounterCurrencyPayment().getAmount() > 0d ? PutCall.PUT : PutCall.CALL;
  }

  /**
   * Get the counter currency of the underlying FX transaction.
   *
   * @return the counter currency
   */
  public Currency getCounterCurrency() {
    return this.underlying.getCounterCurrencyPayment().getCurrency();
  }

  @Override
  public CurrencyAmount getSignedNotional() {
    final double amount =
        (getLongShort().isLong() ? 1d : -1d) * Math.abs(getUnderlying().getBaseCurrencyPayment().getAmount());
    return CurrencyAmount.of(getCounterCurrency(), amount);
  }

  //------------------------- AUTOGENERATED START -------------------------

  /**
   * The meta-bean for {@code ResolvedFxVanillaOption}.
   * @return the meta-bean, not null
   */
  public static ResolvedFxVanillaOption.Meta meta() {
    return ResolvedFxVanillaOption.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ResolvedFxVanillaOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ResolvedFxVanillaOption.Builder builder() {
    return new ResolvedFxVanillaOption.Builder();
  }

  private ResolvedFxVanillaOption(
      LongShort longShort,
      ZonedDateTime expiry,
      ResolvedFxSingle underlying) {
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(expiry, "expiry");
    JodaBeanUtils.notNull(underlying, "underlying");
    this.longShort = longShort;
    this.expiry = expiry;
    this.underlying = underlying;
    validate();
  }

  @Override
  public ResolvedFxVanillaOption.Meta metaBean() {
    return ResolvedFxVanillaOption.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the option is long or short.
   * <p>
   * At expiry, the long party will have the option to enter in this transaction;
   * the short party will, at the option of the long party, potentially enter into the inverse transaction.
   * @return the value of the property, not null
   */
  public LongShort getLongShort() {
    return this.longShort;
  }

  //-----------------------------------------------------------------------

  /**
   * Gets the expiry date-time of the option.
   * <p>
   * The option is European, and can only be exercised on the expiry date.
   * @return the value of the property, not null
   */
  @Override public ZonedDateTime getExpiry() {
    return this.expiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the underlying foreign exchange transaction.
   * <p>
   * At expiry, if the option is in the money, this foreign exchange will occur.
   * A call option permits the transaction as specified to occur.
   * A put option permits the inverse transaction to occur.
   * @return the value of the property, not null
   */
  public ResolvedFxSingle getUnderlying() {
    return this.underlying;
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
      ResolvedFxVanillaOption other = (ResolvedFxVanillaOption) obj;
      return JodaBeanUtils.equal(this.longShort, other.longShort) &&
          JodaBeanUtils.equal(this.expiry, other.expiry) &&
          JodaBeanUtils.equal(this.underlying, other.underlying);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(this.longShort);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.expiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.underlying);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("ResolvedFxVanillaOption{");
    buf.append("longShort").append('=').append(JodaBeanUtils.toString(this.longShort)).append(',').append(' ');
    buf.append("expiry").append('=').append(JodaBeanUtils.toString(this.expiry)).append(',').append(' ');
    buf.append("underlying").append('=').append(JodaBeanUtils.toString(this.underlying));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ResolvedFxVanillaOption}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code longShort} property.
     */
    private final MetaProperty<LongShort> longShort = DirectMetaProperty.ofImmutable(
        this, "longShort", ResolvedFxVanillaOption.class, LongShort.class);
    /**
     * The meta-property for the {@code expiry} property.
     */
    private final MetaProperty<ZonedDateTime> expiry = DirectMetaProperty.ofImmutable(
        this, "expiry", ResolvedFxVanillaOption.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<ResolvedFxSingle> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", ResolvedFxVanillaOption.class, ResolvedFxSingle.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "longShort",
        "expiry",
        "underlying");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return this.longShort;
        case -1289159373:  // expiry
          return this.expiry;
        case -1770633379:  // underlying
          return this.underlying;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ResolvedFxVanillaOption.Builder builder() {
      return new ResolvedFxVanillaOption.Builder();
    }

    @Override
    public Class<? extends ResolvedFxVanillaOption> beanType() {
      return ResolvedFxVanillaOption.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return this.metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code longShort} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LongShort> longShort() {
      return this.longShort;
    }

    /**
     * The meta-property for the {@code expiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> expiry() {
      return this.expiry;
    }

    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ResolvedFxSingle> underlying() {
      return this.underlying;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return ((ResolvedFxVanillaOption) bean).getLongShort();
        case -1289159373:  // expiry
          return ((ResolvedFxVanillaOption) bean).getExpiry();
        case -1770633379:  // underlying
          return ((ResolvedFxVanillaOption) bean).getUnderlying();
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
   * The bean-builder for {@code ResolvedFxVanillaOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ResolvedFxVanillaOption> {

    private LongShort longShort;
    private ZonedDateTime expiry;
    private ResolvedFxSingle underlying;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ResolvedFxVanillaOption beanToCopy) {
      this.longShort = beanToCopy.getLongShort();
      this.expiry = beanToCopy.getExpiry();
      this.underlying = beanToCopy.getUnderlying();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          return this.longShort;
        case -1289159373:  // expiry
          return this.expiry;
        case -1770633379:  // underlying
          return this.underlying;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 116685664:  // longShort
          this.longShort = (LongShort) newValue;
          break;
        case -1289159373:  // expiry
          this.expiry = (ZonedDateTime) newValue;
          break;
        case -1770633379:  // underlying
          this.underlying = (ResolvedFxSingle) newValue;
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
    public ResolvedFxVanillaOption build() {
      return new ResolvedFxVanillaOption(
          this.longShort,
          this.expiry,
          this.underlying);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets whether the option is long or short.
     * <p>
     * At expiry, the long party will have the option to enter in this transaction;
     * the short party will, at the option of the long party, potentially enter into the inverse transaction.
     * @param longShort  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder longShort(LongShort longShort) {
      JodaBeanUtils.notNull(longShort, "longShort");
      this.longShort = longShort;
      return this;
    }

    /**
     * Sets the expiry date-time of the option.
     * <p>
     * The option is European, and can only be exercised on the expiry date.
     * @param expiry  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiry(ZonedDateTime expiry) {
      JodaBeanUtils.notNull(expiry, "expiry");
      this.expiry = expiry;
      return this;
    }

    /**
     * Sets the underlying foreign exchange transaction.
     * <p>
     * At expiry, if the option is in the money, this foreign exchange will occur.
     * A call option permits the transaction as specified to occur.
     * A put option permits the inverse transaction to occur.
     * @param underlying  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder underlying(ResolvedFxSingle underlying) {
      JodaBeanUtils.notNull(underlying, "underlying");
      this.underlying = underlying;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("ResolvedFxVanillaOption.Builder{");
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(this.longShort)).append(',').append(' ');
      buf.append("expiry").append('=').append(JodaBeanUtils.toString(this.expiry)).append(',').append(' ');
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(this.underlying));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
