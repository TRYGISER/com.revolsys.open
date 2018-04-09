package com.revolsys.swing.map.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Angle;
import javax.measure.quantity.Length;
import javax.swing.border.AbstractBorder;

import com.revolsys.geometry.cs.CoordinateSystem;
import com.revolsys.geometry.cs.GeographicCoordinateSystem;
import com.revolsys.geometry.cs.unit.Degree;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.segment.LineSegment;
import com.revolsys.geometry.model.segment.LineSegmentDoubleGF;
import com.revolsys.swing.map.Viewport2D;
import com.revolsys.util.Property;

import tec.uom.se.quantity.Quantities;

public class MapRulerBorder extends AbstractBorder {

  private static final double[] GEOGRAPHICS_STEPS = {
    30, 10, 1, 1e-1, 1e-2, 1e-3, 1e-4, 1e-5, 1e-6, 1e-7, 1e-8, 1e-9, 1e-10, 1e-11, 1e-12, 1e-13,
    1e-14, 1e-15
  };

  private static final double[] STEPS = {
    1e8, 1e7, 1e6, 1e5, 1e4, 1e3, 1e2, 1e1, 1, 1e-1, 1e-2, 1e-3, 1e-4, 1e-5, 1e-6, 1e-7, 1e-8, 1e-9,
    1e-10, 1e-11, 1e-12, 1e-13, 1e-14, 1e-15
  };

  /**
   *
   */
  private static final long serialVersionUID = -3070841484052913548L;

  /**
   * Construct a new list of steps in measurable units from the double array.
   *
   * @param <U> The type of unit (e.g. {@link Angle} or {@link Length}).
   * @param unit The unit of measure.
   * @param steps The list of steps.
   * @return The list of step measures.
   */
  public static <U extends Quantity<U>> List<Quantity<U>> newSteps(final Unit<U> unit,
    final double... steps) {
    final List<Quantity<U>> stepList = new ArrayList<>();
    for (final double step : steps) {
      stepList.add(Quantities.getQuantity(step, unit));
    }
    return stepList;
  }

  private double areaMaxX;

  private double areaMaxY;

  private double areaMinX;

  private double areaMinY;

  @SuppressWarnings("rawtypes")
  private Unit baseUnit;

  private int labelHeight;

  private CoordinateSystem rulerCoordinateSystem;

  private GeometryFactory rulerGeometryFactory;

  private final int rulerSize = 25;

  private final Viewport2D viewport;

  private double unitsPerPixel;

  private int stepLevel;

  private double step;

  private double[] steps = STEPS;

  private String unitLabel;

  public MapRulerBorder(final Viewport2D viewport) {
    this.viewport = viewport;
    final GeometryFactory geometryFactory = viewport.getGeometryFactory();
    setRulerGeometryFactory(geometryFactory);
    Property.addListenerNewValue(viewport, "geometryFactory", this::setRulerGeometryFactory);
    Property.addListenerNewValue(viewport, "unitsPerPixel", this::setUnitsPerPixel);
  }

  private <Q extends Quantity<Q>> void drawLabel(final Graphics2D graphics, final int textX,
    final int textY, final double displayValue, final double stepSize) {
    DecimalFormat format;
    if (displayValue - Math.floor(displayValue) == 0) {
      format = new DecimalFormat("#,###,###,###");
    } else {
      final StringBuilder formatString = new StringBuilder("#,###,###,###.");
      final int numZeros = (int)Math.abs(Math.round(Math.log10(stepSize % 1.0)));
      for (int j = 0; j < numZeros; j++) {
        formatString.append("0");
      }
      format = new DecimalFormat(formatString.toString());
    }
    final String label = String.valueOf(format.format(displayValue) + this.unitLabel);
    graphics.setColor(Color.BLACK);
    graphics.drawString(label, textX, textY);
  }

  /**
   * Returns the insets of the border.
   * @param c the component for which this border insets value applies
   */
  @Override
  public Insets getBorderInsets(final Component c) {
    return new Insets(this.rulerSize, this.rulerSize, this.rulerSize, this.rulerSize);
  }

  /**
   * Reinitialize the insets parameter with this Border's current Insets.
   * @param c the component for which this border insets value applies
   * @param insets the object to be reinitialized
   */
  @Override
  public Insets getBorderInsets(final Component c, final Insets insets) {
    insets.left = this.rulerSize;
    insets.top = this.rulerSize;
    insets.right = this.rulerSize;
    insets.bottom = this.rulerSize;
    return insets;
  }

  private <Q extends Quantity<Q>> int getStepLevel(final double[] steps) {
    final double sizeOf6Pixels = this.unitsPerPixel * 6;
    int i = 0;
    for (final double step : steps) {
      if (sizeOf6Pixels > step) {
        if (i == 0) {
          return 0;
        } else {
          return i - 1;
        }
      }
      i++;
    }
    return i - 1;
  }

