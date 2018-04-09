package com.revolsys.swing.map;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.quantity.Length;

import com.revolsys.awt.CloseableAffineTransform;
import com.revolsys.beans.PropertyChangeSupport;
import com.revolsys.beans.PropertyChangeSupportProxy;
import com.revolsys.datatype.DataType;
import com.revolsys.geometry.cs.CoordinateSystem;
import com.revolsys.geometry.cs.GeographicCoordinateSystem;
import com.revolsys.geometry.cs.unit.CustomUnits;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.GeometryFactoryProxy;
import com.revolsys.geometry.model.Point;
import com.revolsys.io.BaseCloseable;
import com.revolsys.record.Record;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerRenderer;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.layer.record.AbstractRecordLayer;
import com.revolsys.swing.map.layer.record.LayerRecord;
import com.revolsys.swing.map.layer.record.renderer.GeometryStyleRenderer;
import com.revolsys.swing.map.layer.record.renderer.TextStyleRenderer;
import com.revolsys.swing.map.layer.record.style.GeometryStyle;
import com.revolsys.swing.map.layer.record.style.TextStyle;
import com.revolsys.swing.map.overlay.MouseOverlay;
import com.revolsys.util.Property;
import com.revolsys.util.QuantityType;
import com.revolsys.util.number.Doubles;

import systems.uom.common.USCustomary;
import tec.uom.se.quantity.Quantities;
import tec.uom.se.unit.Units;

public class Viewport2D implements GeometryFactoryProxy, PropertyChangeSupportProxy {

  public static final Geometry EMPTY_GEOMETRY = GeometryFactory.DEFAULT_3D.geometry();

  private static final int HOTSPOT_PIXELS = 10;

  public static final List<Long> SCALES = Arrays.asList(500000000L, 250000000L, 100000000L,
    50000000L, 25000000L, 10000000L, 5000000L, 2500000L, 1000000L, 500000L, 250000L, 100000L,
    50000L, 25000L, 10000L, 5000L, 2500L, 1000L, 500L, 250L, 100L, 50L, 25L, 10L, 5L, 1L);

  private static List<Double> GEOGRAPHIC_UNITS_PER_PIXEL = Arrays.asList(5.0, 2.0, 1.0, 0.5, 0.2,
    0.1, 0.05, 0.02, 0.01, 0.005, 0.002, 0.001, 0.0005, 0.0002, 0.0001, 0.00005, 0.00002, 0.00001,
    0.000005, 0.000002, 0.000001, 0.0000005, 0.0000002, 0.0000001);

  private static List<Double> PROJECTED_UNITS_PER_PIXEL = Arrays.asList(500000.0, 200000.0,
    100000.0, 50000.0, 20000.0, 10000.0, 5000.0, 2000.0, 1000.0, 500.0, 200.0, 100.0, 50.0, 20.0,
    10.0, 5.0, 2.0, 1.0, 0.5, 0.2, 0.1, 0.05, 0.02, 0.01, 0.005, 0.002, 0.001);

  public static double getScale(final Quantity<Length> viewWidth,
    final Quantity<Length> modelWidth) {
    final double width1 = QuantityType.doubleValue(viewWidth, Units.METRE);
    final double width2 = QuantityType.doubleValue(modelWidth, Units.METRE);
    if (width1 == 0 || width2 == 0) {
      return Double.NaN;
    } else {
      final double scale = width2 / width1;
      return scale;
    }
  }

  public static AffineTransform newScreenToModelTransform(final BoundingBox boundingBox,
    final double viewWidth, final double viewHeight) {
    final AffineTransform transform = new AffineTransform();
    final double mapWidth = boundingBox.getWidth();
    final double mapHeight = boundingBox.getHeight();
    final double xUnitsPerPixel = mapWidth / viewWidth;
    final double yUnitsPerPixel = mapHeight / viewHeight;

    final double originX = boundingBox.getMinX();
    final double originY = boundingBox.getMaxY();

    transform.concatenate(AffineTransform.getTranslateInstance(originX, originY));
    transform.concatenate(AffineTransform.getScaleInstance(xUnitsPerPixel, -yUnitsPerPixel));
    return transform;
  }

  public static BaseCloseable setUseModelCoordinates(final Viewport2D viewport,
    final Graphics2D graphics, final boolean useModelCoordinates) {
    if (viewport != null) {
      return viewport.setUseModelCoordinates(graphics, useModelCoordinates);
    }
    return null;
  }

