package com.revolsys.swing.map.layer.dataobject.style;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.measure.Measure;
import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;

import org.springframework.core.io.Resource;

import com.revolsys.converter.string.StringConverterRegistry;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.spring.SpringUtil;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.swing.map.layer.dataobject.style.marker.AbstractMarker;
import com.revolsys.swing.map.layer.dataobject.style.marker.ImageMarker;
import com.revolsys.swing.map.layer.dataobject.style.marker.Marker;
import com.revolsys.swing.map.layer.dataobject.style.marker.ShapeMarker;
import com.revolsys.util.JavaBeanUtil;

public class MarkerStyle implements Cloneable {

  public static final AbstractMarker ELLIPSE = new ShapeMarker("ellipse");

  public static final Measure<Length> TEN_PIXELS = Measure.valueOf(10,
    NonSI.PIXEL);

  public static final Measure<Length> ZERO_PIXEL = Measure.valueOf(0,
    NonSI.PIXEL);

  public static final Measure<Length> ONE_PIXEL = Measure.valueOf(1,
    NonSI.PIXEL);

  public static Color getColorWithOpacity(final Color color, final int opacity) {
    return new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity);
  }

  public static <T> T getWithDefault(final T value, final T defaultValue) {
    if (value == null) {
      return defaultValue;
    } else {
      return value;
    }
  }

  public static MarkerStyle marker(final AbstractMarker marker,
    final double markerSize, final Color lineColor, final Color fillColor) {
    final MarkerStyle style = new MarkerStyle();
    setMarker(style, marker, markerSize, lineColor, fillColor);
    return style;
  }

  public static MarkerStyle marker(final Shape shape, final double markerSize,
    final Color lineColor, final double lineWidth, final Color fillColor) {
    final AbstractMarker marker = new ShapeMarker(shape);
    return marker(marker, markerSize, lineColor, fillColor);
  }

  public static MarkerStyle marker(final String markerName,
    final double markerSize, final Color lineColor, final double lineWidth,
    final Color fillColor) {
    final AbstractMarker marker = new ShapeMarker(markerName);
    return marker(marker, markerSize, lineColor, fillColor);
  }

  public static void setMarker(final MarkerStyle style,
    final AbstractMarker marker, final double markerSize,
    final Color lineColor, final Color fillColor) {
    style.setMarker(marker);
    style.setMarkerWidth(markerSize);
    style.setMarkerHeight(markerSize);
    style.setMarkerLineColor(lineColor);
    style.setMarkerHorizontalAlignment("center");
    style.setMarkerVerticalAlignment("middle");
    style.setMarkerFill(fillColor);
  }

  public static void setMarker(final MarkerStyle style,
    final String markerName, final double markerSize, final Color lineColor,
    final double lineWidth, final Color fillColor) {
    final AbstractMarker marker = new ShapeMarker(markerName);
    setMarker(style, marker, markerSize, lineColor, fillColor);
  }

  private String markerHorizontalAlignment = "auto";

  private String markerVerticalAlignment = "auto";

  private String markerOrientationType = "none";

  private Measure<Length> markerWidth = TEN_PIXELS;

  private Measure<Length> markerHeight = TEN_PIXELS;

  private int markerLineOpacity = 255;

  private int markerOpacity = 255;

  private int markerFillOpacity = 255;

  private double markerSmooth = 0;

  private boolean markerClip = true;

  private Measure<Length> markerDeltaX = ZERO_PIXEL;

  private Measure<Length> markerDeltaY = ZERO_PIXEL;

  private Marker marker = ELLIPSE;

  private String markerPlacement = "point";

  private String markerType = "ellipse";

  private Color markerLineColor = new Color(255, 255, 255, 255);

  private Color markerFill = new Color(0, 0, 255, 255);

  private Measure<Length> markerLineWidth = ONE_PIXEL;

  private String markerFile;

  private Resource markerFileResource;

  public MarkerStyle() {
  }

  public MarkerStyle(final Map<String, Object> style) {
    for (final Entry<String, Object> entry : style.entrySet()) {
      final String label = entry.getKey();
      Object value = entry.getValue();
      final MarkerStyleProperty markerStyleProperty = MarkerStyleProperty.getProperty(label);
      if (markerStyleProperty != null) {
        final DataType dataType = markerStyleProperty.getDataType();
        final String propertyName = markerStyleProperty.getPropertyName();
        value = StringConverterRegistry.toObject(dataType, value);
        JavaBeanUtil.setProperty(this, propertyName, value);
      } else if (label.startsWith("marker")) {
        System.out.println(label);
      }
    }
  }

  @Override
  public MarkerStyle clone() {
    try {
      return (MarkerStyle)super.clone();
    } catch (final CloneNotSupportedException e) {
      return null;
    }
  }

  public Marker getMarker() {
    return marker;
  }

  public Measure<Length> getMarkerDeltaX() {
    return markerDeltaX;
  }

  public Measure<Length> getMarkerDeltaY() {
    return markerDeltaY;
  }

  public String getMarkerFile() {
    return markerFile;
  }

  public Color getMarkerFill() {
    return markerFill;
  }

  public int getMarkerFillOpacity() {
    return markerFillOpacity;
  }

  public double getMarkerHeight() {
    return markerHeight.doubleValue(NonSI.PIXEL);
  }

  public Measure<Length> getMarkerHeightMeasure() {
    return markerHeight;
  }

  public String getMarkerHorizontalAlignment() {
    return markerHorizontalAlignment;
  }

  public Color getMarkerLineColor() {
    return markerLineColor;
  }

  public int getMarkerLineOpacity() {
    return markerLineOpacity;
  }

  public double getMarkerLineWidth() {
    return markerLineWidth.doubleValue(NonSI.PIXEL);
  }

  public Measure<Length> getMarkerLineWidthMeasure() {
    return markerLineWidth;
  }

  public int getMarkerOpacity() {
    return markerOpacity;
  }

  public String getMarkerOrientationType() {
    return markerOrientationType;
  }

  public String getMarkerPlacement() {
    return markerPlacement;
  }

  public double getMarkerSmooth() {
    return markerSmooth;
  }

  public String getMarkerType() {
    return markerType;
  }

  public String getMarkerVerticalAlignment() {
    return markerVerticalAlignment;
  }

  public double getMarkerWidth() {
    return markerWidth.doubleValue(NonSI.PIXEL);
  }

  public Measure<Length> getMarkerWidthMeasure() {
    return markerWidth;
  }

  public boolean isMarkerClip() {
    return markerClip;
  }

  public void setMarker(final Marker marker) {
    this.marker = getWithDefault(marker, ELLIPSE);
  }

  public void setMarkerClip(final boolean markerClip) {
    this.markerClip = markerClip;
  }

  public void setMarkerDeltaX(final Measure<Length> markerDx) {
    this.markerDeltaX = getWithDefault(markerDx, ZERO_PIXEL);
  }

  public void setMarkerDeltaY(final Measure<Length> markerDy) {
    this.markerDeltaY = getWithDefault(markerDy, ZERO_PIXEL);
  }

  public void setMarkerDx(final double markerDx) {
    setMarkerDeltaX(Measure.valueOf(markerDx, NonSI.PIXEL));
  }

  public void setMarkerDy(final double markerDy) {
    setMarkerDeltaY(Measure.valueOf(markerDy, NonSI.PIXEL));
  }

  public void setMarkerFile(final String markerFile) {
    this.markerFile = markerFile;
    final Pattern pattern = Pattern.compile("url\\('?([^']+)'?\\)");
    String url;
    final Matcher matcher = pattern.matcher(markerFile);
    if (matcher.find()) {
      url = matcher.group(1);
    } else {
      url = markerFile;
    }
    if (url.toUpperCase().matches("[A-Z][A-Z0-9\\+\\.\\-]*:")) {
      this.markerFileResource = SpringUtil.getUrlResource(url);
    } else {
      this.markerFileResource = SpringUtil.getBaseResource(url);
    }
    setMarker(new ImageMarker(this.markerFileResource));
  }

  public void setMarkerFill(final Color markerFill) {
    if (markerFill == null) {
      this.markerFill = new Color(128, 128, 128, markerFillOpacity);
    } else {
      this.markerFill = markerFill;
      this.markerFillOpacity = markerFill.getAlpha();
    }
  }

  public void setMarkerFillOpacity(final double markerFillOpacity) {
    if (markerFillOpacity < 0 || markerFillOpacity > 1) {
      throw new IllegalArgumentException(
        "The opacity must be between 0.0 - 1.0");
    } else {
      this.markerFillOpacity = (int)(255 * markerFillOpacity);
      this.markerFill = getColorWithOpacity(markerFill, this.markerFillOpacity);
    }
  }

  public void setMarkerFillOpacity(final int markerFillOpacity) {
    if (markerFillOpacity < 0 || markerFillOpacity > 255) {
      throw new IllegalArgumentException("The opacity must be between 0 - 255");
    } else {
      this.markerFillOpacity = markerFillOpacity;
      this.markerFill = getColorWithOpacity(markerFill, this.markerFillOpacity);
    }
  }

  public boolean setMarkerFillStyle(final Viewport2D viewport,
    final Graphics2D graphics) {
    if (markerFill.getAlpha() == 0) {
      return false;
    } else {
      graphics.setPaint(markerFill);
      return true;
    }
  }

  public void setMarkerHeight(final double markerHeight) {
    setMarkerHeightMeasure(Measure.valueOf(markerHeight, NonSI.PIXEL));
  }

  public void setMarkerHeightMeasure(final Measure<Length> markerHeight) {
    this.markerHeight = getWithDefault(markerHeight, TEN_PIXELS);
  }

  public void setMarkerHorizontalAlignment(
    final String markerHorizontalAlignment) {
    this.markerHorizontalAlignment = getWithDefault(markerHorizontalAlignment,
      "auto");
  }

  public void setMarkerLineColor(final Color markerLineColor) {
    if (markerLineColor == null) {
      this.markerLineColor = new Color(128, 128, 128, markerLineOpacity);
    } else {
      this.markerLineColor = markerLineColor;
      this.markerLineOpacity = markerLineColor.getAlpha();
    }
  }

  public void setMarkerLineOpacity(final double markerLineOpacity) {
    if (markerLineOpacity < 0 || markerLineOpacity > 1) {
      throw new IllegalArgumentException(
        "The opacity must be between 0.0 - 1.0");
    } else {
      this.markerLineOpacity = (int)(255 * markerLineOpacity);
      this.markerLineColor = getColorWithOpacity(markerLineColor,
        this.markerLineOpacity);
    }
  }

  public void setMarkerLineOpacity(final int markerLineOpacity) {
    if (markerLineOpacity < 0 || markerLineOpacity > 255) {
      throw new IllegalArgumentException("The opacity must be between 0 - 255");
    } else {
      this.markerLineOpacity = markerLineOpacity;
      this.markerLineColor = getColorWithOpacity(markerLineColor,
        this.markerLineOpacity);
    }
  }

  public boolean setMarkerLineStyle(final Viewport2D viewport,
    final Graphics2D graphics) {
    final Color color = getMarkerLineColor();
    if (color.getAlpha() == 0) {
      return false;
    } else {
      graphics.setColor(color);
      final float width = (float)Viewport2D.toDisplayValue(viewport,
        markerLineWidth);
      final BasicStroke basicStroke = new BasicStroke(width);
      graphics.setStroke(basicStroke);
      return true;
    }
  }

  public void setMarkerLineWidth(final double markerLineWidth) {
    final Measure<Length> measure = Measure.valueOf(markerLineWidth,
      NonSI.PIXEL);
    setMarkerLineWidthMeasure(measure);
  }

  public void setMarkerLineWidthMeasure(final Measure<Length> markerLineWidth) {
    this.markerLineWidth = getWithDefault(markerLineWidth, ONE_PIXEL);
  }

  public void setMarkerOpacity(final double markerOpacity) {
    if (markerLineOpacity < 0 || markerLineOpacity > 1) {
      throw new IllegalArgumentException(
        "The opacity must be between 0.0 - 1.0");
    } else {
      this.markerOpacity = (int)(255 * markerOpacity);
      setMarkerLineOpacity(markerOpacity);
      setMarkerFillOpacity(markerOpacity);
    }
  }

  public void setMarkerOpacity(final int markerOpacity) {
    if (markerOpacity < 0 || markerOpacity > 255) {
      throw new IllegalArgumentException("The opacity must be between 0 - 255");
    } else {
      this.markerOpacity = markerOpacity;
      setMarkerLineOpacity(markerOpacity);
      setMarkerFillOpacity(markerOpacity);
    }
  }

  public void setMarkerOrientationType(final String markerOrientationType) {
    this.markerOrientationType = getWithDefault(markerOrientationType, "none");
  }

  public void setMarkerPlacement(final String markerPlacement) {
    this.markerPlacement = getWithDefault(markerPlacement, "point");
  }

  public void setMarkerSmooth(final double markerSmooth) {
    this.markerSmooth = markerSmooth;
  }

  public void setMarkerType(final String markerType) {
    this.markerType = getWithDefault(markerType, "ellipse");
    setMarker(new ShapeMarker(this.markerType));
  }

  public void setMarkerVerticalAlignment(final String markerVerticalAlignment) {
    this.markerVerticalAlignment = getWithDefault(markerVerticalAlignment,
      "auto");
  }

  public void setMarkerWidth(final double markerWidth) {
    setMarkerWidthMeasure(Measure.valueOf(markerWidth, NonSI.PIXEL));
  }

  public void setMarkerWidthMeasure(final Measure<Length> markerWidth) {
    this.markerWidth = getWithDefault(markerWidth, TEN_PIXELS);
  }

}
