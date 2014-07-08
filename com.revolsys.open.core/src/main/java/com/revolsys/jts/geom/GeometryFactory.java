/*
 * The JTS Topology Suite is a collection of Java classes that
 * implement the fundamental operations required to validate a given
 * geo-spatial data set to a known topological specification.
 *
 * Copyright (C) 2001 Vivid Solutions
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * For more information, contact:
 *
 *     Vivid Solutions
 *     Suite #1A
 *     2328 Government Street
 *     Victoria BC  V8T 5G5
 *     Canada
 *
 *     (250)385-6040
 *     www.vividsolutions.com
 */
package com.revolsys.jts.geom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.revolsys.gis.cs.CoordinateSystem;
import com.revolsys.gis.cs.GeographicCoordinateSystem;
import com.revolsys.gis.cs.ProjectedCoordinateSystem;
import com.revolsys.gis.cs.epsg.EpsgCoordinateSystems;
import com.revolsys.gis.cs.esri.EsriCoordinateSystems;
import com.revolsys.gis.cs.projection.CoordinatesOperation;
import com.revolsys.gis.cs.projection.GeometryProjectionUtil;
import com.revolsys.gis.cs.projection.ProjectionFactory;
import com.revolsys.gis.data.model.types.DataType;
import com.revolsys.gis.data.model.types.DataTypes;
import com.revolsys.gis.model.coordinates.CoordinatesPrecisionModel;
import com.revolsys.gis.model.coordinates.DoubleCoordinates;
import com.revolsys.gis.model.coordinates.PrecisionModelUtil;
import com.revolsys.gis.model.coordinates.SimpleCoordinatesPrecisionModel;
import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesList;
import com.revolsys.gis.model.coordinates.list.DoubleCoordinatesListFactory;
import com.revolsys.io.map.InvokeMethodMapObjectFactory;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.io.map.MapSerializer;
import com.revolsys.io.wkt.WktParser;
import com.revolsys.jts.geom.impl.GeometryCollectionImpl;
import com.revolsys.jts.geom.impl.LineStringImpl;
import com.revolsys.jts.geom.impl.LinearRingImpl;
import com.revolsys.jts.geom.impl.MultiLineStringImpl;
import com.revolsys.jts.geom.impl.MultiPointImpl;
import com.revolsys.jts.geom.impl.MultiPolygonImpl;
import com.revolsys.jts.geom.impl.PointImpl;
import com.revolsys.jts.geom.impl.PolygonImpl;
import com.revolsys.jts.operation.linemerge.LineMerger;
import com.revolsys.util.CollectionUtil;

/**
 * Supplies a set of utility methods for building Geometry objects from lists
 * of Coordinates.
 * <p>
 * Note that the factory constructor methods do <b>not</b> change the input coordinates in any way.
 * In particular, they are not rounded to the supplied <tt>PrecisionModel</tt>.
 * It is assumed that input Coordinates meet the given precision.
 *
 *
 * @version 1.7
 */