  public static double toDisplayValue(final Viewport2D viewport, final Quantity<Length> measure) {
    if (viewport == null) {
      return measure.getValue().doubleValue();
    } else {
      return viewport.toDisplayValue(measure);
    }
  }

  public static double toModelValue(final Viewport2D viewport, final Quantity<Length> measure) {
    if (viewport == null) {
      return measure.getValue().doubleValue();
    } else {
      return viewport.toModelValue(measure);
    }
  }

  public static void translateModelToViewCoordinates(final Viewport2D viewport,
    final Graphics2D graphics, final double modelX, final double modelY) {
    if (viewport != null) {
      final double[] viewCoordinates = viewport.toViewCoordinates(modelX, modelY);
      final double viewX = viewCoordinates[0];
      final double viewY = viewCoordinates[1];
      graphics.translate(viewX, viewY);
    }
  }

  /** The current bounding box of the project. */
  private BoundingBox boundingBox = BoundingBox.empty();

  private GeometryFactory geometryFactory = GeometryFactory.floating3d(3857);

  private GeometryFactory geometryFactory2d = GeometryFactory.floating2d(3857);

  private boolean initialized = false;

  private AffineTransform modelToScreenTransform;

  private double originX;

  private double originY;

  private double pixelsPerXUnit;

  private double pixelsPerYUnit;

  private Reference<Project> project;

  /** The property change listener support. */
  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  private double scale;

  private List<Long> scales = SCALES;

  private AffineTransform screenToModelTransform;

  private double metresPerPixel;

  private double unitsPerPixel;

  private int viewHeight;

  private int viewWidth;

  private double hotspotMapUnits = 6;

  private boolean zoomByUnitsPerPixel = true;

  private List<Double> unitsPerPixelList = PROJECTED_UNITS_PER_PIXEL;

  private double minUnitsPerPixel;

  private double maxUnitsPerPixel;

  public Viewport2D() {
  }

  public Viewport2D(final Project project) {
    this(project, 0, 0, project.getViewBoundingBox());
  }

  public Viewport2D(final Project project, final int width, final int height,
    BoundingBox boundingBox) {
    this.project = new WeakReference<>(project);
    this.viewWidth = width;
    this.viewHeight = height;
    GeometryFactory geometryFactory;
    if (boundingBox == null) {
      geometryFactory = GeometryFactory.worldMercator();
      final CoordinateSystem coordinateSystem = geometryFactory.getHorizontalCoordinateSystem();
      boundingBox = coordinateSystem.getAreaBoundingBox();
    } else {
      geometryFactory = boundingBox.getGeometryFactory();
      if (!geometryFactory.isHasHorizontalCoordinateSystem()) {
        geometryFactory = GeometryFactory.worldMercator();
      }
      if (boundingBox.isEmpty()) {
        final CoordinateSystem coordinateSystem = geometryFactory.getHorizontalCoordinateSystem();
        if (coordinateSystem != null) {
          boundingBox = coordinateSystem.getAreaBoundingBox();
        }
      }
    }
    setGeometryFactory(geometryFactory);
    setBoundingBox(boundingBox);
  }

  public void drawGeometry(final Geometry geometry, final GeometryStyle style) {
    final Graphics2D graphics = getGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    GeometryStyleRenderer.renderGeometry(this, graphics, geometry, style);
  }

  public void drawGeometryOutline(final Geometry geometry, final GeometryStyle style) {
    final Graphics2D graphics = getGraphics();
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    GeometryStyleRenderer.renderGeometryOutline(this, graphics, geometry, style);
  }

  public void drawText(final Record record, final Geometry geometry, final TextStyle style) {
    final Graphics2D graphics = getGraphics();
    if (graphics != null) {
      TextStyleRenderer.renderText(this, graphics, record, geometry, style);
    }

  }

  public BoundingBox getBoundingBox() {
    return this.boundingBox;
  }

  public BoundingBox getBoundingBox(final GeometryFactory geometryFactory, final int pixels) {
    final int x = MouseOverlay.getEventX();
    final int y = MouseOverlay.getEventY();
    return getBoundingBox(geometryFactory, x, y, pixels);
  }

  public BoundingBox getBoundingBox(final GeometryFactory geometryFactory, final int x, final int y,
    final int pixels) {
    final Point p1 = toModelPoint(geometryFactory, x - pixels, y - pixels);
    final Point p2 = toModelPoint(geometryFactory, x + pixels, y + pixels);
    final BoundingBox boundingBox = geometryFactory.newBoundingBox(p1.getX(), p1.getY(), p2.getX(),
      p2.getY());
    return boundingBox;
  }

