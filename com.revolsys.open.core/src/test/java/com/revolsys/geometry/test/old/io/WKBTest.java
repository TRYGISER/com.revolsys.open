package com.revolsys.geometry.test.old.io;

import java.io.IOException;

import com.revolsys.geometry.model.CoordinateSequenceComparator;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.util.GeometricShapeFactory;
import com.revolsys.geometry.wkb.ByteOrderValues;
import com.revolsys.geometry.wkb.ParseException;
import com.revolsys.geometry.wkb.WKBReader;
import com.revolsys.geometry.wkb.WKBWriter;

import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 * Tests the {@link WKBReader} and {@link WKBWriter}.
 * Tests all geometries with both 2 and 3 dimensions and both byte orderings.
 */
public class WKBTest extends TestCase {
  static CoordinateSequenceComparator comp2 = new CoordinateSequenceComparator(2);

  static CoordinateSequenceComparator comp3 = new CoordinateSequenceComparator(3);

  public static void main(final String args[]) {
    TestRunner.run(WKBTest.class);
  }

  private final GeometryFactory geomFactory = GeometryFactory.DEFAULT_3D;

  /**
   * Use single WKB reader, to ensure it can be used for multiple input geometries
   */
  WKBReader wkbReader = new WKBReader(this.geomFactory);

  public WKBTest(final String name) {
    super(name);
  }

  void runGeometry(final Geometry g, final int dimension, final int byteOrder, final boolean toHex,
    final int srid) throws IOException, ParseException {
    boolean includeSRID = false;
    if (srid >= 0) {
      includeSRID = true;
      // g.setSRID(srid);
    }

    final WKBWriter wkbWriter = new WKBWriter(dimension, byteOrder, includeSRID);
    byte[] wkb = wkbWriter.write(g);
    String wkbHex = null;
    if (toHex) {
      wkbHex = WKBWriter.toHex(wkb);
    }

    if (toHex) {
      wkb = WKBReader.hexToBytes(wkbHex);
    }
    final Geometry g2 = this.wkbReader.read(wkb);

    final boolean isEqual = g.equals(2, g2);
    assertTrue(isEqual);

    if (includeSRID) {
      final boolean isSRIDEqual = g.getCoordinateSystemId() == g2.getCoordinateSystemId();
      assertTrue(isSRIDEqual);
    }
  }

  private void runWKBTest(final Geometry g, final int dimension, final boolean toHex)
    throws IOException, ParseException {
    runWKBTest(g, dimension, ByteOrderValues.LITTLE_ENDIAN, toHex);
    runWKBTest(g, dimension, ByteOrderValues.BIG_ENDIAN, toHex);
  }

  private void runWKBTest(final Geometry g, final int dimension, final int byteOrder,
    final boolean toHex) throws IOException, ParseException {
    runGeometry(g, dimension, byteOrder, toHex, 100);
    runGeometry(g, dimension, byteOrder, toHex, 0);
    runGeometry(g, dimension, byteOrder, toHex, 101010);
    runGeometry(g, dimension, byteOrder, toHex, -1);
  }

  private void runWKBTest(final String wkt) throws IOException, ParseException {
    runWKBTestCoordinateArray(wkt);
    runWKBTestPackedCoordinate(wkt);
  }

  private void runWKBTestCoordinateArray(final String wkt) throws IOException, ParseException {
    final GeometryFactory geometryFactory = GeometryFactory.DEFAULT_3D;
    final Geometry g = geometryFactory.geometry(wkt);

    // CoordinateArrays support dimension 3, so test both dimensions
    runWKBTest(g, 2, true);
    runWKBTest(g, 2, false);
    runWKBTest(g, 3, true);
    runWKBTest(g, 3, false);
  }

  private void runWKBTestPackedCoordinate(final String wkt) throws IOException, ParseException {
    final GeometryFactory geometryFactory = GeometryFactory.floating(0, 2);
    final Geometry g = geometryFactory.geometry(wkt);

    // Since we are using a PCS of dim=2, only check 2-dimensional storage
    runWKBTest(g, 2, true);
    runWKBTest(g, 2, false);
  }

  public void testBigPolygon() throws IOException, ParseException {
    final GeometricShapeFactory shapeFactory = new GeometricShapeFactory(this.geomFactory);
    shapeFactory.setBase(new PointDoubleXY(0, 0));
    shapeFactory.setSize(1000);
    shapeFactory.setNumPoints(1000);
    final Geometry geom = shapeFactory.newRectangle();
    runWKBTest(geom, 2, false);
  }

  public void testFirst() throws IOException, ParseException {
    runWKBTest("MULTIPOINT((0 0), (1 4), (100 200))");
  }

  public void testGeometryCollection() throws IOException, ParseException {
    runWKBTest(
      "GEOMETRYCOLLECTION(POINT(1 1),LINESTRING(0 0,10 10),POLYGON((0 0,100 0,100 100,0 100,0 0)))");
  }

  public void testGeometryCollectionEmpty() throws IOException, ParseException {
    runWKBTest("GEOMETRYCOLLECTION EMPTY");
  }

  public void testLineString() throws IOException, ParseException {
    runWKBTest("LINESTRING (1 2, 10 20, 100 200)");
  }

  public void testLineStringEmpty() throws IOException, ParseException {
    runWKBTest("LINESTRING EMPTY");
  }

  public void testMultiLineString() throws IOException, ParseException {
    runWKBTest("MULTILINESTRING ((0 0, 1 10), (10 10, 20 30), (123 123, 456 789))");
  }

  public void testMultiLineStringEmpty() throws IOException, ParseException {
    runWKBTest("MULTILINESTRING EMPTY");
  }

  public void testMultiPoint() throws IOException, ParseException {
    runWKBTest("MULTIPOINT ((0 0), (1 4), (100 200))");
  }

  public void testMultiPointEmpty() throws IOException, ParseException {
    runWKBTest("MULTIPOINT EMPTY");
  }

  public void testMultiPolygon() throws IOException, ParseException {
    runWKBTest(
      "MULTIPOLYGON (((0 0,100 0,100 100,0 100,0 0),(1 1,1 10,10 10,10 1,1 1)),((200 200,200 250,250 250,250 200,200 200)))");
  }

  public void testMultiPolygonEmpty() throws IOException, ParseException {
    runWKBTest("MULTIPOLYGON EMPTY");
  }

  public void testNestedGeometryCollection() throws IOException, ParseException {
    runWKBTest(
      "GEOMETRYCOLLECTION(POINT(20 20),GEOMETRYCOLLECTION(POINT(1 1),LINESTRING(0 0,10 10),POLYGON((0 0,100 0,100 100,0 100,0 0))))");
  }

  public void testPoint() throws IOException, ParseException {
    runWKBTest("POINT (1 2)");
  }

  // static Comparator comp2D = new Coordinate.DimensionalComparator();
  // static Comparator comp3D = new Coordinate.DimensionalComparator(3);

  public void testPointPCS() throws IOException, ParseException {
    runWKBTestPackedCoordinate("POINT (1 2)");
  }

  public void testPolygon() throws IOException, ParseException {
    runWKBTest("POLYGON ((0 0, 100 0, 100 100, 0 100, 0 0))");
  }

  public void testPolygonEmpty() throws IOException, ParseException {
    runWKBTest("LINESTRING EMPTY");
  }

  public void testPolygonWithHole() throws IOException, ParseException {
    runWKBTest("POLYGON((0 0,100 0,100 100,0 100,0 0),(1 1,1 10,10 10,10 1,1 1))");
  }
}
