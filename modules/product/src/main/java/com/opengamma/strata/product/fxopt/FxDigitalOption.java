package com.opengamma.strata.product.fxopt;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.Resolvable;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.etd.EtdOptionType;
import com.opengamma.strata.product.fx.FxOption;
import com.opengamma.strata.product.option.BarrierType;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.opengamma.strata.collect.ArgChecker.isEqual;

/**
 *  Digital payout in counter currency
 */
@BeanDefinition(constructorScope = "package")
public final class FxDigitalOption
    implements FxOption, Resolvable<ResolvedFxDigitalOption>, ImmutableBean, Serializable {

  @PropertyDefinition(validate = "notNull")
  private final BarrierType barrierType;

  @PropertyDefinition(validate = "notNull")
  private final EtdOptionType optionType;

  @PropertyDefinition(validate = "ArgChecker.notNegativeOrNaN")
  private final double strikePrice;

  @PropertyDefinition(validate = "notNull")
  private final LocalDate expiryDate;

  @PropertyDefinition(validate = "notNull")
  private final LocalTime expiryTime;

  @PropertyDefinition(validate = "notNull")
  private final ZoneId expiryZone;

  @PropertyDefinition(validate = "notNull")
  private final FxIndex index;

  /**
   * if long, holder receives digital payment
   * if short, holder pays digital payment
   */
  @Deprecated
  @PropertyDefinition(validate = "notNull")
  private final LongShort longShort;

  /**
   * positive if long, negative if short
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount payment;

  @Override
  public CurrencyPair getCurrencyPair() {
    return this.index.getCurrencyPair();
  }

  /**
   * Gets the expiry date-time.
   * <p>
   * The option expires at this date and time.
   * <p>
   * The result is returned by combining the expiry date, time and time-zone.
   *
   * @return the expiry date and time
   */
  public ZonedDateTime getExpiry() {
    return this.expiryDate.atTime(this.expiryTime).atZone(this.expiryZone);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    //  make sure the payment currency matches the index counter currency (to be safe)
    isEqual(this.payment.getCurrency(), this.index.getCurrencyPair().getCounter(), "payment.currency",
        "index.counterCurrency");
  }

  //-------------------------------------------------------------------------
  @Override
  public ResolvedFxDigitalOption resolve(final ReferenceData refData) {
    return ResolvedFxDigitalOption.builder()
        .barrierType(this.barrierType)
        .optionType(this.optionType)
        .strikePrice(this.strikePrice)
        .expiry(getExpiry())
        .index(this.index)
        .longShort(this.longShort)
        .payment(this.payment)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FxDigitalOption}.
   * @return the meta-bean, not null
   */
  public static FxDigitalOption.Meta meta() {
    return FxDigitalOption.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FxDigitalOption.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FxDigitalOption.Builder builder() {
    return new FxDigitalOption.Builder();
  }

  /**
   * Creates an instance.
   * @param barrierType  the value of the property, not null
   * @param optionType  the value of the property, not null
   * @param strikePrice  the value of the property
   * @param expiryDate  the value of the property, not null
   * @param expiryTime  the value of the property, not null
   * @param expiryZone  the value of the property, not null
   * @param index  the value of the property, not null
   * @param longShort  the value of the property, not null
   * @param payment  the value of the property, not null
   */
  FxDigitalOption(
      BarrierType barrierType,
      EtdOptionType optionType,
      double strikePrice,
      LocalDate expiryDate,
      LocalTime expiryTime,
      ZoneId expiryZone,
      FxIndex index,
      LongShort longShort,
      CurrencyAmount payment) {
    JodaBeanUtils.notNull(barrierType, "barrierType");
    JodaBeanUtils.notNull(optionType, "optionType");
    ArgChecker.notNegativeOrNaN(strikePrice, "strikePrice");
    JodaBeanUtils.notNull(expiryDate, "expiryDate");
    JodaBeanUtils.notNull(expiryTime, "expiryTime");
    JodaBeanUtils.notNull(expiryZone, "expiryZone");
    JodaBeanUtils.notNull(index, "index");
    JodaBeanUtils.notNull(longShort, "longShort");
    JodaBeanUtils.notNull(payment, "payment");
    this.barrierType = barrierType;
    this.optionType = optionType;
    this.strikePrice = strikePrice;
    this.expiryDate = expiryDate;
    this.expiryTime = expiryTime;
    this.expiryZone = expiryZone;
    this.index = index;
    this.longShort = longShort;
    this.payment = payment;
    validate();
  }

  @Override
  public FxDigitalOption.Meta metaBean() {
    return FxDigitalOption.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the barrierType.
   * @return the value of the property, not null
   */
  public BarrierType getBarrierType() {
    return this.barrierType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the optionType.
   * @return the value of the property, not null
   */
  public EtdOptionType getOptionType() {
    return this.optionType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the strikePrice.
   * @return the value of the property
   */
  public double getStrikePrice() {
    return this.strikePrice;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiryDate.
   * @return the value of the property, not null
   */
  public LocalDate getExpiryDate() {
    return this.expiryDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiryTime.
   * @return the value of the property, not null
   */
  public LocalTime getExpiryTime() {
    return this.expiryTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expiryZone.
   * @return the value of the property, not null
   */
  public ZoneId getExpiryZone() {
    return this.expiryZone;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the index.
   * @return the value of the property, not null
   */
  public FxIndex getIndex() {
    return this.index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets if long, holder receives digital payment
   * if short, holder pays digital payment
   * @return the value of the property, not null
   */
  public LongShort getLongShort() {
    return this.longShort;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets positive if long, negative if short
   * @return the value of the property, not null
   */
  public CurrencyAmount getPayment() {
    return this.payment;
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
      FxDigitalOption other = (FxDigitalOption) obj;
      return JodaBeanUtils.equal(this.barrierType, other.barrierType) &&
          JodaBeanUtils.equal(this.optionType, other.optionType) &&
          JodaBeanUtils.equal(this.strikePrice, other.strikePrice) &&
          JodaBeanUtils.equal(this.expiryDate, other.expiryDate) &&
          JodaBeanUtils.equal(this.expiryTime, other.expiryTime) &&
          JodaBeanUtils.equal(this.expiryZone, other.expiryZone) &&
          JodaBeanUtils.equal(this.index, other.index) &&
          JodaBeanUtils.equal(this.longShort, other.longShort) &&
          JodaBeanUtils.equal(this.payment, other.payment);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(this.barrierType);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.optionType);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.strikePrice);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.expiryDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.expiryTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.expiryZone);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.index);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.longShort);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.payment);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(320);
    buf.append("FxDigitalOption{");
    buf.append("barrierType").append('=').append(JodaBeanUtils.toString(this.barrierType)).append(',').append(' ');
    buf.append("optionType").append('=').append(JodaBeanUtils.toString(this.optionType)).append(',').append(' ');
    buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(this.strikePrice)).append(',').append(' ');
    buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(this.expiryDate)).append(',').append(' ');
    buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(this.expiryTime)).append(',').append(' ');
    buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(this.expiryZone)).append(',').append(' ');
    buf.append("index").append('=').append(JodaBeanUtils.toString(this.index)).append(',').append(' ');
    buf.append("longShort").append('=').append(JodaBeanUtils.toString(this.longShort)).append(',').append(' ');
    buf.append("payment").append('=').append(JodaBeanUtils.toString(this.payment));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FxDigitalOption}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code barrierType} property.
     */
    private final MetaProperty<BarrierType> barrierType = DirectMetaProperty.ofImmutable(
        this, "barrierType", FxDigitalOption.class, BarrierType.class);
    /**
     * The meta-property for the {@code optionType} property.
     */
    private final MetaProperty<EtdOptionType> optionType = DirectMetaProperty.ofImmutable(
        this, "optionType", FxDigitalOption.class, EtdOptionType.class);
    /**
     * The meta-property for the {@code strikePrice} property.
     */
    private final MetaProperty<Double> strikePrice = DirectMetaProperty.ofImmutable(
        this, "strikePrice", FxDigitalOption.class, Double.TYPE);
    /**
     * The meta-property for the {@code expiryDate} property.
     */
    private final MetaProperty<LocalDate> expiryDate = DirectMetaProperty.ofImmutable(
        this, "expiryDate", FxDigitalOption.class, LocalDate.class);
    /**
     * The meta-property for the {@code expiryTime} property.
     */
    private final MetaProperty<LocalTime> expiryTime = DirectMetaProperty.ofImmutable(
        this, "expiryTime", FxDigitalOption.class, LocalTime.class);
    /**
     * The meta-property for the {@code expiryZone} property.
     */
    private final MetaProperty<ZoneId> expiryZone = DirectMetaProperty.ofImmutable(
        this, "expiryZone", FxDigitalOption.class, ZoneId.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<FxIndex> index = DirectMetaProperty.ofImmutable(
        this, "index", FxDigitalOption.class, FxIndex.class);
    /**
     * The meta-property for the {@code longShort} property.
     */
    private final MetaProperty<LongShort> longShort = DirectMetaProperty.ofImmutable(
        this, "longShort", FxDigitalOption.class, LongShort.class);
    /**
     * The meta-property for the {@code payment} property.
     */
    private final MetaProperty<CurrencyAmount> payment = DirectMetaProperty.ofImmutable(
        this, "payment", FxDigitalOption.class, CurrencyAmount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "barrierType",
        "optionType",
        "strikePrice",
        "expiryDate",
        "expiryTime",
        "expiryZone",
        "index",
        "longShort",
        "payment");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1029043089:  // barrierType
          return this.barrierType;
        case 1373587791:  // optionType
          return this.optionType;
        case 50946231:  // strikePrice
          return this.strikePrice;
        case -816738431:  // expiryDate
          return this.expiryDate;
        case -816254304:  // expiryTime
          return this.expiryTime;
        case -816069761:  // expiryZone
          return this.expiryZone;
        case 100346066:  // index
          return this.index;
        case 116685664:  // longShort
          return this.longShort;
        case -786681338:  // payment
          return this.payment;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FxDigitalOption.Builder builder() {
      return new FxDigitalOption.Builder();
    }

    @Override
    public Class<? extends FxDigitalOption> beanType() {
      return FxDigitalOption.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return this.metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code barrierType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BarrierType> barrierType() {
      return this.barrierType;
    }

    /**
     * The meta-property for the {@code optionType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<EtdOptionType> optionType() {
      return this.optionType;
    }

    /**
     * The meta-property for the {@code strikePrice} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> strikePrice() {
      return this.strikePrice;
    }

    /**
     * The meta-property for the {@code expiryDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> expiryDate() {
      return this.expiryDate;
    }

    /**
     * The meta-property for the {@code expiryTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalTime> expiryTime() {
      return this.expiryTime;
    }

    /**
     * The meta-property for the {@code expiryZone} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZoneId> expiryZone() {
      return this.expiryZone;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxIndex> index() {
      return this.index;
    }

    /**
     * The meta-property for the {@code longShort} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LongShort> longShort() {
      return this.longShort;
    }

    /**
     * The meta-property for the {@code payment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> payment() {
      return this.payment;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1029043089:  // barrierType
          return ((FxDigitalOption) bean).getBarrierType();
        case 1373587791:  // optionType
          return ((FxDigitalOption) bean).getOptionType();
        case 50946231:  // strikePrice
          return ((FxDigitalOption) bean).getStrikePrice();
        case -816738431:  // expiryDate
          return ((FxDigitalOption) bean).getExpiryDate();
        case -816254304:  // expiryTime
          return ((FxDigitalOption) bean).getExpiryTime();
        case -816069761:  // expiryZone
          return ((FxDigitalOption) bean).getExpiryZone();
        case 100346066:  // index
          return ((FxDigitalOption) bean).getIndex();
        case 116685664:  // longShort
          return ((FxDigitalOption) bean).getLongShort();
        case -786681338:  // payment
          return ((FxDigitalOption) bean).getPayment();
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
   * The bean-builder for {@code FxDigitalOption}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FxDigitalOption> {

    private BarrierType barrierType;
    private EtdOptionType optionType;
    private double strikePrice;
    private LocalDate expiryDate;
    private LocalTime expiryTime;
    private ZoneId expiryZone;
    private FxIndex index;
    private LongShort longShort;
    private CurrencyAmount payment;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FxDigitalOption beanToCopy) {
      this.barrierType = beanToCopy.getBarrierType();
      this.optionType = beanToCopy.getOptionType();
      this.strikePrice = beanToCopy.getStrikePrice();
      this.expiryDate = beanToCopy.getExpiryDate();
      this.expiryTime = beanToCopy.getExpiryTime();
      this.expiryZone = beanToCopy.getExpiryZone();
      this.index = beanToCopy.getIndex();
      this.longShort = beanToCopy.getLongShort();
      this.payment = beanToCopy.getPayment();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1029043089:  // barrierType
          return this.barrierType;
        case 1373587791:  // optionType
          return this.optionType;
        case 50946231:  // strikePrice
          return this.strikePrice;
        case -816738431:  // expiryDate
          return this.expiryDate;
        case -816254304:  // expiryTime
          return this.expiryTime;
        case -816069761:  // expiryZone
          return this.expiryZone;
        case 100346066:  // index
          return this.index;
        case 116685664:  // longShort
          return this.longShort;
        case -786681338:  // payment
          return this.payment;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1029043089:  // barrierType
          this.barrierType = (BarrierType) newValue;
          break;
        case 1373587791:  // optionType
          this.optionType = (EtdOptionType) newValue;
          break;
        case 50946231:  // strikePrice
          this.strikePrice = (Double) newValue;
          break;
        case -816738431:  // expiryDate
          this.expiryDate = (LocalDate) newValue;
          break;
        case -816254304:  // expiryTime
          this.expiryTime = (LocalTime) newValue;
          break;
        case -816069761:  // expiryZone
          this.expiryZone = (ZoneId) newValue;
          break;
        case 100346066:  // index
          this.index = (FxIndex) newValue;
          break;
        case 116685664:  // longShort
          this.longShort = (LongShort) newValue;
          break;
        case -786681338:  // payment
          this.payment = (CurrencyAmount) newValue;
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
    public FxDigitalOption build() {
      return new FxDigitalOption(
          this.barrierType,
          this.optionType,
          this.strikePrice,
          this.expiryDate,
          this.expiryTime,
          this.expiryZone,
          this.index,
          this.longShort,
          this.payment);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the barrierType.
     * @param barrierType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder barrierType(BarrierType barrierType) {
      JodaBeanUtils.notNull(barrierType, "barrierType");
      this.barrierType = barrierType;
      return this;
    }

    /**
     * Sets the optionType.
     * @param optionType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder optionType(EtdOptionType optionType) {
      JodaBeanUtils.notNull(optionType, "optionType");
      this.optionType = optionType;
      return this;
    }

    /**
     * Sets the strikePrice.
     * @param strikePrice  the new value
     * @return this, for chaining, not null
     */
    public Builder strikePrice(double strikePrice) {
      ArgChecker.notNegativeOrNaN(strikePrice, "strikePrice");
      this.strikePrice = strikePrice;
      return this;
    }

    /**
     * Sets the expiryDate.
     * @param expiryDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryDate(LocalDate expiryDate) {
      JodaBeanUtils.notNull(expiryDate, "expiryDate");
      this.expiryDate = expiryDate;
      return this;
    }

    /**
     * Sets the expiryTime.
     * @param expiryTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryTime(LocalTime expiryTime) {
      JodaBeanUtils.notNull(expiryTime, "expiryTime");
      this.expiryTime = expiryTime;
      return this;
    }

    /**
     * Sets the expiryZone.
     * @param expiryZone  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expiryZone(ZoneId expiryZone) {
      JodaBeanUtils.notNull(expiryZone, "expiryZone");
      this.expiryZone = expiryZone;
      return this;
    }

    /**
     * Sets the index.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(FxIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets if long, holder receives digital payment
     * if short, holder pays digital payment
     * @param longShort  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder longShort(LongShort longShort) {
      JodaBeanUtils.notNull(longShort, "longShort");
      this.longShort = longShort;
      return this;
    }

    /**
     * Sets positive if long, negative if short
     * @param payment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder payment(CurrencyAmount payment) {
      JodaBeanUtils.notNull(payment, "payment");
      this.payment = payment;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(320);
      buf.append("FxDigitalOption.Builder{");
      buf.append("barrierType").append('=').append(JodaBeanUtils.toString(this.barrierType)).append(',').append(' ');
      buf.append("optionType").append('=').append(JodaBeanUtils.toString(this.optionType)).append(',').append(' ');
      buf.append("strikePrice").append('=').append(JodaBeanUtils.toString(this.strikePrice)).append(',').append(' ');
      buf.append("expiryDate").append('=').append(JodaBeanUtils.toString(this.expiryDate)).append(',').append(' ');
      buf.append("expiryTime").append('=').append(JodaBeanUtils.toString(this.expiryTime)).append(',').append(' ');
      buf.append("expiryZone").append('=').append(JodaBeanUtils.toString(this.expiryZone)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(this.index)).append(',').append(' ');
      buf.append("longShort").append('=').append(JodaBeanUtils.toString(this.longShort)).append(',').append(' ');
      buf.append("payment").append('=').append(JodaBeanUtils.toString(this.payment));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
