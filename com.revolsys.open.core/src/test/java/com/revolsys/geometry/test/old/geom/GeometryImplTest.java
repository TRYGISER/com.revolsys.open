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
import com.revolsys.geometry.model.LinearRing;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.Polygon;
import com.revolsys.geometry.wkb.ParseException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @version 1.7
 */
public class GeometryImplTest extends TestCase {
  private interface CollectionFactory {
    Geometry newCollection(Geometry... geometries);
  }

  public static void main(final String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }

  public static Test suite() {
    return new TestSuite(GeometryImplTest.class);
  }

  private final GeometryFactory geometryFactory = GeometryFactory.fixed(0, 1.0, 1.0);

  private final GeometryFactory geometryFactoryFloat = GeometryFactory.DEFAULT_3D;

  public GeometryImplTest(final String name) {
    super(name);
  }

  private void doTestEquals(final Geometry a, final Geometry b, final boolean equalsGeometry,
    final boolean equalsObject, final boolean equalsExact, final boolean equalsHash) {
    assertEquals(equalsGeometry, a.equals(b));
    assertEquals(equalsObject, a.equals((Object)b));
    assertEquals(equalsExact, a.equals(2, b));
    assertEquals(equalsHash, a.hashCode() == b.hashCode());
  }

  private void doTestEqualsExact(final Geometry x, final Geometry somethingExactlyEqual,
    final Geometry somethingEqualButNotExactly, final Geometry somethingNotEqualButSameClass)
    throws Exception {
    Geometry differentClass;

    if (x instanceof Point) {
      differentClass = this.geometryFactory.geometry("POLYGON((0 0,0 50,50 43949,50 0,0 0))");
    } else {
      differentClass = this.geometryFactory.geometry("POINT(2351 1563)");
    }

    assertTrue(x.equals(2, x));
    assertTrue(x.equals(2, somethingExactlyEqual));
    assertTrue(somethingExactlyEqual.equals(2, x));
    // assertTrue(!x.equalsExact(somethingEqualButNotExactly));
    // assertTrue(!somethingEqualButNotExactly.equalsExact(x));
    // assertTrue(!x.equalsExact(somethingEqualButNotExactly));
    // assertTrue(!somethingEqualButNotExactly.equalsExact(x));
    assertTrue(!x.equals(2, differentClass));
    assertTrue(!differentClass.equals(2, x));
  }

  private void doTestEqualsExact(final Geometry x, final Geometry somethingExactlyEqual,
    final Geometry somethingNotEqualButSameClass, final Geometry sameClassButEmpty,
    final Geometry anotherSameClassButEmpty, final CollectionFactory collectionFactory)
    throws Exception {
    Geometry emptyDifferentClass;

    if (x instanceof Point) {
      emptyDifferentClass = this.geometryFactory.geometryCollection();
    } else {
      emptyDifferentClass = this.geometryFactory.point();
    }

    doTestEqualsExact(x, somethingExactlyEqual, collectionFactory.newCollection(x),
      somethingNotEqualButSameClass);

    doTestEqualsExact(sameClassButEmpty, anotherSameClassButEmpty, emptyDifferentClass, x);

    /**
     * Test comparison of non-empty versus empty.
     */
    doTestEqualsExact(x, somethingExactlyEqual, sameClassButEmpty, sameClassButEmpty);
  }

  private void doTestFromCommcast2003AtYahooDotCa(final GeometryFactory geometryFactory)
    throws ParseException {
    this.geometryFactoryFloat
      .geometry(
        "POLYGON ((708653.498611049 2402311.54647056, 708708.895756966 2402203.47250014, 708280.326454234 2402089.6337791, 708247.896591321 2402252.48269854, 708367.379593851 2402324.00761653, 708248.882609455 2402253.07294874, 708249.523621829 2402244.3124463, 708261.854734465 2402182.39086576, 708262.818392579 2402183.35452387, 708653.498611049 2402311.54647056))")
      .intersection(geometryFactory.geometry(
        "POLYGON ((708258.754920656 2402197.91172757, 708257.029447455 2402206.56901508, 708652.961095455 2402312.65463437, 708657.068786251 2402304.6356364, 708258.754920656 2402197.91172757))"));
  }

  public void testDepthMismatchAssertionFailedException() throws Exception {
    // register@robmeek.com reported an assertion failure
    // ("depth mismatch at (160.0, 300.0, Nan)") [Jon Aquino 10/28/2003]
    this.geometryFactory.geometry("MULTIPOLYGON (((100 300, 100 400, 200 400, 200 300, 100 300)),"
      + "((160 300, 160 400, 260 400, 260 300, 160 300)),"
      + "((160 300, 160 200, 260 200, 260 300, 160 300)))").buffer(0);
  }

