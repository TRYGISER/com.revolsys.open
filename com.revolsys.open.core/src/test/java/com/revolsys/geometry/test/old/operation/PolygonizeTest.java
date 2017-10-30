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
package com.revolsys.geometry.test.old.operation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.operation.polygonize.Polygonizer;

import junit.framework.TestCase;

/**
 * @version 1.7
 */
public class PolygonizeTest extends TestCase {
  public static void main(final String[] args) {
    junit.textui.TestRunner.run(PolygonizeTest.class);
  }

  private final GeometryFactory geometryFactory = GeometryFactory.DEFAULT_3D;

  public PolygonizeTest(final String name) {
    super(name);
  }

  private void compare(final Collection expectedGeometries, final Collection actualGeometries) {
    assertEquals(
      "Geometry count - expected " + expectedGeometries.size() + " but actual was "
        + actualGeometries.size() + " in " + actualGeometries,
      expectedGeometries.size(), actualGeometries.size());
    for (final Iterator i = expectedGeometries.iterator(); i.hasNext();) {
      final Geometry expectedGeometry = (Geometry)i.next();
      assertTrue("Expected to find: " + expectedGeometry + " in Actual result:" + actualGeometries,
        contains(actualGeometries, expectedGeometry));
    }
  }

  private boolean contains(final Collection geometries, final Geometry g) {
    for (final Iterator i = geometries.iterator(); i.hasNext();) {
      final Geometry element = (Geometry)i.next();
      if (element.equalsNorm(g)) {
        return true;
      }
    }

    return false;
  }

  private void doTest(final String[] inputWKT, final String[] expectedOutputWKT) {
    final Polygonizer polygonizer = new Polygonizer();
    polygonizer.add(toGeometries(inputWKT));
    compare(toGeometries(expectedOutputWKT), polygonizer.getPolygons());
  }

  /*
   * public void test2() { doTest(new String[]{ "LINESTRING(20 20, 20 100)",
   * "LINESTRING  (20 100, 20 180, 100 180)", "LINESTRING  (100 180, 180 180, 180 100)",
   * "LINESTRING  (180 100, 180 20, 100 20)", "LINESTRING  (100 20, 20 20)",
   * "LINESTRING  (100 20, 20 100)", "LINESTRING  (20 100, 100 180)",
   * "LINESTRING  (100 180, 180 100)", "LINESTRING  (180 100, 100 20)" }, new String[]{}); }
   */

  public void test1() {
    doTest(new String[] {
      "LINESTRING EMPTY", "LINESTRING EMPTY"
    }, new String[] {});
  }

  public void test2() {
    doTest(new String[] {
      "LINESTRING (100 180, 20 20, 160 20, 100 180)",
      "LINESTRING (100 180, 80 60, 120 60, 100 180)",
    }, new String[] {
      "POLYGON ((100 180, 120 60, 80 60, 100 180))",
      "POLYGON ((100 180, 160 20, 20 20, 100 180), (100 180, 80 60, 120 60, 100 180))"
    });
  }

  public void test3() {
    doTest(new String[] {
      "LINESTRING (0 0, 4 0)", "LINESTRING (4 0, 5 3)", "LINESTRING (5 3, 4 6, 6 6, 5 3)",
      "LINESTRING (5 3, 6 0)", "LINESTRING (6 0, 10 0, 5 10, 0 0)", "LINESTRING (4 0, 6 0)"
    }, new String[] {
      "POLYGON ((5 3, 4 0, 0 0, 5 10, 10 0, 6 0, 5 3), (5 3, 6 6, 4 6, 5 3))",
      "POLYGON ((5 3, 4 6, 6 6, 5 3))", "POLYGON ((4 0, 5 3, 6 0, 4 0))"
    });
  }

  private Collection toGeometries(final String[] inputWKT) {
    final ArrayList geometries = new ArrayList();
    for (final String element : inputWKT) {
      geometries.add(this.geometryFactory.geometry(element));
    }

    return geometries;
  }
}