  private void paintBackground(final Graphics2D g, final int x, final int y, final int width,
    final int height) {
    g.setColor(Color.WHITE);
    g.fillRect(x, y, this.rulerSize - 1, height); // left
    g.fillRect(x + width - this.rulerSize + 1, y, this.rulerSize - 1, height - 1); // right
    g.fillRect(x + this.rulerSize - 1, y, width - 2 * this.rulerSize + 2, this.rulerSize - 1); // top
    g.fillRect(x + this.rulerSize - 1, y + height - this.rulerSize + 1,
      width - 2 * this.rulerSize + 2, this.rulerSize - 1); // bottom
  }

  @Override
  public void paintBorder(final Component c, final Graphics g, final int x, final int y,
    final int width, final int height) {
    final Graphics2D graphics = (Graphics2D)g;
    if (width > 0) {
      graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
      final FontMetrics fontMetrics = graphics.getFontMetrics();
      this.labelHeight = fontMetrics.getHeight();

      paintBackground(graphics, x, y, width, height);

      final BoundingBox boundingBox = this.viewport.getBoundingBox();
      if (boundingBox.getWidth() > 0) {
        paintRuler(graphics, boundingBox, true, x, y, width, height);
        graphics.setColor(Color.BLACK);
        graphics.drawRect(this.rulerSize - 1, this.rulerSize - 1, width - 2 * this.rulerSize + 1,
          height - 2 * this.rulerSize + 1);
      }
    }
  }

  private <Q extends Quantity<Q>> void paintHorizontalRuler(final Graphics2D g,
    final BoundingBox boundingBox, final int x, final int y, final int width, final int height,
    final boolean top) {
    final double viewSize = this.viewport.getViewWidthPixels();
    if (viewSize > 0) {
      final AffineTransform transform = g.getTransform();
      final Shape clip = g.getClip();
      try {
        int textY;
        LineSegment line;

        final double x1 = boundingBox.getMinX();
        final double x2 = boundingBox.getMaxX();
        double y0;
        if (top) {
          g.translate(this.rulerSize, 0);
          textY = this.labelHeight;
          y0 = boundingBox.getMaxY();
        } else {
          g.translate(this.rulerSize, height - this.rulerSize);
          textY = this.rulerSize - 3;
          y0 = boundingBox.getMinY();
        }
        line = new LineSegmentDoubleGF(boundingBox.getGeometryFactory(), 2, x1, y0, x2, y0);

        line = line.convertGeometry(this.rulerGeometryFactory);

        g.setClip(0, 0, width - 2 * this.rulerSize, this.rulerSize);

        final double mapSize = boundingBox.getWidth();
        final double minX = line.getX(0);
        double maxX = line.getX(1);
        if (maxX > this.areaMaxX) {
          maxX = this.areaMaxX;
        }

        final double pixelsPerUnit = viewSize / mapSize;

        final long minIndex = (long)Math.floor(this.areaMinX / this.step);
        final long maxIndex = (long)Math.floor(maxX / this.step);
        long startIndex = (long)Math.floor(minX / this.step);
        if (startIndex < minIndex) {
          startIndex = minIndex;
        }
        for (long index = startIndex; index <= maxIndex; index++) {
          final double value = this.step * index;
          final int pixel = (int)((value - minX) * pixelsPerUnit);
          boolean found = false;
          int barSize = 4;

          g.setColor(Color.LIGHT_GRAY);
          for (int i = 0; !found && i < this.stepLevel; i++) {
            final double stepResolution = this.steps[i];
            final double diff = Math.abs(value % stepResolution);
            if (diff < 0.000001) {
              barSize = 4
                + (int)((this.rulerSize - 4) * (((double)this.stepLevel - i) / this.stepLevel));
              found = true;
              drawLabel(g, pixel + 3, textY, value, stepResolution);
            }

          }

          if (top) {
            g.drawLine(pixel, this.rulerSize - 1 - barSize, pixel, this.rulerSize - 1);
          } else {
            g.drawLine(pixel, 0, pixel, barSize);
          }

        }
      } finally {
        g.setTransform(transform);
        g.setClip(clip);
      }
    }
  }

  private <Q extends Quantity<Q>> void paintRuler(final Graphics2D g, final BoundingBox boundingBox,
    final boolean horizontal, final int x, final int y, final int width, final int height) {
    paintHorizontalRuler(g, boundingBox, x, y, width, height, true);
    paintHorizontalRuler(g, boundingBox, x, y, width, height, false);

    paintVerticalRuler(g, boundingBox, x, y, width, height, true);
    paintVerticalRuler(g, boundingBox, x, y, width, height, false);

  }