  public BoundingBox getBoundingBox(final GeometryFactory geometryFactory, final MouseEvent event,
    final int pixels) {
    final int x = event.getX();
    final int y = event.getY();
    return getBoundingBox(geometryFactory, x, y, pixels);
  }

  public Geometry getGeometry(final Geometry geometry) {
    final BoundingBox viewExtent = getBoundingBox();
    if (Property.hasValue(geometry)) {
      if (!viewExtent.isEmpty()) {
        final BoundingBox geometryExtent = geometry.getBoundingBox();
        if (geometryExtent.intersects(viewExtent)) {

          final GeometryFactory geometryFactory = getGeometryFactory2dFloating();
          if (geometryFactory.isSameCoordinateSystem(geometry)) {
            return geometry;
          } else {
            return geometryFactory.geometry(geometry);
          }
        }
      }
    }
    return EMPTY_GEOMETRY;
  }

  /**
   * Get the coordinate system the project is displayed in.
   *
   * @return The coordinate system the project is displayed in.
   */
  @Override
  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  public GeometryFactory getGeometryFactory2dFloating() {
    return this.geometryFactory2d;
  }

  @Deprecated
  public Graphics2D getGraphics() {
    return null;
  }

  public double getHotspotMapUnits() {
    return this.hotspotMapUnits;
  }

  public double getMetresPerPixel() {
    return this.metresPerPixel;
  }

  public double getModelHeight() {
    final double height = getBoundingBox().getHeight();
    return height;
  }

  public Quantity<Length> getModelHeightLength() {
    return getBoundingBox().getHeightLength();
  }

  public AffineTransform getModelToScreenTransform() {
    return this.modelToScreenTransform;
  }

  public double getModelUnitsPerViewUnit() {
    return getModelHeight() / getViewHeightPixels();
  }

  public double getModelWidth() {
    final double width = getBoundingBox().getWidth();
    return width;
  }

  public Quantity<Length> getModelWidthLength() {
    return getBoundingBox().getWidthLength();
  }

  public double getOriginX() {
    return this.originX;
  }

  public double getOriginY() {
    return this.originY;
  }

  public double getPixelsPerXUnit() {
    return this.pixelsPerXUnit;
  }

  public double getPixelsPerYUnit() {
    return this.pixelsPerYUnit;
  }

  protected double getPixelsPerYUnit(final double viewHeight, final double mapHeight) {
    return -viewHeight / mapHeight;
  }

  public Project getProject() {
    if (this.project == null) {
      return null;
    } else {
      return this.project.get();
    }
  }

  /**
   * Get the property change support, used to fire property change
   * notifications. Returns null if no listeners are registered.
   *
   * @return The property change support.
   */
  @Override
  public PropertyChangeSupport getPropertyChangeSupport() {
    return this.propertyChangeSupport;
  }

  public <V extends Geometry> V getRoundedGeometry(final V geometry) {
    if (geometry == null) {
      return null;
    } else {
      final GeometryFactory geometryFactory = geometry.getGeometryFactory();

      final GeometryFactory roundedGeometryFactory = getRoundedGeometryFactory(geometryFactory);
      if (geometryFactory == roundedGeometryFactory) {
        return geometry;
      } else {
        return (V)geometry.newGeometry(geometryFactory);
      }
    }
  }

  public GeometryFactory getRoundedGeometryFactory(GeometryFactory geometryFactory) {
    if (geometryFactory.isProjected()) {
      final double resolution = getMetresPerPixel();
      if (resolution > 2) {
        geometryFactory = geometryFactory.convertScales(1.0, 1.0);
      } else {
        geometryFactory = geometryFactory.convertScales(1000.0, 1000.0);
      }
    }
    return geometryFactory;
  }

  public double getScale() {
    return this.scale;
  }

  public double getScaleForUnitsPerPixel(final double unitsPerPixel) {
    return unitsPerPixel * getScreenResolution() / 0.0254;
  }

  /**
   * Get the scale which dictates if a layer or renderer is visible. This is used when printing
   * to ensure the same layers and renderers are used for printing as is shown on the screen.
   *
   * @return
   */
  public double getScaleForVisible() {
    return getScale();
  }

  public List<Long> getScales() {
    return this.scales;
  }

  public int getScreenResolution() {
    final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
    final int screenResolution = defaultToolkit.getScreenResolution();
    return 96;
  }

  public AffineTransform getScreenToModelTransform() {
    return this.screenToModelTransform;
  }

  public Unit<Length> getScreenUnit() {
    final int screenResolution = getScreenResolution();
    return USCustomary.INCH.divide(screenResolution);
  }

