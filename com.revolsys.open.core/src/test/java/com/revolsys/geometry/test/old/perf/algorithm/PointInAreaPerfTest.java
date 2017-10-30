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
package com.revolsys.geometry.test.old.perf.algorithm;

import com.revolsys.geometry.algorithm.locate.PointOnGeometryLocator;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.util.SineStarFactory;
import com.revolsys.geometry.util.Stopwatch;

public class PointInAreaPerfTest {

  public static void main(final String args[]) {
    final PointInAreaPerfTest test = new PointInAreaPerfTest();
    test.run();
  }

  public PointInAreaPerfTest() {
  }

  public void run() {
    final GeometryFactory geomFactory = GeometryFactory.DEFAULT_3D;

    final SineStarFactory ssFact = new SineStarFactory();
    ssFact.setSize(1000.0);
    ssFact.setNumPoints(2000);
    ssFact.setArmLengthRatio(0.1);
    ssFact.setNumArms(100);

    final Geometry area = ssFact.newSineStar();
    // System.out.println(area);

    final Stopwatch sw = new Stopwatch();

    final PointOnGeometryLocator pia = new MCIndexedPointInAreaLocator(area);
    // PointInAreaLocator pia = new IntervalIndexedPointInAreaLocator(area);
    // PointInAreaLocator pia = new SimplePointInAreaLocator(area);

    final PointInAreaPerfTester perfTester = new PointInAreaPerfTester(geomFactory, area);
    perfTester.setNumPoints(50000);
    perfTester.setPIA(pia);
    perfTester.run();

    // System.out.println("Overall time: " + sw.getTimeString());
  }
}
