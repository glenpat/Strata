/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.impl.direct.DirectPrivateBeanBuilder;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Digital option function.
 */
@BeanDefinition(builderScope = "private")
public final class DigitalOptionFunction
    implements OptionFunction, ImmutableBean, Serializable {

  /**
   * The strike value.
   */
  @PropertyDefinition
  private final double strike;
  /**
   * The time to expiry.
   */
  @PropertyDefinition(overrideGet = true)
  private final double timeToExpiry;
  /**
   * The sign.
   * <p>
   * The sign is +1 for above and -1 for below.
   */
  @PropertyDefinition
  private final double sign;

  /**
   * The number of time steps.
   */
  @PropertyDefinition(overrideGet = true)
  private final int numberOfSteps;

  //-------------------------------------------------------------------------

  /**
   * Obtains an instance.
   *
   * @param strike  the strike
   * @param timeToExpiry  the time to expiry
   * @param barrierType  down or up
   * @param knockType
   * @param numberOfSteps  the number of time steps
   * @return the instance
   */
  public static DigitalOptionFunction of(double strike, double timeToExpiry, BarrierType barrierType,
      KnockType knockType, int numberOfSteps) {
    double sign;
    switch (knockType) {
      case KNOCK_IN:
        sign = barrierType.isDown() ? -1d : 1d;
        break;
      case KNOCK_OUT:
      default:
        sign = barrierType.isDown() ? 1d : -1d;
        break;
    }

    ArgChecker.isTrue(numberOfSteps > 0, "the number of steps should be positive");
    return new DigitalOptionFunction(strike, timeToExpiry, sign, numberOfSteps);
  }

  //-------------------------------------------------------------------------
  @Override
  public DoubleArray getPayoffAtExpiryTrinomial(DoubleArray stateValue) {
    int nNodes = stateValue.size();
    double[] values = new double[nNodes];
    for (int i = 0; i < nNodes; ++i) {
      final double value = stateValue.get(i);
      //  TODO: need to determine if greater than or greater than or equal
      if ((this.sign > 0 && value > this.strike)
          ||
          (this.sign < 0 && value < this.strike)) {
        values[i] = 1D;
      } else {
        values[i] = 0D;
      }
    }
    return DoubleArray.ofUnsafe(values);
  }
  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code DigitalOptionFunction}.
   * @return the meta-bean, not null
   */
  public static DigitalOptionFunction.Meta meta() {
    return DigitalOptionFunction.Meta.INSTANCE;
  }

  static {
    MetaBean.register(DigitalOptionFunction.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private DigitalOptionFunction(
      double strike,
      double timeToExpiry,
      double sign,
      int numberOfSteps) {
    this.strike = strike;
    this.timeToExpiry = timeToExpiry;
    this.sign = sign;
    this.numberOfSteps = numberOfSteps;
  }

  @Override
  public DigitalOptionFunction.Meta metaBean() {
    return DigitalOptionFunction.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strike value.
   * @return the value of the property
   */
  public double getStrike() {
    return strike;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time to expiry.
   * @return the value of the property
   */
  @Override
  public double getTimeToExpiry() {
    return timeToExpiry;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sign.
   * <p>
   * The sign is +1 for above and -1 for below.
   * @return the value of the property
   */
  public double getSign() {
    return sign;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the number of time steps.
   * @return the value of the property
   */
  @Override
  public int getNumberOfSteps() {
    return numberOfSteps;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      DigitalOptionFunction other = (DigitalOptionFunction) obj;
      return JodaBeanUtils.equal(strike, other.strike) &&
          JodaBeanUtils.equal(timeToExpiry, other.timeToExpiry) &&
          JodaBeanUtils.equal(sign, other.sign) &&
          (numberOfSteps == other.numberOfSteps);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(strike);
    hash = hash * 31 + JodaBeanUtils.hashCode(timeToExpiry);
    hash = hash * 31 + JodaBeanUtils.hashCode(sign);
    hash = hash * 31 + JodaBeanUtils.hashCode(numberOfSteps);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("DigitalOptionFunction{");
    buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
    buf.append("timeToExpiry").append('=').append(JodaBeanUtils.toString(timeToExpiry)).append(',').append(' ');
    buf.append("sign").append('=').append(JodaBeanUtils.toString(sign)).append(',').append(' ');
    buf.append("numberOfSteps").append('=').append(JodaBeanUtils.toString(numberOfSteps));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code DigitalOptionFunction}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code strike} property.
     */
    private final MetaProperty<Double> strike = DirectMetaProperty.ofImmutable(
        this, "strike", DigitalOptionFunction.class, Double.TYPE);
    /**
     * The meta-property for the {@code timeToExpiry} property.
     */
    private final MetaProperty<Double> timeToExpiry = DirectMetaProperty.ofImmutable(
        this, "timeToExpiry", DigitalOptionFunction.class, Double.TYPE);
    /**
     * The meta-property for the {@code sign} property.
     */
    private final MetaProperty<Double> sign = DirectMetaProperty.ofImmutable(
        this, "sign", DigitalOptionFunction.class, Double.TYPE);
    /**
     * The meta-property for the {@code numberOfSteps} property.
     */
    private final MetaProperty<Integer> numberOfSteps = DirectMetaProperty.ofImmutable(
        this, "numberOfSteps", DigitalOptionFunction.class, Integer.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "strike",
        "timeToExpiry",
        "sign",
        "numberOfSteps");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -891985998:  // strike
          return strike;
        case -1831499397:  // timeToExpiry
          return timeToExpiry;
        case 3530173:  // sign
          return sign;
        case -1323103225:  // numberOfSteps
          return numberOfSteps;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends DigitalOptionFunction> builder() {
      return new DigitalOptionFunction.Builder();
    }

    @Override
    public Class<? extends DigitalOptionFunction> beanType() {
      return DigitalOptionFunction.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code strike} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strike() {
      return strike;
    }

    /**
     * The meta-property for the {@code timeToExpiry} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> timeToExpiry() {
      return timeToExpiry;
    }

    /**
     * The meta-property for the {@code sign} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> sign() {
      return sign;
    }

    /**
     * The meta-property for the {@code numberOfSteps} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Integer> numberOfSteps() {
      return numberOfSteps;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -891985998:  // strike
          return ((DigitalOptionFunction) bean).getStrike();
        case -1831499397:  // timeToExpiry
          return ((DigitalOptionFunction) bean).getTimeToExpiry();
        case 3530173:  // sign
          return ((DigitalOptionFunction) bean).getSign();
        case -1323103225:  // numberOfSteps
          return ((DigitalOptionFunction) bean).getNumberOfSteps();
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
   * The bean-builder for {@code DigitalOptionFunction}.
   */
  private static final class Builder extends DirectPrivateBeanBuilder<DigitalOptionFunction> {

    private double strike;
    private double timeToExpiry;
    private double sign;
    private int numberOfSteps;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -891985998:  // strike
          return strike;
        case -1831499397:  // timeToExpiry
          return timeToExpiry;
        case 3530173:  // sign
          return sign;
        case -1323103225:  // numberOfSteps
          return numberOfSteps;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -891985998:  // strike
          this.strike = (Double) newValue;
          break;
        case -1831499397:  // timeToExpiry
          this.timeToExpiry = (Double) newValue;
          break;
        case 3530173:  // sign
          this.sign = (Double) newValue;
          break;
        case -1323103225:  // numberOfSteps
          this.numberOfSteps = (Integer) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public DigitalOptionFunction build() {
      return new DigitalOptionFunction(
          strike,
          timeToExpiry,
          sign,
          numberOfSteps);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("DigitalOptionFunction.Builder{");
      buf.append("strike").append('=').append(JodaBeanUtils.toString(strike)).append(',').append(' ');
      buf.append("timeToExpiry").append('=').append(JodaBeanUtils.toString(timeToExpiry)).append(',').append(' ');
      buf.append("sign").append('=').append(JodaBeanUtils.toString(sign)).append(',').append(' ');
      buf.append("numberOfSteps").append('=').append(JodaBeanUtils.toString(numberOfSteps));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}