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
package com.revolsys.geometry.test.old.geom.prep;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.Polygon;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.model.util.SineStarFactory;
import com.revolsys.geometry.util.GeometricShapeFactory;

public abstract class StressTestHarness {
  private static final GeometryFactory geometryFactory = GeometryFactory.floating(0, 2);

  static final int MAX_ITER = 10000;

  private int numTargetPts = 1000;

  public StressTestHarness() {
  }

  public abstract boolean checkResult(Geometry target, Geometry test);

  Geometry newCircle(final Point origin, final double size, final int nPts) {
    final GeometricShapeFactory gsf = new GeometricShapeFactory();
    gsf.setCentre(origin);
    gsf.setSize(size);
    gsf.setNumPoints(nPts);
    final Geometry circle = gsf.newCircle();
    // Polygon gRect = gsf.createRectangle();
    // Geometry g = gRect.getExteriorRing();
    return circle;
  }

  Geometry newRandomTestGeometry(final BoundingBox env, final double size, final int nPts) {
    final double width = env.getWidth();
    final double xOffset = width * Math.random();
    final double yOffset = env.getHeight() * Math.random();
    final Point basePt = new PointDoubleXY(env.getMinX() + xOffset, env.getMinY() + yOffset);
    Geometry test = newTestCircle(basePt, size, nPts);
    if (test instanceof Polygon && Math.random() > 0.5) {
      test = test.getBoundary();
    }
    return test;
  }

  Geometry newSineStar(final Point origin, final double size, final int nPts) {
    final SineStarFactory gsf = new SineStarFactory();
    gsf.setCentre(origin);
    gsf.setSize(size);
    gsf.setNumPoints(nPts);
    gsf.setArmLengthRatio(0.1);
    gsf.setNumArms(20);
    final Geometry poly = gsf.newSineStar();
    return poly;
  }

  Geometry newTestCircle(final Point base, final double size, final int nPts) {
    final GeometricShapeFactory gsf = new GeometricShapeFactory();
    gsf.setCentre(base);
    gsf.setSize(size);
    gsf.setNumPoints(nPts);
    final Geometry circle = gsf.newCircle();
    // System.out.println(circle);
    return circle;
  }

  public void run(final int nIter) {
    // System.out.println("Running " + nIter + " tests");
    // Geometry poly = newCircle(new PointDoubleXY((double)0, 0), 100, nPts);
    final Geometry poly = newSineStar(new PointDoubleXY(0, 0), 100, this.numTargetPts);
    // System.out.println(poly);

    // System.out.println();
    // System.out.println("Running with " + nPts + " points");
    run(nIter, poly);
  }

  public void run(final int nIter, final Geometry target) {
    int count = 0;
    while (count < nIter) {
      count++;
      final Geometry test = newRandomTestGeometry(target.getBoundingBox(), 10, 20);

      // System.out.println("Test # " + count);
      // System.out.println(line);
      // System.out.println("Test[" + count + "] " + target.getClass() + "/" +
      // test.getClass());
      final boolean isResultCorrect = checkResult(target, test);
      if (!isResultCorrect) {
        throw new RuntimeException("Invalid result found");
      }
    }
  }

  public void setTargetSize(final int nPts) {
    this.numTargetPts = nPts;
  }

}
