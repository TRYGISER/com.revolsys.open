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
package com.revolsys.geometry.test.function;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.coordinates.list.CoordinatesListUtil;
import com.revolsys.geometry.model.segment.LineSegmentDouble;
import com.revolsys.geometry.util.Triangles;

public class TriangleFunctions {

  public static Geometry angleBisectors(final Geometry g) {
    final Point[] pts = trianglePts(g);
    final Point cc = Triangles.inCentre(pts[0], pts[1], pts[2]);
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    final LineString[] line = new LineString[3];
    line[0] = geomFact.lineString(pts[0], cc);
    line[1] = geomFact.lineString(pts[1], cc);
    line[2] = geomFact.lineString(pts[2], cc);
    return geomFact.lineal(line);
  }

  public static Geometry centroid(final Geometry geometry) {
    return geometry.applyGeometry((final Geometry part) -> {
      final Point[] pts = trianglePts(part);
      final Point cc = Triangles.centroid(pts[0], pts[1], pts[2]);
      final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(part);
      return geomFact.point(cc);
    });
  }

  public static Geometry circumcentre(final Geometry geometry) {
    return geometry.applyGeometry((final Geometry part) -> {
      final Point[] pts = trianglePts(part);
      final Point cc = Triangles.circumcentre(pts[0], pts[1], pts[2]);
      final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(part);
      return geomFact.point(cc);
    });
  }

  public static Geometry incentre(final Geometry geometry) {
    return geometry.applyGeometry((final Geometry part) -> {
      final Point[] pts = trianglePts(part);
      final Point cc = Triangles.inCentre(pts[0], pts[1], pts[2]);
      final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(part);
      return geomFact.point(cc);
    });
  }

  public static Geometry perpendicularBisectors(final Geometry g) {
    final Point[] pts = trianglePts(g);
    final Point cc = Triangles.circumcentre(pts[0], pts[1], pts[2]);
    final GeometryFactory geomFact = FunctionsUtil.getFactoryOrDefault(g);
    final LineString[] line = new LineString[3];
    final Point p0 = new LineSegmentDouble(pts[1], pts[2]).closestPoint(cc);
    line[0] = geomFact.lineString(p0, cc);
    final Point p1 = new LineSegmentDouble(pts[0], pts[2]).closestPoint(cc);
    line[1] = geomFact.lineString(p1, cc);
    final Point p2 = new LineSegmentDouble(pts[0], pts[1]).closestPoint(cc);
    line[2] = geomFact.lineString(p2, cc);
    return geomFact.lineal(line);
  }

  private static Point[] trianglePts(final Geometry g) {
    final Point[] pts = CoordinatesListUtil.getPointArray(g, 3);
    if (pts.length < 3) {
      throw new IllegalArgumentException("Input geometry must have at least 3 points");
    }
    return pts;
  }
}
