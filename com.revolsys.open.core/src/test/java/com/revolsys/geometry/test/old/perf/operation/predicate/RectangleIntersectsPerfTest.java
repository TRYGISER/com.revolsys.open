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
package com.revolsys.geometry.test.old.perf.operation.predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.impl.BoundingBoxDoubleXY;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.model.util.SineStarFactory;
import com.revolsys.geometry.util.GeometricShapeFactory;
import com.revolsys.geometry.util.Stopwatch;

public class RectangleIntersectsPerfTest {
  private static final GeometryFactory geometryFactory = GeometryFactory.floating(0, 2);

  static final int MAX_ITER = 10;

  static final int NUM_AOI_PTS = 2000;

  static final int NUM_LINE_PTS = 1000;

  static final int NUM_LINES = 5000;

  public static void main(final String[] args) {
    final RectangleIntersectsPerfTest test = new RectangleIntersectsPerfTest();
    test.test();
  }

  Stopwatch sw = new Stopwatch();

  boolean testFailed = false;

  public RectangleIntersectsPerfTest() {
  }

  Geometry newRectangle(final Point origin, final double size) {
    final GeometricShapeFactory gsf = new GeometricShapeFactory();
    gsf.setCentre(origin);
    gsf.setSize(size);
    gsf.setNumPoints(4);
    final Geometry g = gsf.newRectangle();
    // Polygon gRect = gsf.createRectangle();
    // Geometry g = gRect.getExteriorRing();
    return g;
  }

  /**
   * Creates a set of rectangular Polygons which
   * cover the given envelope.
   * The rectangles
   * At least nRect rectangles are created.
   *
   * @param env
   * @param nRect
   * @param rectSize
   * @return
   */
  List<Geometry> newRectangles(final BoundingBox env, final int nRect, final double rectSize) {
    final int nSide = 1 + (int)Math.sqrt(nRect);
    final double dx = env.getWidth() / nSide;
    final double dy = env.getHeight() / nSide;

    final List<Geometry> rectList = new ArrayList<>();
    for (int i = 0; i < nSide; i++) {
      for (int j = 0; j < nSide; j++) {
        final double baseX = env.getMinX() + i * dx;
        final double baseY = env.getMinY() + j * dy;
        final BoundingBox envRect = new BoundingBoxDoubleXY(baseX, baseY, baseX + dx, baseY + dy);
        final Geometry rect = envRect.toGeometry();
        rectList.add(rect);
      }
    }
    return rectList;
  }

  Geometry newSineStar(final Point origin, final double size, final int nPts) {
    final SineStarFactory gsf = new SineStarFactory();
    gsf.setCentre(origin);
    gsf.setSize(size);
    gsf.setNumPoints(nPts);
    gsf.setArmLengthRatio(2);
    gsf.setNumArms(20);
    final Geometry poly = gsf.newSineStar();
    return poly;
  }

  public void test() {
    // test(5);
    // test(10);
    test(500);
    // test(1000);
    // test(2000);
    test(100000);
    /*
     * test(100); test(1000); test(2000); test(4000); test(8000);
     */
  }

  void test(final Collection<Geometry> rect, final Geometry g) {
    // System.out.println("Target # pts: " + g.getVertexCount()
    // + " -- # Rectangles: " + rect.size());

    final int maxCount = MAX_ITER;
    final Stopwatch sw = new Stopwatch();
    final int count = 0;
    for (int i = 0; i < MAX_ITER; i++) {
      for (final Geometry element : rect) {
        // rect[j].relate(g);
        element.intersects(g);
      }
    }
    // System.out.println("Finished in " + sw.getTimeString());
    // System.out.println();
  }

  void test(final int nPts) {
    final double size = 100;
    final Point origin = new PointDoubleXY(0, 0);
    final Geometry sinePoly = newSineStar(origin, size, nPts).getBoundary();
    GeometryFactory geometryFactory = sinePoly.getGeometryFactory();
    geometryFactory = GeometryFactory.fixed(geometryFactory.getCoordinateSystemId(),
      geometryFactory.getAxisCount(), size / 10, geometryFactory.getScaleZ());
    final Geometry newGeometry = sinePoly.convertGeometry(geometryFactory);
    /**
     * Make the geometry "crinkly" by rounding off the points.
     * This defeats the  MonotoneChain optimization in the full relate
     * algorithm, and provides a more realistic test.
     */
    final Geometry sinePolyCrinkly = newGeometry;
    final Geometry target = sinePolyCrinkly;

    final Geometry rect = newRectangle(origin, 5);
    // System.out.println(target);
    // System.out.println("Running with " + nPts + " points");
    testRectangles(target, 100, 5);
  }

  void testRectangles(final Geometry target, final int nRect, final double rectSize) {
    final Collection<Geometry> rects = newRectangles(target.getBoundingBox(), nRect, rectSize);
    test(rects, target);
  }

}