  public void testEmptyGeometryCentroid() throws Exception {
    assertTrue(this.geometryFactory.geometry("POINT EMPTY").getCentroid().isEmpty());
    assertTrue(this.geometryFactory.geometry("POLYGON EMPTY").getCentroid().isEmpty());
    assertTrue(this.geometryFactory.geometry("LINESTRING EMPTY").getCentroid().isEmpty());
    assertTrue(this.geometryFactory.geometry("GEOMETRYCOLLECTION EMPTY").getCentroid().isEmpty());
    assertTrue(this.geometryFactory
      .geometry("GEOMETRYCOLLECTION(GEOMETRYCOLLECTION EMPTY, GEOMETRYCOLLECTION EMPTY)")
      .getCentroid()
      .isEmpty());
    assertTrue(this.geometryFactory.geometry("MULTIPOLYGON EMPTY").getCentroid().isEmpty());
    assertTrue(this.geometryFactory.geometry("MULTILINESTRING EMPTY").getCentroid().isEmpty());
    assertTrue(this.geometryFactory.geometry("MULTIPOINT EMPTY").getCentroid().isEmpty());
  }

  public void testEquals() throws Exception {
    final Geometry g = this.geometryFactory.geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    final Geometry same = this.geometryFactory.geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    final Geometry differentStart = this.geometryFactory
      .geometry("POLYGON ((0 50, 50 50, 50 0, 0 0, 0 50))");
    final Geometry differentFourth = this.geometryFactory
      .geometry("POLYGON ((0 0, 0 50, 50 50, 50 -99, 0 0))");
    final Geometry differentSecond = this.geometryFactory
      .geometry("POLYGON ((0 0, 0 99, 50 50, 50 0, 0 0))");
    doTestEquals(g, same, true, true, true, true);
    doTestEquals(g, differentStart, true, false, false, true);
    doTestEquals(g, differentFourth, false, false, false, false);
    doTestEquals(g, differentSecond, false, false, false, false);
  }

  public void testEquals1() throws Exception {
    final Geometry polygon1 = this.geometryFactory
      .geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    final Geometry polygon2 = this.geometryFactory
      .geometry("POLYGON ((50 50, 50 0, 0 0, 0 50, 50 50))");
    assertTrue(polygon1.equals(polygon2));
  }

  public void testEqualsExactForGeometryCollections() throws Exception {
    final Geometry polygon1 = this.geometryFactory
      .geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    final Geometry polygon2 = this.geometryFactory
      .geometry("POLYGON ((50 50, 50 0, 0 0, 0 50, 50 50))");
    final Geometry x = this.geometryFactory.geometry(polygon1, polygon2);
    final Geometry somethingExactlyEqual = this.geometryFactory.geometry(polygon1, polygon2);
    final Geometry somethingNotEqualButSameClass = this.geometryFactory.geometry(polygon2);
    final Geometry sameClassButEmpty = this.geometryFactory.geometryCollection();
    final Geometry anotherSameClassButEmpty = this.geometryFactory.geometryCollection();
    final CollectionFactory collectionFactory = new CollectionFactory() {
      @Override
      public Geometry newCollection(final Geometry... geometries) {
        return GeometryImplTest.this.geometryFactory.geometry(geometries);
      }
    };

    doTestEqualsExact(x, somethingExactlyEqual, somethingNotEqualButSameClass, sameClassButEmpty,
      anotherSameClassButEmpty, collectionFactory);
  }

  // public void testEquals2() throws Exception {
  // Geometry lineString =
  // reader.read("LINESTRING(0 0, 0 50, 50 50, 50 0, 0 0)");
  // Geometry geometryCollection =
  // reader.read("GEOMETRYCOLLECTION ( LINESTRING(0 0 , 0 50), "
  // + "LINESTRING(0 50 , 50 50), "
  // + "LINESTRING(50 50, 50 0 ), "
  // + "LINESTRING(50 0 , 0 0 ))");
  // assertTrue(lineString.equals(geometryCollection));
  // }
  public void testEqualsExactForLinearRings() throws Exception {
    final LinearRing x = this.geometryFactory.linearRing(2, 0.0, 0, 100.0, 0, 100.0, 100, 0.0, 0);
    final LinearRing somethingExactlyEqual = this.geometryFactory.linearRing(2, 0.0, 0, 100.0, 0,
      100.0, 100, 0.0, 0);
    final LinearRing somethingNotEqualButSameClass = this.geometryFactory.linearRing(2, 0.0, 0,
      100.0, 0, 100.0, 555, 0.0, 0);
    final LinearRing sameClassButEmpty = this.geometryFactory.linearRing((LineString)null);
    final LinearRing anotherSameClassButEmpty = this.geometryFactory.linearRing((LineString)null);
    final CollectionFactory collectionFactory = new CollectionFactory() {
      @Override
      public Geometry newCollection(final Geometry... geometries) {
        return GeometryImplTest.this.geometryFactory.lineal(geometries);
      }
    };

    doTestEqualsExact(x, somethingExactlyEqual, somethingNotEqualButSameClass, sameClassButEmpty,
      anotherSameClassButEmpty, collectionFactory);

    // LineString somethingEqualButNotExactly =
    // geometryFactory.createLineString(new Point[] {
    // new PointDoubleXY((double)0, 0), new PointDouble((double)100, 0), new
    // Coordinate((double)100, 100),
    // new PointDoubleXY((double)0, 0) });
    //
    // doTestEqualsExact(x, somethingExactlyEqual, somethingEqualButNotExactly,
    // somethingNotEqualButSameClass);
  }

