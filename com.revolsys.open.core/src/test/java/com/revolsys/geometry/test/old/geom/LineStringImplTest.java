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

package com.revolsys.geometry.test.old.geom;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.Lineal;
import com.revolsys.geometry.model.LinearRing;
import com.revolsys.geometry.model.impl.PointDoubleXY;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 * Test for com.revolsys.geometry.testold.geom.impl.LineStringImpl.
 *
 * @version 1.7
 */
public class LineStringImplTest extends TestCase {

  public static void main(final String args[]) {
    TestRunner.run(suite());
  }

  public static Test suite() {
    return new TestSuite(LineStringImplTest.class);
  }

  private final GeometryFactory geometryFactory = GeometryFactory.fixed(0, 1000.0);

  public LineStringImplTest(final String name) {
    super(name);
  }

  public void testEquals1() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444)");
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444)");
    assertTrue(l1.equals(l2));
  }

  public void testEquals10() throws Exception {
    final GeometryFactory geometryFactotyr = GeometryFactory.fixed(0, 1.0);
    Geometry l1 = geometryFactotyr.geometry(
      "POLYGON((1732328800 519578384, 1732026179 519976285, 1731627364 519674014, 1731929984 519276112, 1732328800 519578384))");
    Geometry l2 = geometryFactotyr.geometry(
      "POLYGON((1731627364 519674014, 1731929984 519276112, 1732328800 519578384, 1732026179 519976285, 1731627364 519674014))");
    l1 = l1.normalize();
    l2 = l2.normalize();
    assertTrue(l1.equals(2, l2));
  }

  public void testEquals2() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444)");
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING(3.333 4.444, 1.111 2.222)");
    assertTrue(l1.equals(l2));
  }

  public void testEquals3() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444)");
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING(3.333 4.443, 1.111 2.222)");
    assertTrue(!l1.equals(l2));
  }

  public void testEquals4() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444)");
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING(3.333 4.4445, 1.111 2.222)");
    assertTrue(!l1.equals(l2));
  }

  public void testEquals5() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444)");
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING(3.333 4.4446, 1.111 2.222)");
    assertTrue(!l1.equals(l2));
  }

  public void testEquals6() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444, 5.555 6.666)");
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444, 5.555 6.666)");
    assertTrue(l1.equals(l2));
  }

  public void testEquals7() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 5.555 6.666, 3.333 4.444)");
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING(1.111 2.222, 3.333 4.444, 5.555 6.666)");
    assertTrue(!l1.equals(l2));
  }

  public void testEquals8() throws Exception {
    final GeometryFactory geometryFactory = GeometryFactory.fixed(0, 1000.0);
    final Lineal l1 = (Lineal)geometryFactory.geometry(
      "MULTILINESTRING((1732328800 519578384, 1732026179 519976285, 1731627364 519674014, 1731929984 519276112, 1732328800 519578384))");
    final Lineal l2 = (Lineal)geometryFactory.geometry(
      "MULTILINESTRING((1731627364 519674014, 1731929984 519276112, 1732328800 519578384, 1732026179 519976285, 1731627364 519674014))");
    assertTrue(l1.equals(l2));
  }

  public void testEquals9() throws Exception {
    final GeometryFactory geometryFactory = GeometryFactory.fixed(0, 1.0);
    final Lineal l1 = (Lineal)geometryFactory.geometry(
      "MULTILINESTRING((1732328800 519578384, 1732026179 519976285, 1731627364 519674014, 1731929984 519276112, 1732328800 519578384))");
    final Lineal l2 = (Lineal)geometryFactory.geometry(
      "MULTILINESTRING((1731627364 519674014, 1731929984 519276112, 1732328800 519578384, 1732026179 519976285, 1731627364 519674014))");
    assertTrue(l1.equals(l2));
  }

  public void testFiveZeros() {
    final GeometryFactory factory = GeometryFactory.floating(0, 2);
    final LineString line = factory.lineString(2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    final boolean closed = line.isClosed();
    assertTrue(closed);
  }

  public void testGetGeometryType() throws Exception {
    final LineString l = (LineString)this.geometryFactory.geometry("LINESTRING EMPTY");
    assertEquals("LineString", l.getGeometryType());
  }

  public void testIsClosed() throws Exception {
    final LineString l = (LineString)this.geometryFactory.geometry("LINESTRING EMPTY");
    assertTrue(l.isEmpty());
    assertTrue(!l.isClosed());

    final LinearRing r = this.geometryFactory.linearRing();
    assertTrue(r.isEmpty());
    assertTrue(r.isClosed());

    final Lineal m = this.geometryFactory.lineal(l, r);
    assertTrue(!m.isClosed());

    final Lineal m2 = this.geometryFactory.lineal(r);
    assertTrue(m2.isClosed());
  }

  public void testIsSimple() throws Exception {
    final LineString l1 = (LineString)this.geometryFactory
      .geometry("LINESTRING (0 0, 10 10, 10 0, 0 10, 0 0)");
    assertTrue(!l1.isSimple());
    final LineString l2 = (LineString)this.geometryFactory
      .geometry("LINESTRING (0 0, 10 10, 10 0, 0 10)");
    assertTrue(!l2.isSimple());
  }

  public void testLinearRingConstructor() throws Exception {
    try {
      final LinearRing ring = GeometryFactory.DEFAULT_3D.linearRing(2, 0.0, 0, 10.0, 10);
      assertTrue(false);
    } catch (final IllegalArgumentException e) {
      assertTrue(true);
    }
  }

  public void testUnclosedLinearRing() {
    try {
      final LinearRing linearRing = this.geometryFactory.linearRing(new PointDoubleXY(0.0, 0),
        new PointDoubleXY(1.0, 0), new PointDoubleXY(1.0, 1), new PointDoubleXY(2.0, 1));
      assertTrue(false);
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }

}