public class GeometryFactory implements Serializable,
CoordinatesPrecisionModel, MapSerializer {
  public static void clear() {
    factories.clear();
  }

  public static GeometryFactory create(final Map<String, Object> properties) {
    final int srid = CollectionUtil.getInteger(properties, "srid", 0);
    final int axisCount = CollectionUtil.getInteger(properties, "axisCount", 2);
    final double scaleXY = CollectionUtil.getDouble(properties, "scaleXy", 0.0);
    final double scaleZ = CollectionUtil.getDouble(properties, "scaleZ", 0.0);
    return GeometryFactory.getFactory(srid, axisCount, scaleXY, scaleZ);
  }

  /**
   * <p>
   * Get a GeometryFactory with no coordinate system, 3D axis (x, y &amp; z) and
   * a floating precision model.
   * </p>
   *
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory() {
    return getFactory(0, 3, 0, 0);
  }

  /**
   * get a 3d geometry factory with a floating scale.
   */
  public static GeometryFactory getFactory(
    final CoordinateSystem coordinateSystem) {
    final int srid = getId(coordinateSystem);
    return getFactory(srid, 3, 0, 0);
  }

  public static GeometryFactory getFactory(
    final CoordinateSystem coordinateSystem, final int axisCount,
    final double scaleXY, final double scaleZ) {
    return new GeometryFactory(coordinateSystem, axisCount, scaleXY, scaleZ);
  }

  /**
   * <p>
   * Get a GeometryFactory with no coordinate system, 3D axis (x, y &amp; z) and
   * a fixed x, y & floating z precision models.
   * </p>
   *
   * @param scaleXY The scale factor used to round the x, y coordinates. The
   *          precision is 1 / scaleXy. A scale factor of 1000 will give a
   *          precision of 1 / 1000 = 1mm for projected coordinate systems using
   *          metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final double scaleXY) {
    return getFactory(0, 3, scaleXY, 0);
  }

  public static GeometryFactory getFactory(final Geometry geometry) {
    if (geometry == null) {
      return getFactory(0, 3, 0, 0);
    } else {
      return geometry.getGeometryFactory();
    }
  }

  /**
   * <p>
   * Get a GeometryFactory with the coordinate system, 3D axis (x, y &amp; z)
   * and a floating precision models.
   * </p>
   *
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG
   *          coordinate system id</a>.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid) {
    return getFactory(srid, 3, 0, 0);
  }

  /**
   * <p>
   * Get a GeometryFactory with the coordinate system, 2D axis (x &amp; y) and a
   * fixed x, y precision model.
   * </p>
   *
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG
   *          coordinate system id</a>.
   * @param scaleXY The scale factor used to round the x, y coordinates. The
   *          precision is 1 / scaleXy. A scale factor of 1000 will give a
   *          precision of 1 / 1000 = 1mm for projected coordinate systems using
   *          metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final double scaleXY) {
    return getFactory(srid, 2, scaleXY, 0);
  }

  /**
   * <p>
   * Get a GeometryFactory with no coordinate system, 3D axis (x, y &amp; z) and
   * a fixed x, y &amp; floating z precision models.
   * </p>
   *
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG
   *          coordinate system id</a>.
   * @param scaleXY The scale factor used to round the x, y coordinates. The
   *          precision is 1 / scaleXy. A scale factor of 1000 will give a
   *          precision of 1 / 1000 = 1mm for projected coordinate systems using
   *          metres.
   * @param scaleZ The scale factor used to round the z coordinates. The
   *          precision is 1 / scaleZ. A scale factor of 1000 will give a
   *          precision of 1 / 1000 = 1mm for projected coordinate systems using
   *          metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid,
    final double scaleXY, final double scaleZ) {
    return getFactory(srid, 3, scaleXY, scaleZ);
  }

  /**
   * <p>
   * Get a GeometryFactory with the coordinate system, number of axis and a
   * floating precision model.
   * </p>
   *
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG
   *          coordinate system id</a>.
   * @param axisCount The number of coordinate axis. 2 for 2D x &amp; y
   *          coordinates. 3 for 3D x, y &amp; z coordinates.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final int axisCount) {
    return getFactory(srid, axisCount, 0, 0);
  }

  /**
   * <p>
   * Get a GeometryFactory with the coordinate system, number of axis and a
   * fixed x, y &amp; fixed z precision models.
   * </p>
   *
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG
   *          coordinate system id</a>.
   * @param axisCount The number of coordinate axis. 2 for 2D x &amp; y
   *          coordinates. 3 for 3D x, y &amp; z coordinates.
   * @param scaleXY The scale factor used to round the x, y coordinates. The
   *          precision is 1 / scaleXy. A scale factor of 1000 will give a
   *          precision of 1 / 1000 = 1mm for projected coordinate systems using
   *          metres.
   * @param scaleZ The scale factor used to round the z coordinates. The
   *          precision is 1 / scaleZ. A scale factor of 1000 will give a
   *          precision of 1 / 1000 = 1mm for projected coordinate systems using
   *          metres.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final int srid, final int axisCount,
    final double scaleXY, final double scaleZ) {
    synchronized (factories) {
      final String key = srid + "-" + axisCount + "-" + scaleXY + "-" + scaleZ;
      GeometryFactory factory = factories.get(key);
      if (factory == null) {
        factory = new GeometryFactory(srid, axisCount, scaleXY, scaleZ);
        factories.put(key, factory);
      }
      return factory;
    }
  }

  /**
   * <p>
   * Get a GeometryFactory with the coordinate system, 3D axis (x, y &amp; z)
   * and a floating precision models.
   * </p>
   *
   * @param srid The <a href="http://spatialreference.org/ref/epsg/">EPSG
   *          coordinate system id</a>.
   * @return The geometry factory.
   */
  public static GeometryFactory getFactory(final String wkt) {
    final CoordinateSystem esriCoordinateSystem = EsriCoordinateSystems.getCoordinateSystem(wkt);
    if (esriCoordinateSystem == null) {
      return getFactory();
    } else {
      final CoordinateSystem epsgCoordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(esriCoordinateSystem);
      final int srid = epsgCoordinateSystem.getId();
      return getFactory(srid, 3, 0, 0);
    }
  }

  private static Set<DataType> getGeometryDataTypes(
    final Collection<? extends Geometry> geometries) {
    final Set<DataType> dataTypes = new LinkedHashSet<DataType>();
    for (final Geometry geometry : geometries) {
      final DataType dataType = geometry.getDataType();
      dataTypes.add(dataType);
    }
    return dataTypes;
  }

  private static int getId(final CoordinateSystem coordinateSystem) {
    if (coordinateSystem == null) {
      return 0;
    } else {
      return coordinateSystem.getId();
    }
  }

  public static GeometryFactory wgs84() {
    return getFactory(4326);
  }

  public static GeometryFactory worldMercator() {
    return getFactory(3857);
  }

  public static final MapObjectFactory FACTORY = new InvokeMethodMapObjectFactory(
    "geometryFactory", "Geometry Factory", GeometryFactory.class, "create");

  /** The cached geometry factories. */
  private static Map<String, GeometryFactory> factories = new HashMap<String, GeometryFactory>();

  private static final long serialVersionUID = 4328651897279304108L;

  private final PrecisionModel precisionModel;

  private final CoordinatesPrecisionModel coordinatesPrecisionModel;

  private final CoordinateSystem coordinateSystem;

  private int axisCount = 2;

  private final CoordinateSequenceFactory coordinateSequenceFactory;

  private final int srid;

  private final WktParser parser = new WktParser(this);

  protected GeometryFactory(final CoordinateSystem coordinateSystem,
    final int axisCount, final double scaleXY, final double scaleZ) {
    this.precisionModel = PrecisionModelUtil.getPrecisionModel(scaleXY);
    this.coordinateSequenceFactory = new DoubleCoordinatesListFactory();
    this.srid = coordinateSystem.getId();

    this.coordinateSystem = coordinateSystem;
    this.coordinatesPrecisionModel = new SimpleCoordinatesPrecisionModel(
      scaleXY, scaleZ);
    this.axisCount = Math.max(axisCount, 2);
  }

  protected GeometryFactory(final int srid, final int axisCount,
    final double scaleXY, final double scaleZ) {
    this.precisionModel = PrecisionModelUtil.getPrecisionModel(scaleXY);
    this.coordinateSequenceFactory = new DoubleCoordinatesListFactory();
    this.srid = srid;

    this.coordinateSystem = EpsgCoordinateSystems.getCoordinateSystem(srid);
    this.coordinatesPrecisionModel = new SimpleCoordinatesPrecisionModel(
      scaleXY, scaleZ);
    this.axisCount = Math.max(axisCount, 2);
  }

  public void addGeometries(final List<Geometry> geometryList,
    final Geometry geometry) {
    if (geometry != null && !geometry.isEmpty()) {
      for (final Geometry part : geometry.geometries()) {
        if (part != null && !part.isEmpty()) {
          geometryList.add(copy(part));
        }
      }
    }
  }

  /**
   *  Build an appropriate <code>Geometry</code>, <code>MultiGeometry</code>, or
   *  <code>GeometryCollection</code> to contain the <code>Geometry</code>s in
   *  it.
   * For example:<br>
   *
   *  <ul>
   *    <li> If <code>geomList</code> contains a single <code>Polygon</code>,
   *    the <code>Polygon</code> is returned.
   *    <li> If <code>geomList</code> contains several <code>Polygon</code>s, a
   *    <code>MultiPolygon</code> is returned.
   *    <li> If <code>geomList</code> contains some <code>Polygon</code>s and
   *    some <code>LineString</code>s, a <code>GeometryCollection</code> is
   *    returned.
   *    <li> If <code>geomList</code> is empty, an empty <code>GeometryCollection</code>
   *    is returned
   *  </ul>
   *
   * Note that this method does not "flatten" Geometries in the input, and hence if
   * any MultiGeometries are contained in the input a GeometryCollection containing
   * them will be returned.
   *
   *@param  geometries  the <code>Geometry</code>s to combine
   *@return           a <code>Geometry</code> of the "smallest", "most
   *      type-specific" class that can contain the elements of <code>geomList</code>
   *      .
   */
  public Geometry buildGeometry(final Collection<? extends Geometry> geometries) {

    /**
     * Determine some facts about the geometries in the list
     */
    DataType collectionDataType = null;
    boolean isHeterogeneous = false;
    boolean hasGeometryCollection = false;
    for (final Geometry geometry : geometries) {
      DataType geometryDataType = geometry.getDataType();
      if (geometry instanceof LinearRing) {
        geometryDataType = DataTypes.LINE_STRING;
      }
      if (collectionDataType == null) {
        collectionDataType = geometryDataType;
      } else if (geometryDataType != collectionDataType) {

        isHeterogeneous = true;
      }
      if (geometry instanceof GeometryCollection) {
        hasGeometryCollection = true;
      }
    }

    /**
     * Now construct an appropriate geometry to return
     */
    if (collectionDataType == null) {
      return geometryCollection();
    } else if (isHeterogeneous || hasGeometryCollection) {
      return geometryCollection(geometries);
    } else if (geometries.size() == 1) {
      return geometries.iterator().next();
    } else if (DataTypes.POINT.equals(collectionDataType)) {
      return multiPoint(geometries);
    } else if (DataTypes.LINE_STRING.equals(collectionDataType)) {
      return multiLineString(geometries);
    } else if (DataTypes.POLYGON.equals(collectionDataType)) {
      return multiPolygon(geometries);
    } else {
      throw new IllegalArgumentException("Unknown geometry type "
          + collectionDataType);
    }
  }

  public GeometryFactory convertAxisCount(final int axisCount) {
    if (axisCount == getAxisCount()) {
      return this;
    } else {
      final int srid = getSrid();
      final double scaleXY = getScaleXY();
      final double scaleZ = getScaleZ();
      return GeometryFactory.getFactory(srid, axisCount, scaleXY, scaleZ);
    }
  }

  @SuppressWarnings("unchecked")
  public <G extends Geometry> G copy(final G geometry) {
    return (G)geometry(geometry);
  }

  public double[] copyPrecise(final double[] values) {
    final double[] valuesPrecise = new double[values.length];
    makePrecise(values, valuesPrecise);
    return valuesPrecise;
  }

  public Coordinates createCoordinates(final Coordinates point) {
    final Coordinates newPoint = new DoubleCoordinates(point, this.axisCount);
    makePrecise(newPoint);
    return newPoint;
  }

  public Coordinates createCoordinates(final double... coordinates) {
    final Coordinates newPoint = new DoubleCoordinates(this.axisCount,
      coordinates);
    makePrecise(newPoint);
    return newPoint;
  }

  public CoordinatesList createCoordinatesList(final Collection<?> points) {
    if (points == null || points.isEmpty()) {
      return null;
    } else {
      final int numPoints = points.size();
      final int axisCount = getAxisCount();
      CoordinatesList coordinatesList = new DoubleCoordinatesList(numPoints,
        axisCount);
      int i = 0;
      for (final Object object : points) {
        Coordinates point;
        if (object == null) {
          point = null;
        } else if (object instanceof Coordinates) {
          point = (Coordinates)object;
        } else if (object instanceof Point) {
          final Point projectedPoint = copy((Point)object);
          point = projectedPoint;
        } else if (object instanceof double[]) {
          point = new DoubleCoordinates((double[])object);
        } else if (object instanceof CoordinatesList) {
          final CoordinatesList coordinates = (CoordinatesList)object;
          point = coordinates.get(0);
        } else {
          throw new IllegalArgumentException("Unexepected data type: " + object);
        }

        if (point != null && point.getAxisCount() > 1) {
          coordinatesList.setPoint(i, point);
          i++;
        }
      }
      if (i < coordinatesList.size()) {
        coordinatesList = coordinatesList.subList(0, i);
      }
      makePrecise(coordinatesList);
      return coordinatesList;
    }
  }

  public Geometry geometry() {
    return point();
  }

  /**
   * <p>
   * Create a new geometry of the requested target geometry class.
   * <p>
   *
   * @param targetClass
   * @param geometry
   * @return
   */
  @SuppressWarnings({
    "unchecked"
  })
  public <V extends Geometry> V geometry(final Class<?> targetClass,
    Geometry geometry) {
    if (geometry != null && !geometry.isEmpty()) {
      geometry = copy(geometry);
      if (geometry instanceof GeometryCollection) {
        if (geometry.getGeometryCount() == 1) {
          geometry = geometry.getGeometry(0);
        } else {
          geometry = geometry.union();
          // Union doesn't use this geometry factory
          geometry = copy(geometry);
        }
      }
      final Class<?> geometryClass = geometry.getClass();
      if (targetClass.isAssignableFrom(geometryClass)) {
        // TODO if geometry collection then clean up
        return (V)geometry;
      } else if (Point.class.isAssignableFrom(targetClass)) {
        if (geometry instanceof MultiPoint) {
          if (geometry.getGeometryCount() == 1) {
            return (V)geometry.getGeometry(0);
          }
        }
      } else if (LineString.class.isAssignableFrom(targetClass)) {
        if (geometry instanceof MultiLineString) {
          if (geometry.getGeometryCount() == 1) {
            return (V)geometry.getGeometry(0);
          } else {
            final LineMerger merger = new LineMerger();
            merger.add(geometry);
            final List<LineString> mergedLineStrings = (List<LineString>)merger.getMergedLineStrings();
            if (mergedLineStrings.size() == 1) {
              return (V)mergedLineStrings.get(0);
            }
          }
        }
      } else if (Polygon.class.isAssignableFrom(targetClass)) {
        if (geometry instanceof MultiPolygon) {
          if (geometry.getGeometryCount() == 1) {
            return (V)geometry.getGeometry(0);
          }
        }
      } else if (MultiPoint.class.isAssignableFrom(targetClass)) {
        if (geometry instanceof Point) {
          return (V)multiPoint(geometry);
        }
      } else if (MultiLineString.class.isAssignableFrom(targetClass)) {
        if (geometry instanceof LineString) {
          return (V)multiLineString(geometry);
        }
      } else if (MultiPolygon.class.isAssignableFrom(targetClass)) {
        if (geometry instanceof Polygon) {
          return (V)multiPolygon(geometry);
        }
      }
    }
    return null;
  }

  /**
   * Create a new geometry my flattening the input geometries, ignoring and null or empty
   * geometries. If there are no geometries an empty {@link GeometryCollection} will be returned.
   * If there is one geometry that single geometry will be returned. Otherwise the result
   * will be a subclass of {@link GeometryCollection}.
   *
   * @author Paul Austin <paul.austin@revolsys.com>
   * @param geometries
   * @return
   */
  @SuppressWarnings("unchecked")
  public <V extends Geometry> V geometry(
    final Collection<? extends Geometry> geometries) {
    final Collection<? extends Geometry> geometryList = getGeometries(geometries);
    if (geometryList == null || geometries.size() == 0) {
      return (V)geometryCollection();
    } else if (geometries.size() == 1) {
      return (V)CollectionUtil.get(geometries, 0);
    } else {
      final Set<DataType> dataTypes = getGeometryDataTypes(geometryList);
      if (dataTypes.size() == 1) {
        final DataType dataType = CollectionUtil.get(dataTypes, 0);
        if (dataType.equals(DataTypes.POINT)) {
          return (V)multiPoint(geometryList);
        } else if (dataType.equals(DataTypes.LINE_STRING)) {
          return (V)multiLineString(geometryList);
        } else if (dataType.equals(DataTypes.POLYGON)) {
          return (V)multiPolygon(geometryList);
        }
      }
      return (V)geometryCollection(geometries);
    }
  }

  @SuppressWarnings("unchecked")
  public <V extends Geometry> V geometry(final Geometry... geometries) {
    return (V)geometry(Arrays.asList(geometries));
  }

  /**
   * Creates a deep copy of the input {@link Geometry}.
   * The {@link CoordinateSequenceFactory} defined for this factory
   * is used to copy the {@link CoordinatesList}s
   * of the input geometry.
   * <p>
   * This is a convenient way to change the <tt>CoordinatesList</tt>
   * used to represent a geometry, or to change the
   * factory used for a geometry.
   * <p>
   * {@link Geometry#clone()} can also be used to make a deep copy,
   * but it does not allow changing the CoordinatesList type.
   *
   * @return a deep copy of the input geometry, using the CoordinatesList type of this factory
   *
   * @see Geometry#clone()
   */
  public Geometry geometry(final Geometry geometry) {
    if (geometry == null) {
      return null;
    } else {
      final int srid = getSrid();
      final int geometrySrid = geometry.getSrid();
      if (srid == 0 && geometrySrid != 0) {
        final GeometryFactory geometryFactory = GeometryFactory.getFactory(
          geometrySrid, this.axisCount, getScaleXY(), getScaleZ());
        return geometryFactory.geometry(geometry);
      } else if (srid != 0 && geometrySrid != 0 && geometrySrid != srid) {
        if (geometry instanceof MultiPoint) {
          final List<Geometry> geometries = new ArrayList<Geometry>();
          addGeometries(geometries, geometry);
          return multiPoint(geometries);
        } else if (geometry instanceof MultiLineString) {
          final List<Geometry> geometries = new ArrayList<Geometry>();
          addGeometries(geometries, geometry);
          return multiLineString(geometries);
        } else if (geometry instanceof MultiPolygon) {
          final List<Geometry> geometries = new ArrayList<Geometry>();
          addGeometries(geometries, geometry);
          return multiPolygon(geometries);
        } else if (geometry instanceof GeometryCollection) {
          final List<Geometry> geometries = new ArrayList<Geometry>();
          addGeometries(geometries, geometry);
          return geometryCollection(geometries);
        } else {
          return GeometryProjectionUtil.performCopy(geometry, this);
        }
      } else if (geometry instanceof MultiPoint) {
        final List<Geometry> geometries = new ArrayList<Geometry>();
        addGeometries(geometries, geometry);
        return multiPoint(geometries);
      } else if (geometry instanceof MultiLineString) {
        final List<Geometry> geometries = new ArrayList<Geometry>();
        addGeometries(geometries, geometry);
        return multiLineString(geometries);
      } else if (geometry instanceof MultiPolygon) {
        final List<Geometry> geometries = new ArrayList<Geometry>();
        addGeometries(geometries, geometry);
        return multiPolygon(geometries);
      } else if (geometry instanceof GeometryCollection) {
        final List<Geometry> geometries = new ArrayList<Geometry>();
        addGeometries(geometries, geometry);
        return geometryCollection(geometries);
      } else if (geometry instanceof Point) {
        final Point point = (Point)geometry;
        return point.copy(this);
      } else if (geometry instanceof LinearRing) {
        final LinearRing linearRing = (LinearRing)geometry;
        return linearRing.copy(this);
      } else if (geometry instanceof LineString) {
        final LineString lineString = (LineString)geometry;
        return lineString.copy(this);
      } else if (geometry instanceof Polygon) {
        final Polygon polygon = (Polygon)geometry;
        return polygon(polygon);
      } else {
        return null;
      }
    }
  }

  @SuppressWarnings("unchecked")
  public <T extends Geometry> T geometry(final String wkt) {
    return (T)this.parser.parseGeometry(wkt);
  }

  @SuppressWarnings("unchecked")
  public <T extends Geometry> T geometry(final String wkt,
    final boolean useAxisCountFromGeometryFactory) {
    return (T)this.parser.parseGeometry(wkt, useAxisCountFromGeometryFactory);
  }

  public GeometryCollection geometryCollection() {
    return new GeometryCollectionImpl(this);
  }

  @SuppressWarnings("unchecked")
  public <V extends GeometryCollection> V geometryCollection(
    final Collection<? extends Geometry> geometries) {
    final Set<DataType> dataTypes = new HashSet<>();
    final List<Geometry> geometryList = new ArrayList<>();
    if (geometries != null) {
      for (final Geometry geometry : geometries) {
        if (geometry != null) {
          dataTypes.add(geometry.getDataType());
          final Geometry copy = geometry.copy(this);
          geometryList.add(copy);
        }
      }
    }
    if (geometryList == null || geometryList.size() == 0) {
      return (V)geometryCollection();
    } else if (dataTypes.equals(Collections.singleton(DataTypes.POINT))) {
      return (V)multiPoint(geometryList);
    } else if (dataTypes.equals(Collections.singleton(DataTypes.LINE_STRING))) {
      return (V)multiLineString(geometryList);
    } else if (dataTypes.equals(Collections.singleton(DataTypes.POLYGON))) {
      return (V)multiPolygon(geometryList);
    } else {
      final Geometry[] geometryArray = new Geometry[geometries.size()];
      geometries.toArray(geometryArray);
      return (V)new GeometryCollectionImpl(this, geometryArray);
    }
  }

  @SuppressWarnings("unchecked")
  public <V extends GeometryCollection> V geometryCollection(
    final Geometry... geometries) {
    return (V)geometryCollection(Arrays.asList(geometries));
  }

  public int getAxisCount() {
    return this.axisCount;
  }

  public Coordinates getCoordinates(final Point point) {
    final Point convertedPoint = project(point);
    return convertedPoint;
  }

  public CoordinateSequenceFactory getCoordinateSequenceFactory() {
    return this.coordinateSequenceFactory;
  }

  /**
   * <p>Get the {@link CoordinatesOperation} to convert between this factory's and the other factory's
   * {@link CoordinateSystem}.</p>
   *
   * @author Paul Austin <paul.austin@revolsys.com>
   * @param geometryFactory The geometry factory to convert to.
   * @return The coordinates operation or null if no conversion is available.
   */
  public CoordinatesOperation getCoordinatesOperation(
    final GeometryFactory geometryFactory) {
    final CoordinateSystem coordinateSystem = getCoordinateSystem();
    final CoordinateSystem otherCoordinateSystem = geometryFactory.getCoordinateSystem();
    return ProjectionFactory.getCoordinatesOperation(coordinateSystem,
      otherCoordinateSystem);
  }

  public CoordinatesPrecisionModel getCoordinatesPrecisionModel() {
    return this.coordinatesPrecisionModel;
  }

  public CoordinateSystem getCoordinateSystem() {
    return this.coordinateSystem;
  }

  public GeometryFactory getGeographicGeometryFactory() {
    if (this.coordinateSystem instanceof GeographicCoordinateSystem) {
      return this;
    } else if (this.coordinateSystem instanceof ProjectedCoordinateSystem) {
      final ProjectedCoordinateSystem projectedCs = (ProjectedCoordinateSystem)this.coordinateSystem;
      final GeographicCoordinateSystem geographicCs = projectedCs.getGeographicCoordinateSystem();
      final int srid = geographicCs.getId();
      return getFactory(srid, getAxisCount(), 0, 0);
    } else {
      return getFactory(4326, getAxisCount(), 0, 0);
    }
  }

  public List<Geometry> getGeometries(
    final Collection<? extends Geometry> geometries) {
    final List<Geometry> geometryList = new ArrayList<Geometry>();
    for (final Geometry geometry : geometries) {
      addGeometries(geometryList, geometry);
    }
    return geometryList;
  }

  private LinearRing getLinearRing(final List<?> rings, final int index) {
    final Object ring = rings.get(index);
    if (ring instanceof LinearRing) {
      return (LinearRing)ring;
    } else if (ring instanceof CoordinatesList) {
      final CoordinatesList points = (CoordinatesList)ring;
      return linearRing(points);
    } else if (ring instanceof LineString) {
      final LineString line = (LineString)ring;
      final CoordinatesList points = CoordinatesListUtil.get(line);
      return linearRing(points);
    } else if (ring instanceof double[]) {
      final double[] coordinates = (double[])ring;
      final DoubleCoordinatesList points = new DoubleCoordinatesList(
        getAxisCount(), coordinates);
      return linearRing(points);
    } else {
      return null;
    }
  }

  public LineString[] getLineStringArray(final Collection<?> lines) {
    final List<LineString> lineStrings = new ArrayList<LineString>();
    for (final Object value : lines) {
      LineString lineString;
      if (value instanceof LineString) {
        lineString = (LineString)value;
      } else if (value instanceof CoordinatesList) {
        final CoordinatesList coordinates = (CoordinatesList)value;
        lineString = lineString(coordinates);
      } else if (value instanceof double[]) {
        final double[] points = (double[])value;
        lineString = lineString(getAxisCount(), points);
      } else {
        lineString = null;
      }
      if (lineString != null) {
        lineStrings.add(lineString);
      }
    }
    return lineStrings.toArray(new LineString[lineStrings.size()]);
  }

  public Point[] getPointArray(final Collection<?> pointsList) {
    final List<Point> points = new ArrayList<Point>();
    for (final Object object : pointsList) {
      final Point point = point(object);
      if (point != null && !point.isEmpty()) {
        points.add(point);
      }
    }
    return points.toArray(new Point[points.size()]);
  }

  @SuppressWarnings("unchecked")
  public Polygon[] getPolygonArray(final Collection<?> polygonList) {
    final List<Polygon> polygons = new ArrayList<Polygon>();
    for (final Object value : polygonList) {
      Polygon polygon;
      if (value instanceof Polygon) {
        polygon = (Polygon)value;
      } else if (value instanceof List) {
        final List<CoordinatesList> coordinateList = (List<CoordinatesList>)value;
        polygon = polygon(coordinateList);
      } else if (value instanceof CoordinatesList) {
        final CoordinatesList coordinateList = (CoordinatesList)value;
        polygon = polygon(coordinateList);
      } else {
        polygon = null;
      }
      if (polygon != null) {
        polygons.add(polygon);
      }
    }
    return polygons.toArray(new Polygon[polygons.size()]);
  }

  @Override
  public Coordinates getPreciseCoordinates(final Coordinates point) {
    return this.coordinatesPrecisionModel.getPreciseCoordinates(point);
  }

  /**
   * Returns the PrecisionModel that Geometries created by this factory
   * will be associated with.
   *
   * @return the PrecisionModel for this factory
   */
  public PrecisionModel getPrecisionModel() {
    return this.precisionModel;
  }

  @Override
  public double getResolutionXy() {
    return this.coordinatesPrecisionModel.getResolutionXy();
  }

  @Override
  public double getResolutionZ() {
    return this.coordinatesPrecisionModel.getResolutionZ();
  }

  @Override
  public double getScaleXY() {
    final CoordinatesPrecisionModel precisionModel = getCoordinatesPrecisionModel();
    return precisionModel.getScaleXY();
  }

  @Override
  public double getScaleZ() {
    final CoordinatesPrecisionModel precisionModel = getCoordinatesPrecisionModel();
    return precisionModel.getScaleZ();
  }

  /**
   * Gets the srid value defined for this factory.
   *
   * @return the factory srid value
   */
  public int getSrid() {
    return this.srid;
  }

  public boolean hasM() {
    return this.axisCount > 3;
  }

  public boolean hasZ() {
    return this.axisCount > 2;
  }

  @Override
  public boolean isFloating() {
    return this.coordinatesPrecisionModel.isFloating();
  }

  public LinearRing linearRing() {
    return new LinearRingImpl(this);
  }

  public LinearRing linearRing(final Collection<?> points) {
    if (points == null || points.isEmpty()) {
      return linearRing();
    } else {
      final CoordinatesList coordinatesList = createCoordinatesList(points);
      return linearRing(coordinatesList);
    }
  }

  /**
   * Creates a {@link LinearRing} using the given {@link Coordinates}s.
   * A null or empty array creates an empty LinearRing.
   * The points must form a closed and simple linestring.
   * @param coordinates an array without null elements, or an empty array, or null
   * @return the created LinearRing
   * @throws IllegalArgumentException if the ring is not closed, or has too few points
   */
  public LinearRing linearRing(final Coordinates... coordinates) {
    return linearRing(coordinates != null ? getCoordinateSequenceFactory().create(
      coordinates)
      : null);
  }

  /**
   * Creates a {@link LinearRing} using the given {@link CoordinatesList}.
   * A null or empty array creates an empty LinearRing.
   * The points must form a closed and simple linestring.
   *
   * @param coordinates a CoordinatesList (possibly empty), or null
   * @return the created LinearRing
   * @throws IllegalArgumentException if the ring is not closed, or has too few points
   */
  public LinearRing linearRing(final CoordinatesList points) {
    return new LinearRingImpl(this, points);
  }

  public LinearRing linearRing(final int axisCount, final double... coordinates) {
    return new LinearRingImpl(this, axisCount, coordinates);
  }

  public LinearRing linearRing(final LineString lineString) {
    return linearRing(lineString.getCoordinatesList());
  }

  public LineString lineString() {
    return new LineStringImpl(this);
  }

  public LineString lineString(final Collection<?> points) {
    if (points.isEmpty()) {
      return lineString();
    } else {
      final CoordinatesList coordinatesList = createCoordinatesList(points);
      return lineString(coordinatesList);
    }
  }

  public LineString lineString(final Coordinates... points) {
    if (points == null) {
      return lineString();
    } else {
      final List<Coordinates> pointList = Arrays.asList(points);
      return lineString(pointList);
    }
  }

  /**
   * Creates a LineString using the given CoordinatesList.
   * A null or empty CoordinatesList creates an empty LineString.
   *
   * @param coordinates a CoordinatesList (possibly empty), or null
   */
  public LineString lineString(final CoordinatesList points) {
    return new LineStringImpl(this, points);
  }

  public LineString lineString(final int axisCount, final double... coordinates) {
    return new LineStringImpl(this, axisCount, coordinates);
  }

  public LineString lineString(final LineString lineString) {
    if (lineString == null || lineString.isEmpty()) {
      return lineString();
    } else {
      return new LineStringImpl(this, lineString.getCoordinatesList());
    }
  }

  @Override
  public void makePrecise(final Coordinates point) {
    this.coordinatesPrecisionModel.makePrecise(point);
  }

  public void makePrecise(final Coordinates... points) {
    for (final Coordinates point : points) {
      this.coordinatesPrecisionModel.makePrecise(point);
    }
  }

  public void makePrecise(final CoordinatesList points) {
    points.makePrecise(this.coordinatesPrecisionModel);
  }

  public void makePrecise(final double[] values) {
    makePrecise(values, values);
  }

  public void makePrecise(final double[] values, final double[] valuesPrecise) {
    for (int i = 0; i < valuesPrecise.length; i++) {
      final int axisIndex = i % this.axisCount;
      valuesPrecise[i] = makePrecise(axisIndex, values[i]);
    }
  }

  public double makePrecise(final int axisIndex, final double value) {
    if (axisIndex < 2) {
      return makeXyPrecise(value);
    } else if (axisIndex == 2) {
      return makeZPrecise(value);
    } else {
      return value;
    }
  }

  @Override
  public void makePrecise(final int axisCount, final double... coordinates) {
    this.coordinatesPrecisionModel.makePrecise(axisCount, coordinates);
  }

  public void makePrecise(final Iterable<Coordinates> points) {
    for (final Coordinates point : points) {
      this.coordinatesPrecisionModel.makePrecise(point);
    }
  }

  @Override
  public double makeXyPrecise(final double value) {
    return this.coordinatesPrecisionModel.makeXyPrecise(value);
  }

  @Override
  public double makeZPrecise(final double value) {
    return this.coordinatesPrecisionModel.makeZPrecise(value);
  }

  public MultiLineString multiLineString(final Collection<?> lines) {
    final LineString[] lineArray = getLineStringArray(lines);
    return multiLineString(lineArray);
  }

  /**
   * Creates a MultiLineString using the given LineStrings; a null or empty
   * array will create an empty MultiLineString.
   *
   * @param lineStrings LineStrings, each of which may be empty but not null
   * @return the created MultiLineString
   */
  public MultiLineString multiLineString(final LineString[] lineStrings) {
    return new MultiLineStringImpl(lineStrings, this);
  }

  public MultiLineString multiLineString(final Object... lines) {
    return multiLineString(Arrays.asList(lines));
  }

  public MultiPoint multiPoint() {
    return new MultiPointImpl(this);
  }

  public MultiPoint multiPoint(final Collection<?> points) {
    final Point[] pointArray = getPointArray(points);
    return multiPoint(pointArray);
  }

  /**
   * Creates a {@link MultiPoint} using the given {@link Coordinates}s.
   * A null or empty array will create an empty MultiPoint.
   *
   * @param coordinates an array (without null elements), or an empty array, or <code>null</code>
   * @return a MultiPoint object
   */
  public MultiPoint multiPoint(final Coordinates[] coordinates) {
    return multiPoint(coordinates != null ? getCoordinateSequenceFactory().create(
      coordinates)
      : null);
  }

  /**
   * Creates a {@link MultiPoint} using the
   * points in the given {@link CoordinatesList}.
   * A <code>null</code> or empty CoordinatesList creates an empty MultiPoint.
   *
   * @param coordinates a CoordinatesList (possibly empty), or <code>null</code>
   * @return a MultiPoint geometry
   */
  public MultiPoint multiPoint(final CoordinatesList coordinatesList) {
    if (coordinatesList == null) {
      return multiPoint();
    } else {
      final Point[] points = new Point[coordinatesList.size()];
      for (int i = 0; i < points.length; i++) {
        final Coordinates coordinates = coordinatesList.get(i);
        final Point point = point(coordinates);
        points[i] = point;
      }
      return multiPoint(points);
    }
  }

  public MultiPoint multiPoint(final Object... points) {
    return multiPoint(Arrays.asList(points));
  }

  /**
   * Creates a {@link MultiPoint} using the given {@link Point}s.
   * A null or empty array will create an empty MultiPoint.
   *
   * @param point an array of Points (without null elements), or an empty array, or <code>null</code>
   * @return a MultiPoint object
   */
  public MultiPoint multiPoint(final Point[] point) {
    return new MultiPointImpl(this, point);
  }

  public MultiPolygon multiPolygon(final Collection<?> polygons) {
    final Polygon[] polygonArray = getPolygonArray(polygons);
    return multiPolygon(polygonArray);
  }

  public MultiPolygon multiPolygon(final Object... polygons) {
    return multiPolygon(Arrays.asList(polygons));
  }

  /**
   * Creates a MultiPolygon using the given Polygons; a null or empty array
   * will create an empty Polygon. The polygons must conform to the
   * assertions specified in the <A
   * HREF="http://www.opengis.org/techno/specs.htm">OpenGIS Simple Features
   * Specification for SQL</A>.
   *
   * @param polygons
   *            Polygons, each of which may be empty but not null
   * @return the created MultiPolygon
   */
  public MultiPolygon multiPolygon(final Polygon[] polygons) {
    return new MultiPolygonImpl(polygons, this);
  }

  /**
   * <p>Create an empty {@link Point}.</p>
   *
   * @return The point.
   */
  public Point point() {
    return new PointImpl(this);
  }

  /**
   * <p>Create a new {@link Point} from the specified point ({@link Coordinates}).
   * If the point is null or has {@link Coordinates#getAxisCount()} &lt; 2 an empty
   * point will be returned. The result point will have the same  {@link #getAxisCount()} from this
   * factory. Additional axis in the point will be ignored. If the point has a smaller
   * {@link Point#getAxisCount()} then {@link Double#NaN} will be used for that axis.</p>
   *
   * @param point The coordinates to create the point from.
   * @return The point.
   */
  public Point point(final Coordinates point) {
    if (point == null) {
      return point();
    } else {
      return point(point.getCoordinates());
    }
  }

  /**
   * Creates a Point using the given CoordinatesList; a null or empty
   * CoordinatesList will create an empty Point.
   *
   * @param points a CoordinatesList (possibly empty), or null
   * @return the created Point
   */
  public Point point(final CoordinatesList points) {
    if (points == null) {
      return point();
    } else {
      final int size = points.size();
      if (size == 0) {
        return point();
      } else if (size == 1) {
        final int axisCount = Math.min(points.getAxisCount(), getAxisCount());
        final double[] coordinates = new double[axisCount];
        for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
          final double coordinate = points.getValue(0, axisIndex);
          coordinates[axisIndex] = coordinate;
        }
        return point(coordinates);
      } else {
        throw new IllegalArgumentException("Point can only have 1 vertex not "
            + size);
      }
    }
  }

  /**
   * <p>Create a new {@link Point} from the specified point coordinates.
   * If the point is null or has length < 2 an empty point will be returned.
   * The result point will have the same  {@link #getAxisCount()} from this factory.
   * Additional coordinates in the point will be ignored. If the point length is &lt;
   * {@link #getAxisCount()} then {@link Double#NaN} will be used for that axis.</p>
   *
   * @param point The coordinates to create the point from.
   * @return The point.
   */
  public Point point(final double... coordinates) {
    if (coordinates == null || coordinates.length < 2) {
      return point();
    } else {
      return new PointImpl(this, coordinates);
    }
  }

  /**
   * <p>Create a new {@link Point} from the object using the following rules.<p>
   * <ul>
   *   <li><code>null</code> using {@link #point()}</li>
   *   <li>Instances of {@link Point} using {@link Point#copy(GeometryFactory)}</li>
   *   <li>Instances of {@link Coordinates} using {@link #point(Coordinates)}</li>
   *   <li>Instances of {@link CoordinatesList} using {@link #point(CoordinatesList)}</li>
   *   <li>Instances of {@link double[]} using {@link #point(double[])}</li>
   *   <li>Instances of any other class throws {@link IllegalArgumentException}.<li>
   * </ul>
   *
   * @param point The coordinates to create the point from.
   * @return The point.
   * @throws IllegalArgumentException If the object is not an instance of a supported class.
   */
  public Point point(final Object object) {
    if (object == null) {
      return point();
    } else if (object instanceof Point) {
      final Point point = (Point)object;
      return point.copy(this);
    } else if (object instanceof double[]) {
      return point((double[])object);
    } else if (object instanceof Coordinates) {
      return point((Coordinates)object);
    } else if (object instanceof CoordinatesList) {
      return point((CoordinatesList)object);
    } else {
      throw new IllegalArgumentException("Cannot create a point from "
          + object.getClass());
    }
  }

  public PolygonImpl polygon() {
    return new PolygonImpl(this);
  }

  public Polygon polygon(final CoordinatesList... rings) {
    final List<CoordinatesList> ringList = Arrays.asList(rings);
    return polygon(ringList);
  }

  /**
   * Constructs a <code>Polygon</code> with the given exterior boundary.
   *
   * @param shell
   *            the outer boundary of the new <code>Polygon</code>, or
   *            <code>null</code> or an empty <code>LinearRing</code> if
   *            the empty geometry is to be created.
   * @throws IllegalArgumentException if the boundary ring is invalid
   */
  public Polygon polygon(final LinearRing shell) {
    return new PolygonImpl(this, shell);
  }

  public Polygon polygon(final List<?> rings) {
    if (rings.size() == 0) {
      return polygon();
    } else {
      final LinearRing[] linearRings = new LinearRing[rings.size()];
      for (int i = 0; i < rings.size(); i++) {
        linearRings[i] = getLinearRing(rings, i);
      }
      return new PolygonImpl(this, linearRings);
    }
  }

  public Polygon polygon(final Object... rings) {
    return polygon(Arrays.asList(rings));
  }

  public Polygon polygon(final Polygon polygon) {
    final List<LinearRing> rings = new ArrayList<LinearRing>();
    for (final LinearRing ring : polygon.rings()) {
      final LinearRing clone = ring.copy(this);
      rings.add(clone);

    }
    return polygon(rings);
  }

  /**
   * Project the geometry if it is in a different coordinate system
   *
   * @param geometry
   * @return
   */
  public <G extends Geometry> G project(final G geometry) {
    return GeometryProjectionUtil.perform(geometry, this);
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = new LinkedHashMap<String, Object>();
    map.put("type", "geometryFactory");
    map.put("srid", getSrid());
    map.put("axisCount", getAxisCount());

    final double scaleXY = getScaleXY();
    if (scaleXY > 0) {
      map.put("scaleXy", scaleXY);
    }
    if (this.axisCount > 2) {
      final double scaleZ = getScaleZ();
      if (scaleZ > 0) {
        map.put("scaleZ", scaleZ);
      }
    }
    return map;
  }

  @Override
  public String toString() {
    final StringBuffer string = new StringBuffer();
    final int srid = getSrid();
    if (this.coordinateSystem != null) {
      string.append(this.coordinateSystem.getName());
      string.append(", ");
    }
    string.append("srid=");
    string.append(srid);
    string.append(", axisCount=");
    string.append(this.axisCount);
    final double scaleXY = this.coordinatesPrecisionModel.getScaleXY();
    string.append(", scaleXy=");
    if (scaleXY <= 0) {
      string.append("floating");
    } else {
      string.append(scaleXY);
    }
    if (hasZ()) {
      final double scaleZ = this.coordinatesPrecisionModel.getScaleZ();
      string.append(", scaleZ=");
      if (scaleZ <= 0) {
        string.append("floating");
      } else {
        string.append(scaleZ);
      }
    }
    return string.toString();
  }
}