  public double getUnitsPerPixel() {
    return this.unitsPerPixel;
  }

  public double getUnitsPerPixel(final double scale) {
    return scale * 0.0254 / getScreenResolution();
  }

  public List<Double> getUnitsPerPixelList() {
    return this.unitsPerPixelList;
  }

  public double getViewAspectRatio() {
    final int viewWidthPixels = getViewWidthPixels();
    final int viewHeightPixels = getViewHeightPixels();
    if (viewHeightPixels == 0) {
      return 0;
    } else {
      return (double)viewWidthPixels / viewHeightPixels;
    }
  }

  public Quantity<Length> getViewHeightLength() {
    double width = getViewHeightPixels();
    if (width < 0) {
      width = 0;
    }
    return Quantities.getQuantity(width, getScreenUnit());
  }

  public int getViewHeightPixels() {
    return this.viewHeight;
  }

  public Quantity<Length> getViewWidthLength() {
    double width = getViewWidthPixels();
    if (width < 0) {
      width = 0;
    }
    return Quantities.getQuantity(width, getScreenUnit());
  }

  public int getViewWidthPixels() {
    return this.viewWidth;
  }

  public long getZoomInScale(final double scale, final int steps) {
    final List<Long> values = this.scales;
    final long scaleCeil = (long)Math.floor(scale);
    for (int i = 0; i < values.size(); i++) {
      long nextScale = values.get(i);
      if (nextScale < scaleCeil) {
        for (int j = 1; j < steps && i + j < values.size(); j++) {
          nextScale = values.get(i + j);
        }
        return nextScale;
      }
    }
    return values.get(values.size() - 1);
  }

  public double getZoomInUnitsPerPixel(final double unitsPerPixel, final int steps) {
    final List<Double> values = this.unitsPerPixelList;
    for (int i = 0; i < values.size(); i++) {
      double nextValue = values.get(i);
      if (nextValue < unitsPerPixel) {
        for (int j = 1; j < steps && i + j < values.size(); j++) {
          nextValue = values.get(i + j);
        }
        return nextValue;
      }
    }
    return this.minUnitsPerPixel;
  }

  public double getZoomOutScale(final double scale) {
    final long scaleCeil = (long)Math.floor(scale);
    final List<Long> scales = new ArrayList<>(this.scales);
    Collections.reverse(scales);
    for (final double nextScale : scales) {
      final long newScale = (long)Math.floor(nextScale);
      if (newScale >= scaleCeil) {
        return nextScale;
      }
    }
    return scales.get(0);
  }

  public long getZoomOutScale(final double scale, final int steps) {
    final long scaleCeil = (long)Math.floor(scale);
    for (int i = this.scales.size() - 1; i >= 0; i--) {
      long nextScale = this.scales.get(i);
      if (nextScale > scaleCeil) {
        for (int j = 1; j < steps && i - j >= 0; j++) {
          nextScale = this.scales.get(i - j);
        }
        return nextScale;
      }
    }
    return this.scales.get(0);
  }

  public double getZoomOutUnitsPerPixel(final double unitsPerPixel, final int steps) {
    final List<Double> values = this.unitsPerPixelList;
    for (int i = values.size() - 1; i >= 0; i--) {
      double nextValue = values.get(i);
      if (nextValue > unitsPerPixel) {
        for (int j = 1; j < steps && i - j >= 0; j++) {
          nextValue = values.get(i - j);
        }
        return nextValue;
      }
    }
    return this.maxUnitsPerPixel;
  }

  public boolean isHidden(final AbstractRecordLayer layer, final LayerRecord record) {
    return layer.isHidden(record);
  }

  public boolean isInitialized() {
    return this.initialized;
  }

  public boolean isZoomByUnitsPerPixel() {
    return this.zoomByUnitsPerPixel;
  }

  public AffineTransform newModelToScreenTransform(final BoundingBox boundingBox,
    final double viewWidth, final double viewHeight) {
    final AffineTransform modelToScreenTransform = new AffineTransform();
    final double mapWidth = boundingBox.getWidth();
    this.pixelsPerXUnit = viewWidth / mapWidth;
    this.hotspotMapUnits = HOTSPOT_PIXELS / this.pixelsPerXUnit;

    final double mapHeight = boundingBox.getHeight();
    this.pixelsPerYUnit = getPixelsPerYUnit(viewHeight, mapHeight);

    this.originX = boundingBox.getMinX();
    this.originY = boundingBox.getMaxY();
    modelToScreenTransform
      .concatenate(AffineTransform.getScaleInstance(this.pixelsPerXUnit, this.pixelsPerYUnit));
    modelToScreenTransform
      .concatenate(AffineTransform.getTranslateInstance(-this.originX, -this.originY));
    return modelToScreenTransform;
  }