  public void testEqualsExactForLineStrings() throws Exception {
    final LineString x = this.geometryFactory.lineString(2, 0.0, 0, 100.0, 0, 100.0, 100);
    final LineString somethingExactlyEqual = this.geometryFactory.lineString(2, 0.0, 0, 100.0, 0,
      100.0, 100);
    final LineString somethingNotEqualButSameClass = this.geometryFactory.lineString(2, 0.0, 0,
      100.0, 0, 100.0, 555);
    final LineString sameClassButEmpty = this.geometryFactory.lineString();
    final LineString anotherSameClassButEmpty = this.geometryFactory.lineString();
    final CollectionFactory collectionFactory = new CollectionFactory() {
      @Override
      public Geometry newCollection(final Geometry... geometries) {
        return GeometryImplTest.this.geometryFactory.lineal(geometries);
      }
    };

    doTestEqualsExact(x, somethingExactlyEqual, somethingNotEqualButSameClass, sameClassButEmpty,
      anotherSameClassButEmpty, collectionFactory);

    final CollectionFactory collectionFactory2 = new CollectionFactory() {
      @Override
      public Geometry newCollection(final Geometry... geometries) {
        return GeometryImplTest.this.geometryFactory.lineal(geometries);
      }
    };

    doTestEqualsExact(x, somethingExactlyEqual, somethingNotEqualButSameClass, sameClassButEmpty,
      anotherSameClassButEmpty, collectionFactory2);
  }

  public void testEqualsExactForPoints() throws Exception {
    final Point x = this.geometryFactory.point(100.0, 100.0);
    final Point somethingExactlyEqual = this.geometryFactory.point(100.0, 100);
    final Point somethingNotEqualButSameClass = this.geometryFactory.point(999.0, 100);
    final Point sameClassButEmpty = this.geometryFactory.point();
    final Point anotherSameClassButEmpty = this.geometryFactory.point();
    final CollectionFactory collectionFactory = new CollectionFactory() {
      @Override
      public Geometry newCollection(final Geometry... geometries) {
        return GeometryImplTest.this.geometryFactory.punctual(geometries);
      }
    };

    doTestEqualsExact(x, somethingExactlyEqual, somethingNotEqualButSameClass, sameClassButEmpty,
      anotherSameClassButEmpty, collectionFactory);
  }

  public void testEqualsExactForPolygons() throws Exception {
    final Polygon x = (Polygon)this.geometryFactory
      .geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    final Polygon somethingExactlyEqual = (Polygon)this.geometryFactory
      .geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    final Polygon somethingNotEqualButSameClass = (Polygon)this.geometryFactory
      .geometry("POLYGON ((50 50, 50 0, 0 0, 0 50, 50 50))");
    final Polygon sameClassButEmpty = (Polygon)this.geometryFactory.geometry("POLYGON EMPTY");
    final Polygon anotherSameClassButEmpty = (Polygon)this.geometryFactory
      .geometry("POLYGON EMPTY");
    final CollectionFactory collectionFactory = new CollectionFactory() {
      @Override
      public Geometry newCollection(final Geometry... geometries) {
        return GeometryImplTest.this.geometryFactory.polygonal((Object[])geometries);
      }
    };

    doTestEqualsExact(x, somethingExactlyEqual, somethingNotEqualButSameClass, sameClassButEmpty,
      anotherSameClassButEmpty, collectionFactory);
  }

  public void testEqualsWithNull() throws Exception {
    final Geometry polygon = this.geometryFactory
      .geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    assertTrue(!polygon.equals(null));
    final Object g = null;
    assertTrue(!polygon.equals(g));
  }

  // public void testInvalidateEnvelope() throws Exception {
  // final Geometry g =
  // this.reader.read("POLYGON((0 0, 0 50, 50 50, 50 0, 0 0))");
  // assertEquals(new BoundingBox(0, 50, 0, 50),
  // g.getEnvelopeInternal());
  // g.apply(new CoordinateFilter() {
  // @Override
  // public void filter(final Point coord) {
  // coord.setX(coord.getX() + 1);
  // coord.setY(coord.getY() + 1);
  // }
  // });
  // assertEquals(new BoundingBox(0, 50, 0, 50),
  // g.getEnvelopeInternal());
  // g.geometryChanged();
  // assertEquals(new BoundingBox(1, 51, 1, 51),
  // g.getEnvelopeInternal());
  // }

  public void testNoOutgoingDirEdgeFound() throws Exception {
    doTestFromCommcast2003AtYahooDotCa(this.geometryFactory);
  }

  // public void testOutOfMemoryError() throws Exception {
  // doTestFromCommcast2003AtYahooDotCa(new WKTReader());
  // }

  public void testPolygonRelate() throws Exception {
    final Geometry bigPolygon = this.geometryFactory
      .geometry("POLYGON ((0 0, 0 50, 50 50, 50 0, 0 0))");
    final Geometry smallPolygon = this.geometryFactory
      .geometry("POLYGON ((10 10, 10 30, 30 30, 30 10, 10 10))");
    assertTrue(bigPolygon.contains(smallPolygon));
  }
}
