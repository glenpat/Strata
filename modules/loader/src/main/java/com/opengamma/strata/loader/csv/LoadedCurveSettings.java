/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurveDefinition;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.param.LabelDateParameterMetadata;
import com.opengamma.strata.market.param.ParameterMetadata;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents curve settings, used when loading curves.
 * <p>
 * This contains settings that apply across all instances of a particular curve.
 */
@BeanDefinition(style = "light")
final class LoadedCurveSettings
    implements ImmutableBean {

  /**
   * The curve name.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveName curveName;
  /**
   * The x-value type.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueType xValueType;
  /**
   * The y-value type.
   */
  @PropertyDefinition(validate = "notNull")
  private final ValueType yValueType;
  /**
   * The day count convention.
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;
  /**
   * The interpolator.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveInterpolator interpolator;
  /**
   * The extrapolator used to find points to the left of the leftmost point on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorLeft;
  /**
   * The extrapolator used to find points to the right of the rightmost point on the curve.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurveExtrapolator extrapolatorRight;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance.
   * 
   * @param curveName  the curve name
   * @param yValueType  the value type
   * @param dayCount  the day count
   * @param interpolator  the interpolator
   * @param extrapolatorLeft  the left extrapolator
   * @param extrapolatorRight  the right extrapolator
   * @return the curve settings
   */
  static LoadedCurveSettings of(
      CurveName curveName,
      ValueType xValueType,
      ValueType yValueType,
      DayCount dayCount,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight) {

    return new LoadedCurveSettings(
        curveName, xValueType, yValueType, dayCount, interpolator, extrapolatorLeft, extrapolatorRight);
  }

  //-------------------------------------------------------------------------
  // constructs an interpolated nodal curve
  InterpolatedNodalCurve createCurve(LocalDate date, List<LoadedCurveNode> curveNodes) {
    // copy and sort
    List<LoadedCurveNode> nodes = new ArrayList<>(curveNodes);
    nodes.sort(Comparator.naturalOrder());

    // build each node
    double[] xValues = new double[nodes.size()];
    double[] yValues = new double[nodes.size()];
    List<ParameterMetadata> pointsMetadata = new ArrayList<>(nodes.size());
    for (int i = 0; i < nodes.size(); i++) {
      LoadedCurveNode point = nodes.get(i);
      double yearFraction = this.dayCount.yearFraction(date, point.getDate());
      xValues[i] = yearFraction;
      yValues[i] = point.getValue();
      ParameterMetadata pointMetadata = LabelDateParameterMetadata.of(point.getDate(), point.getLabel());
      pointsMetadata.add(pointMetadata);
    }

    // create metadata
    CurveMetadata curveMetadata = DefaultCurveMetadata.builder()
        .curveName(this.curveName)
        .xValueType(this.xValueType)
        .yValueType(this.yValueType)
        .dayCount(this.dayCount)
        .parameterMetadata(pointsMetadata)
        .build();
    return InterpolatedNodalCurve.builder()
        .metadata(curveMetadata)
        .xValues(DoubleArray.copyOf(xValues))
        .yValues(DoubleArray.copyOf(yValues))
        .interpolator(this.interpolator)
        .extrapolatorLeft(this.extrapolatorLeft)
        .extrapolatorRight(this.extrapolatorRight)
        .build();
  }

  // constructs an interpolated nodal curve definition
  InterpolatedNodalCurveDefinition createCurveDefinition(List<CurveNode> nodes) {
    return InterpolatedNodalCurveDefinition.builder()
        .name(this.curveName)
        .xValueType(this.xValueType)
        .yValueType(this.yValueType)
        .dayCount(this.dayCount)
        .nodes(nodes)
        .interpolator(this.interpolator)
        .extrapolatorLeft(this.extrapolatorLeft)
        .extrapolatorRight(this.extrapolatorRight)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code LoadedCurveSettings}.
   */
  private static final TypedMetaBean<LoadedCurveSettings> META_BEAN =
      LightMetaBean.of(
          LoadedCurveSettings.class,
          MethodHandles.lookup(),
          new String[] {
              "curveName",
              "xValueType",
              "yValueType",
              "dayCount",
              "interpolator",
              "extrapolatorLeft",
              "extrapolatorRight"},
          new Object[0]);

  /**
   * The meta-bean for {@code LoadedCurveSettings}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<LoadedCurveSettings> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  private LoadedCurveSettings(
      CurveName curveName,
      ValueType xValueType,
      ValueType yValueType,
      DayCount dayCount,
      CurveInterpolator interpolator,
      CurveExtrapolator extrapolatorLeft,
      CurveExtrapolator extrapolatorRight) {
    JodaBeanUtils.notNull(curveName, "curveName");
    JodaBeanUtils.notNull(xValueType, "xValueType");
    JodaBeanUtils.notNull(yValueType, "yValueType");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(interpolator, "interpolator");
    JodaBeanUtils.notNull(extrapolatorLeft, "extrapolatorLeft");
    JodaBeanUtils.notNull(extrapolatorRight, "extrapolatorRight");
    this.curveName = curveName;
    this.xValueType = xValueType;
    this.yValueType = yValueType;
    this.dayCount = dayCount;
    this.interpolator = interpolator;
    this.extrapolatorLeft = extrapolatorLeft;
    this.extrapolatorRight = extrapolatorRight;
  }

  @Override
  public TypedMetaBean<LoadedCurveSettings> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the curve name.
   * @return the value of the property, not null
   */
  public CurveName getCurveName() {
    return this.curveName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the x-value type.
   * @return the value of the property, not null
   */
  public ValueType getXValueType() {
    return this.xValueType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the y-value type.
   * @return the value of the property, not null
   */
  public ValueType getYValueType() {
    return this.yValueType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return this.dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interpolator.
   * @return the value of the property, not null
   */
  public CurveInterpolator getInterpolator() {
    return this.interpolator;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator used to find points to the left of the leftmost point on the curve.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorLeft() {
    return this.extrapolatorLeft;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the extrapolator used to find points to the right of the rightmost point on the curve.
   * @return the value of the property, not null
   */
  public CurveExtrapolator getExtrapolatorRight() {
    return this.extrapolatorRight;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      LoadedCurveSettings other = (LoadedCurveSettings) obj;
      return JodaBeanUtils.equal(this.curveName, other.curveName) &&
          JodaBeanUtils.equal(this.xValueType, other.xValueType) &&
          JodaBeanUtils.equal(this.yValueType, other.yValueType) &&
          JodaBeanUtils.equal(this.dayCount, other.dayCount) &&
          JodaBeanUtils.equal(this.interpolator, other.interpolator) &&
          JodaBeanUtils.equal(this.extrapolatorLeft, other.extrapolatorLeft) &&
          JodaBeanUtils.equal(this.extrapolatorRight, other.extrapolatorRight);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(this.curveName);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.xValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.yValueType);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.interpolator);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.extrapolatorLeft);
    hash = hash * 31 + JodaBeanUtils.hashCode(this.extrapolatorRight);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(256);
    buf.append("LoadedCurveSettings{");
    buf.append("curveName").append('=').append(JodaBeanUtils.toString(this.curveName)).append(',').append(' ');
    buf.append("xValueType").append('=').append(JodaBeanUtils.toString(this.xValueType)).append(',').append(' ');
    buf.append("yValueType").append('=').append(JodaBeanUtils.toString(this.yValueType)).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(this.dayCount)).append(',').append(' ');
    buf.append("interpolator").append('=').append(JodaBeanUtils.toString(this.interpolator)).append(',').append(' ');
    buf.append("extrapolatorLeft").append('=').append(JodaBeanUtils.toString(this.extrapolatorLeft)).append(',').append(' ');
    buf.append("extrapolatorRight").append('=').append(JodaBeanUtils.toString(this.extrapolatorRight));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