  public void render(final Layer layer) {
    if (layer != null && layer.isExists() && layer.isVisible()) {
      final LayerRenderer<Layer> renderer = layer.getRenderer();
      if (renderer != null) {
        renderer.render(this);
      }
    }
  }

  public BoundingBox setBoundingBox(BoundingBox boundingBox) {
    if (boundingBox != null && !boundingBox.isEmpty()) {
      final GeometryFactory geometryFactory = getGeometryFactory2dFloating();
      boundingBox = boundingBox.convert(geometryFactory);
      if (!boundingBox.isEmpty()) {
        BoundingBox newBoundingBox = boundingBox;
        double width = newBoundingBox.getWidth();
        final double height = newBoundingBox.getHeight();

        final int viewWidthPixels = getViewWidthPixels();
        final int viewHeightPixels = getViewHeightPixels();
        double unitsPerPixel;
        if (viewWidthPixels > 0) {
          final double viewAspectRatio = getViewAspectRatio();
          final double aspectRatio = newBoundingBox.getAspectRatio();
          if (viewAspectRatio != aspectRatio) {
            if (aspectRatio < viewAspectRatio) {
              final double newWidth = height * viewAspectRatio;
              final double expandX = (newWidth - width) / 2;
              newBoundingBox = newBoundingBox.expand(expandX, 0);
              width = newBoundingBox.getWidth();
            } else if (aspectRatio > viewAspectRatio) {
              final double newHeight = width / viewAspectRatio;
              final double expandY = (newHeight - height) / 2;
              newBoundingBox = newBoundingBox.expand(0, expandY);
            }
          }
          unitsPerPixel = Doubles.makePrecise(10000000, width / viewWidthPixels);
          if (!this.unitsPerPixelList.isEmpty() && viewWidthPixels > 0 && viewHeightPixels > 0) {
            if (unitsPerPixel < this.minUnitsPerPixel) {
              unitsPerPixel = this.minUnitsPerPixel;
            } else if (unitsPerPixel > this.maxUnitsPerPixel) {
              unitsPerPixel = this.maxUnitsPerPixel;
            }
          }
        } else {
          unitsPerPixel = Double.NaN;
        }
        setBoundingBoxAndUnitsPerPixel(newBoundingBox, unitsPerPixel);
      }
    }
    return getBoundingBox();
  }

  public void setBoundingBoxAndGeometryFactory(final BoundingBox boundingBox) {
    final GeometryFactory oldGeometryFactory = this.geometryFactory;
    final GeometryFactory geometryFactory = boundingBox.getGeometryFactory();
    setBoundingBox(boundingBox);

    if (setGeometryFactoryDo(geometryFactory)) {
      this.propertyChangeSupport.firePropertyChange("geometryFactory", oldGeometryFactory,
        this.geometryFactory);
    }
  }

  private void setBoundingBoxAndUnitsPerPixel(final BoundingBox boundingBox, double unitsPerPixel) {
    final double oldScale = getScale();
    final double oldUnitsPerPixel = getUnitsPerPixel();
    final BoundingBox oldBoundingBox = this.boundingBox;
    synchronized (this) {
      final int viewWidthPixels = getViewWidthPixels();
      final int viewHeightPixels = getViewHeightPixels();

      if (Double.isFinite(unitsPerPixel)) {
        if (unitsPerPixel < this.minUnitsPerPixel) {
          unitsPerPixel = this.minUnitsPerPixel;
        }
        final double pixelSizeMetres = QuantityType
          .doubleValue(Quantities.getQuantity(1, getScreenUnit()), Units.METRE);
        this.unitsPerPixel = unitsPerPixel;
        setModelToScreenTransform(
          newModelToScreenTransform(boundingBox, viewWidthPixels, viewHeightPixels));
        this.screenToModelTransform = newScreenToModelTransform(boundingBox, viewWidthPixels,
          viewHeightPixels);
        final Quantity<Length> modelWidthLength = boundingBox.getWidthLength();
        double modelWidthMetres;
        if (getGeometryFactory().isProjected()) {
          modelWidthMetres = QuantityType.doubleValue(modelWidthLength, Units.METRE);
        } else {
          final double minX = boundingBox.getMinX();
          final double centreY = boundingBox.getCentreY();
          final double maxX = boundingBox.getMaxX();
          modelWidthMetres = GeographicCoordinateSystem.distanceMetres(minX, centreY, maxX,
            centreY);
        }
        this.metresPerPixel = modelWidthMetres / viewWidthPixels;
        this.scale = this.metresPerPixel / pixelSizeMetres;
      } else {
        this.metresPerPixel = 0;
        this.unitsPerPixel = 0;
        setModelToScreenTransform(null);
        this.screenToModelTransform = null;
        this.scale = 0;
      }
      setBoundingBoxInternal(boundingBox);
    }
    if (this.unitsPerPixel > 0) {
      this.propertyChangeSupport.firePropertyChange("unitsPerPixel", oldUnitsPerPixel,
        this.unitsPerPixel);
    }
    if (this.scale > 0) {
      this.propertyChangeSupport.firePropertyChange("scale", oldScale, this.scale);
    }
    this.propertyChangeSupport.firePropertyChange("boundingBox", oldBoundingBox, boundingBox);
  }

