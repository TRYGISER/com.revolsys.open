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
package com.revolsys.jts.geom.segment;

import com.revolsys.gis.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.LineString;
import com.revolsys.jts.geom.Point;
import com.revolsys.jts.geom.impl.PointDouble;

/**
 * Represents a line segment defined by two {@link Coordinates}s.
 * Provides methods to compute various geometric properties
 * and relationships of line segments.
 * <p>
 * This class is designed to be easily mutable (to the extent of
 * having its contained points public).
 * This supports a common pattern of reusing a single LineSegmentDouble
 * object as a way of computing segment properties on the
 * segments defined by arrays or lists of {@link Coordinates}s.
 *
 *@version 1.7
 */
public class LineSegmentDouble extends AbstractLineSegment {

  private double[] coordinates;

  public LineSegmentDouble() {
    this.coordinates = null;
  }

  protected LineSegmentDouble(final GeometryFactory geometryFactory,
    final int axisCount, final double... coordinates) {
    if (coordinates == null || coordinates.length == 0 || axisCount < 1) {
      this.coordinates = null;
    } else if (coordinates.length % axisCount == 0
      && coordinates.length / 2 == axisCount) {
      this.coordinates = coordinates;
      if (coordinates != null && geometryFactory != null) {
        int coordinateIndex = 0;
        for (int vertexIndex = 0; vertexIndex < 2; vertexIndex++) {
          for (int axisIndex = 0; axisIndex < axisCount; axisIndex++) {
            final double value = coordinates[coordinateIndex];
            coordinates[coordinateIndex] = geometryFactory.makePrecise(
              axisIndex, value);
            coordinateIndex++;
          }
        }
      }
    } else {
      throw new IllegalArgumentException("(coordinates.length) "
        + coordinates.length + " != 2 * " + axisCount + " (axisCount)");
    }
  }

  protected LineSegmentDouble(final GeometryFactory geometryFactory,
    final LineString line) {
    this(geometryFactory, line.getVertex(0), line.getVertex(-1));
  }

  protected LineSegmentDouble(final GeometryFactory geometryFactory,
    final Point point1, final Point point2) {
    final int axisCount = geometryFactory.getAxisCount();
    coordinates = new double[axisCount * 2];
    CoordinatesListUtil.setCoordinates(geometryFactory, coordinates, axisCount,
      0, point1);
    CoordinatesListUtil.setCoordinates(geometryFactory, coordinates, axisCount,
      1, point2);
  }

  public LineSegmentDouble(final int axisCount, final double... coordinates) {
    if (coordinates == null || coordinates.length == 0 || axisCount < 1) {
      this.coordinates = null;
    } else if (coordinates.length % axisCount == 0
      && coordinates.length / 2 == axisCount) {
      this.coordinates = coordinates;
    } else {
      throw new IllegalArgumentException("(coordinates.length) "
        + coordinates.length + " != 2 * " + axisCount + " (axisCount)");
    }
  }

  public LineSegmentDouble(final LineString line) {
    this(line.getVertex(0), line.getVertex(-1));
  }

  public LineSegmentDouble(final Point point1, final Point point2) {
    final int axisCount = Math.max(point1.getAxisCount(), point2.getAxisCount());
    coordinates = new double[axisCount * 2];
    CoordinatesListUtil.setCoordinates(coordinates, axisCount, 0, point1);
    CoordinatesListUtil.setCoordinates(coordinates, axisCount, 1, point2);
  }

  @Override
  public LineSegmentDouble clone() {
    final LineSegmentDouble clone = (LineSegmentDouble)super.clone();
    if (clone.coordinates != null) {
      clone.coordinates = clone.coordinates.clone();
    }
    return clone;
  }

  @Override
  protected LineSegment createLineSegment(
    final GeometryFactory geometryFactory, final int axisCount,
    final double... coordinates) {
    return new LineSegmentDouble(axisCount, coordinates);
  }

  @Override
  protected Point createPoint(final GeometryFactory geometryFactory,
    final double... coordinates) {
    return new PointDouble(coordinates);
  }

  @Override
  public int getAxisCount() {
    return coordinates.length / 2;
  }

  @Override
  public double getCoordinate(final int index, final int axisIndex) {
    final int axisCount = getAxisCount();
    if (axisIndex >= 0 && axisIndex < axisCount) {
      if (index >= 0 && index < 2) {
        final int valueIndex = index * axisCount + axisIndex;
        final double value = coordinates[valueIndex];
        return value;
      }
    }
    return Double.NaN;
  }

  @Override
  public double[] getCoordinates() {
    if (coordinates == null) {
      return coordinates;
    } else {
      return coordinates.clone();
    }
  }

  @Override
  public boolean isEmpty() {
    return coordinates == null;
  }

}