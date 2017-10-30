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

package com.revolsys.geometry.model;

import java.util.List;
import java.util.function.Function;

import com.revolsys.datatype.DataTypes;
import com.revolsys.geometry.model.editor.MultiPolygonEditor;
import com.revolsys.geometry.model.editor.PolygonalEditor;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.model.prep.PreparedMultiPolygon;

public interface Polygonal extends Geometry {
  @SuppressWarnings("unchecked")
  static <G extends Geometry> G newPolygonal(final Object value) {
    if (value == null) {
      return null;
    } else if (value instanceof Polygonal) {
      return (G)value;
    } else if (value instanceof Geometry) {
      final Geometry geometry = (Geometry)value;
      throw new IllegalArgumentException(
        "Expecting a Polygonal geometry not " + geometry.getGeometryType() + "\n" + geometry);
    } else {
      final String string = DataTypes.toString(value);
      final Geometry geometry = GeometryFactory.DEFAULT_3D.geometry(string, false);
      return (G)newPolygonal(geometry);
    }
  }

  Polygonal applyPolygonal(Function<Polygon, Polygon> function);

  @Override
  default boolean contains(final double x, final double y) {
    return locate(new PointDoubleXY(x, y)) != Location.EXTERIOR;
  }

  @Override
  default boolean contains(final double x, final double y, final double w, final double h) {
    return false;
  }

  default double getCoordinate(final int partIndex, final int ringIndex, final int vertexIndex,
    final int axisIndex) {
    final Polygon polygon = getGeometry(partIndex);
    if (polygon == null) {
      return Double.NaN;
    } else {
      return polygon.getCoordinate(ringIndex, vertexIndex, axisIndex);
    }
  }

  default double getM(final int partIndex, final int ringIndex, final int vertexIndex) {
    return getCoordinate(partIndex, ringIndex, vertexIndex, M);
  }

  Polygon getPolygon(int partIndex);

  @SuppressWarnings({
    "unchecked", "rawtypes"
  })
  default <V extends Polygon> List<V> getPolygons() {
    return (List)getGeometries();
  }

  default double getX(final int partIndex, final int ringIndex, final int vertexIndex) {
    return getCoordinate(partIndex, ringIndex, vertexIndex, X);
  }

  default double getY(final int partIndex, final int ringIndex, final int vertexIndex) {
    return getCoordinate(partIndex, ringIndex, vertexIndex, Y);
  }

  default double getZ(final int partIndex, final int ringIndex, final int vertexIndex) {
    return getCoordinate(partIndex, ringIndex, vertexIndex, Z);
  }

  @Override
  default boolean isContainedInBoundary(final BoundingBox boundingBox) {
    return false;
  }

  @Override
  default PolygonalEditor newGeometryEditor() {
    return new MultiPolygonEditor(this);
  }

  @Override
  default PolygonalEditor newGeometryEditor(final int axisCount) {
    final PolygonalEditor geometryEditor = newGeometryEditor();
    geometryEditor.setAxisCount(axisCount);
    return geometryEditor;
  }

  default Polygonal newPolygonal(final GeometryFactory geometryFactory, final Polygon... polygons) {
    return geometryFactory.polygonal(polygons);
  }

  @Override
  Polygonal normalize();

  default Iterable<Polygon> polygons() {
    return getGeometries();
  }

  @Override
  default Polygonal prepare() {
    return new PreparedMultiPolygon(this);
  }
}