  private BoundingBox setBoundingBoxForScale(final BoundingBox boundingBox, final double scale) {
    final Point centre = boundingBox.getCentre();
    return setCentreFromScale(centre, scale);
  }

  /**
   * Set the bounding box using the units perPixel from the minX, minY.
   * @return
   */
  private BoundingBox setBoundingBoxFromUnitsPerPixel(final double minX, final double minY,
    final double unitsPerPixel) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    final int viewWidthPixels = getViewWidthPixels();
    final int viewHeightPixels = getViewHeightPixels();

    final double width = viewWidthPixels * unitsPerPixel;
    final double height = viewHeightPixels * unitsPerPixel;

    final double maxX = minX + width;
    final double maxY = minY + height;
    final BoundingBox boundingBox = geometryFactory.newBoundingBox(minX, minY, maxX, maxY);
    setBoundingBoxAndUnitsPerPixel(boundingBox, unitsPerPixel);
    return boundingBox;
  }

  protected void setBoundingBoxInternal(final BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public void setCentre(Point centre) {
    if (centre != null) {
      final GeometryFactory geometryFactory = getGeometryFactory();
      centre = centre.convertGeometry(geometryFactory, 2);
      if (!centre.isEmpty()) {
        final double scale = getScale();
        setCentreFromScale(centre, scale);
      }
    }
  }

  private BoundingBox setCentreFromScale(final Point centre, final double scale) {
    final double unitsPerPixel = getUnitsPerPixel(scale);
    return setCentreFromUnitsPerPixel(centre, unitsPerPixel);
  }

  private BoundingBox setCentreFromUnitsPerPixel(final Point centre, final double unitsPerPixel) {
    final int viewWidthPixels = getViewWidthPixels();
    final int viewHeightPixels = getViewHeightPixels();

    final int minXPixelOffset = (int)Math.ceil(viewWidthPixels / 2);
    final int minYPixelOffset = (int)Math.ceil(viewHeightPixels / 2);

    final double centreX = centre.getX();
    final double centreY = centre.getY();

    final double minX = centreX - minXPixelOffset * unitsPerPixel;
    final double minY = centreY - minYPixelOffset * unitsPerPixel;

    return setBoundingBoxFromUnitsPerPixel(minX, minY, unitsPerPixel);
  }

  /**
   * Set the coordinate system the project is displayed in.
   *
   * @param coordinateSystem The coordinate system the project is displayed in.
   */
  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    final GeometryFactory oldGeometryFactory = this.geometryFactory;
    if (setGeometryFactoryDo(geometryFactory)) {
      setGeometryFactoryPreEvent(geometryFactory);
      this.propertyChangeSupport.firePropertyChange("geometryFactory", oldGeometryFactory,
        geometryFactory);
      setGeometryFactoryPostEvent(geometryFactory);
    }
  }

  protected boolean setGeometryFactoryDo(final GeometryFactory geometryFactory) {
    final GeometryFactory oldGeometryFactory = this.geometryFactory;
    if (DataType.equal(oldGeometryFactory, geometryFactory)) {
      return false;
    } else {
      if (geometryFactory == null) {
        this.geometryFactory = GeometryFactory.DEFAULT_3D;
      } else {
        this.geometryFactory = geometryFactory;
      }

      this.geometryFactory2d = this.geometryFactory.to2dFloating();
      if (geometryFactory.isGeographics()) {
        this.unitsPerPixelList = GEOGRAPHIC_UNITS_PER_PIXEL;
      } else {
        this.unitsPerPixelList = PROJECTED_UNITS_PER_PIXEL;
      }
      this.maxUnitsPerPixel = this.unitsPerPixelList.get(0);
      this.minUnitsPerPixel = this.unitsPerPixelList.get(this.unitsPerPixelList.size() - 1);
      return true;
    }
  }

  protected void setGeometryFactoryPostEvent(final GeometryFactory geometryFactory2) {
  }

  protected void setGeometryFactoryPreEvent(final GeometryFactory geometryFactory2) {
  }

  public void setInitialized(final boolean initialized) {
    this.initialized = initialized;
  }

  protected void setModelToScreenTransform(final AffineTransform modelToScreenTransform) {
    this.modelToScreenTransform = modelToScreenTransform;
  }

  public void setScale(final double scale) {
    setZoomByUnitsPerPixel(false);
    final double oldValue = getScale();
    if (scale > 0 && Math.abs(oldValue - scale) > 0.0001) {
      final double oldUnitsPerPixel = getMetresPerPixel();
      final BoundingBox boundingBox = getBoundingBox();
      setBoundingBoxForScale(boundingBox, scale);
      this.propertyChangeSupport.firePropertyChange("scale", oldValue, scale);

      final double unitsPerPixel = getMetresPerPixel();
      if (Math.abs(oldUnitsPerPixel - unitsPerPixel) > 0.001) {
        this.propertyChangeSupport.firePropertyChange("unitsPerPixel", oldUnitsPerPixel,
          unitsPerPixel);
      }
    }
  }

  public void setScales(final List<Long> scales) {
    this.scales = scales;
  }

  public void setUnitsPerPixel(final double unitsPerPixel) {
    setZoomByUnitsPerPixel(true);
    final double oldUnitsPerPixel = getUnitsPerPixel();
    if (Double.isFinite(unitsPerPixel) && unitsPerPixel > 0
      && Math.abs(oldUnitsPerPixel - unitsPerPixel) >= 0.0000001) {

      final BoundingBox boundingBox = getBoundingBox();
      final Point centre = boundingBox.getCentre();
      setCentreFromUnitsPerPixel(centre, unitsPerPixel);
    }
  }

  public BaseCloseable setUseModelCoordinates(final boolean useModelCoordinates) {
    return null;
  }

  public BaseCloseable setUseModelCoordinates(final Graphics2D graphics,
    final boolean useModelCoordinates) {
    if (useModelCoordinates) {
      final CloseableAffineTransform transform = new CloseableAffineTransform(graphics);
      final AffineTransform modelToScreenTransform = getModelToScreenTransform();
      transform.concatenate(modelToScreenTransform);
      return transform;
    }
    return null;
  }

  protected void setViewHeight(final int height) {
    this.viewHeight = height;
  }

  protected void setViewWidth(final int width) {
    this.viewWidth = width;
  }

  public void setZoomByUnitsPerPixel(final boolean zoomByUnitsPerPixel) {
    this.zoomByUnitsPerPixel = zoomByUnitsPerPixel;
  }

  public double toDisplayValue(final Quantity<Length> value) {
    double convertedValue;
    final Unit<Length> unit = value.getUnit();
    if (unit.equals(CustomUnits.PIXEL)) {
      convertedValue = QuantityType.doubleValue(value, CustomUnits.PIXEL);
    } else {
      convertedValue = QuantityType.doubleValue(value, Units.METRE);
      final CoordinateSystem coordinateSystem = this.geometryFactory2d
        .getHorizontalCoordinateSystem();
      if (coordinateSystem instanceof GeographicCoordinateSystem) {
        final GeographicCoordinateSystem geoCs = (GeographicCoordinateSystem)coordinateSystem;
        final double radius = geoCs.getDatum().getEllipsoid().getSemiMajorAxis();
        convertedValue = Math.toDegrees(convertedValue / radius);

      }
      final double modelUnitsPerViewUnit = getModelUnitsPerViewUnit();
      convertedValue = convertedValue / modelUnitsPerViewUnit;
    }
    return convertedValue;
  }

  public double[] toModelCoordinates(final double... viewCoordinates) {
    final AffineTransform transform = getScreenToModelTransform();
    if (transform == null) {
      return viewCoordinates;
    } else {
      final double[] coordinates = new double[2];
      transform.transform(viewCoordinates, 0, coordinates, 0, 1);
      return coordinates;
    }
  }

  public Point toModelPoint(final double... viewCoordinates) {
    if (this.geometryFactory2d == null) {
      return GeometryFactory.DEFAULT_2D.point();
    } else {
      final double[] coordinates = toModelCoordinates(viewCoordinates);
      return this.geometryFactory2d.point(coordinates[0], coordinates[1]);
    }
  }

  public Point toModelPoint(final GeometryFactory geometryFactory,
    final double... viewCoordinates) {
    final double[] coordinates = toModelCoordinates(viewCoordinates);
    if (Double.isInfinite(coordinates[0]) || Double.isInfinite(coordinates[1])
      || Double.isNaN(coordinates[0]) || Double.isNaN(coordinates[1])) {
      return geometryFactory.point();
    } else {
      final Point point = this.geometryFactory2d.point(coordinates[0], coordinates[1]);
      return point.newGeometry(geometryFactory);
    }
  }

  public Point toModelPoint(final GeometryFactory geometryFactory, final java.awt.Point point) {
    final double x = point.getX();
    final double y = point.getY();
    return toModelPoint(geometryFactory, x, y);
  }

  public Point toModelPoint(final GeometryFactory geometryFactory, final MouseEvent event) {
    final java.awt.Point eventPoint = event.getPoint();
    return toModelPoint(geometryFactory, eventPoint);
  }

  public Point toModelPoint(final int x, final int y) {
    final AffineTransform transform = getScreenToModelTransform();
    if (transform == null) {
      return this.geometryFactory2d.point(x, y);
    } else {
      final double[] coordinates = new double[] {
        x, y
      };
      transform.transform(coordinates, 0, coordinates, 0, 1);
      return this.geometryFactory2d.point(coordinates);
    }
  }

  public Point toModelPoint(final java.awt.Point point) {
    final double x = point.getX();
    final double y = point.getY();
    return toModelPoint(x, y);
  }

  public Point toModelPointRounded(GeometryFactory geometryFactory, final int x, final int y) {
    geometryFactory = getRoundedGeometryFactory(geometryFactory);
    return toModelPoint(geometryFactory, x, y);
  }

  public double toModelValue(final Quantity<Length> value) {
    double convertedValue;
    final Unit<Length> unit = value.getUnit();
    if (unit.equals(CustomUnits.PIXEL)) {
      convertedValue = QuantityType.doubleValue(value, CustomUnits.PIXEL);
      final double modelUnitsPerViewUnit = getModelUnitsPerViewUnit();
      convertedValue *= modelUnitsPerViewUnit;
    } else {
      convertedValue = QuantityType.doubleValue(value, Units.METRE);
      final CoordinateSystem coordinateSystem = this.geometryFactory2d
        .getHorizontalCoordinateSystem();
      if (coordinateSystem instanceof GeographicCoordinateSystem) {
        final GeographicCoordinateSystem geoCs = (GeographicCoordinateSystem)coordinateSystem;
        final double radius = geoCs.getDatum().getEllipsoid().getSemiMajorAxis();
        convertedValue = Math.toDegrees(convertedValue / radius);

      }
    }
    return convertedValue;
  }

  public double[] toViewCoordinates(final double... modelCoordinates) {
    final double[] ordinates = new double[2];
    final AffineTransform transform = getModelToScreenTransform();
    if (transform == null) {
      return modelCoordinates;
    } else {
      transform.transform(modelCoordinates, 0, ordinates, 0, 1);
      return ordinates;
    }
  }

  public Point2D toViewPoint(final double x, final double y) {
    final double[] coordinates = toViewCoordinates(x, y);
    final double viewX = coordinates[0];
    final double viewY = coordinates[1];
    return new Point2D.Double(viewX, viewY);
  }

  public Point2D toViewPoint(Point point) {
    point = this.geometryFactory2d.project(point);
    final double x = point.getX();
    final double y = point.getY();
    return toViewPoint(x, y);
  }

  public void update() {
  }

  public void zoomIn() {
    if (this.zoomByUnitsPerPixel) {
      final double unitsPerPixel = getUnitsPerPixel();
      final double newUnitsPerPixel = getZoomInUnitsPerPixel(unitsPerPixel, 1);
      setUnitsPerPixel(newUnitsPerPixel);
    } else {
      final double scale = getScale();
      final long newScale = getZoomInScale(scale, 1);
      setScale(newScale);
    }
  }

  public void zoomOut() {
    if (this.zoomByUnitsPerPixel) {
      final double unitsPerPixel = getUnitsPerPixel();
      final double newUnitsPerPixel = getZoomOutUnitsPerPixel(unitsPerPixel, 1);
      setUnitsPerPixel(newUnitsPerPixel);
    } else {
      final double scale = getScale();
      final long newScale = getZoomOutScale(scale, 1);
      setScale(newScale);
    }
  }

}