  private <Q extends Quantity<Q>> void paintVerticalRuler(final Graphics2D g,
    final BoundingBox boundingBox, final int x, final int y, final int width, final int height,
    final boolean left) {
    final double viewSize = this.viewport.getViewHeightPixels();
    if (viewSize > 0) {
      final AffineTransform transform = g.getTransform();
      final Shape clip = g.getClip();
      try {
        int textX;
        LineSegment line;
        final double y1 = boundingBox.getMinY();
        final double y2 = boundingBox.getMaxY();
        double x0;
        if (left) {
          g.translate(0, -this.rulerSize);
          textX = this.labelHeight;
          x0 = boundingBox.getMinX();
        } else {
          g.translate(width - this.rulerSize, -this.rulerSize);
          textX = this.rulerSize - 3;
          x0 = boundingBox.getMaxX();
        }
        line = new LineSegmentDoubleGF(boundingBox.getGeometryFactory(), 2, x0, y1, x0, y2);

        line = line.convertGeometry(this.rulerGeometryFactory);

        g.setClip(0, this.rulerSize * 2, this.rulerSize, height - 2 * this.rulerSize);

        final double mapSize = boundingBox.getHeight();
        final double minY = line.getY(0);
        double maxY = line.getY(1);
        if (maxY > this.areaMaxY) {
          maxY = this.areaMaxY;
        }

        final double pixelsPerUnit = viewSize / mapSize;

        final long minIndex = (long)Math.ceil(this.areaMinY / this.step);
        final long maxIndex = (long)Math.ceil(maxY / this.step);
        long startIndex = (long)Math.floor(minY / this.step);
        if (startIndex < minIndex) {
          startIndex = minIndex;
        }
        for (long index = startIndex; index <= maxIndex; index++) {
          final double value = this.step * index;
          final int pixel = (int)((value - minY) * pixelsPerUnit);
          boolean found = false;
          int barSize = 4;

          g.setColor(Color.LIGHT_GRAY);
          for (int i = 0; !found && i < this.stepLevel; i++) {
            final double stepResolution = this.steps[i];
            final double diff = Math.abs(value % stepResolution);
            if (diff < 0.000001) {
              barSize = 4
                + (int)((this.rulerSize - 4) * (((double)this.stepLevel - i) / this.stepLevel));
              found = true;
              final AffineTransform transform2 = g.getTransform();
              try {
                g.translate(textX, height - pixel - 3);
                g.rotate(-Math.PI / 2);
                drawLabel(g, 0, 0, value, value);
              } finally {
                g.setTransform(transform2);
              }
            }

          }

          if (left) {
            g.drawLine(this.rulerSize - 1 - barSize, height - pixel, this.rulerSize - 1,
              height - pixel);
          } else {
            g.drawLine(0, height - pixel, barSize, height - pixel);
          }

        }

      } finally {
        g.setTransform(transform);
        g.setClip(clip);
      }
    }
  }

  private void setRulerGeometryFactory(final GeometryFactory rulerGeometryFactory) {
    this.rulerGeometryFactory = rulerGeometryFactory;
    if (rulerGeometryFactory == null) {
      this.rulerGeometryFactory = this.viewport.getGeometryFactory();
    } else {
      this.rulerGeometryFactory = rulerGeometryFactory;
    }
    this.rulerCoordinateSystem = this.rulerGeometryFactory.getHorizontalCoordinateSystem();
    this.baseUnit = this.rulerCoordinateSystem.getUnit();
    this.unitLabel = this.baseUnit.getSymbol();
    if (this.unitLabel == null) {
      this.unitLabel = this.baseUnit.getName();
    }
    this.steps = STEPS;
    if (this.rulerCoordinateSystem instanceof GeographicCoordinateSystem) {
      final GeographicCoordinateSystem geoCs = (GeographicCoordinateSystem)this.rulerCoordinateSystem;
      if (geoCs.getAngularUnit() instanceof Degree) {
        this.areaMinX = -180;
        this.areaMaxX = 180;
        this.areaMinY = -90;
        this.areaMaxY = 90;
        this.steps = GEOGRAPHICS_STEPS;
        this.unitLabel = "°";
      }
    } else {
      final BoundingBox areaBoundingBox = this.rulerCoordinateSystem.getAreaBoundingBox();

      this.areaMinX = areaBoundingBox.getMinX();
      this.areaMaxX = areaBoundingBox.getMaxX();
      this.areaMinY = areaBoundingBox.getMinY();
      this.areaMaxY = areaBoundingBox.getMaxY();
    }
    updateValues();
  }

  private void setUnitsPerPixel(final double unitsPerPixel) {
    this.unitsPerPixel = unitsPerPixel;
    updateValues();
  }

  private void updateValues() {
    this.stepLevel = getStepLevel(this.steps);
    this.step = this.steps[this.stepLevel];
  }
}
