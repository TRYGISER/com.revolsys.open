package com.revolsys.geometry.test.model;

import org.junit.Assert;
import org.junit.Test;

import com.revolsys.geometry.cs.GeographicCoordinateSystem;
import com.revolsys.geometry.cs.ProjectedCoordinateSystem;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.impl.PointDouble;
import com.revolsys.geometry.model.impl.SortedPointList;
import com.revolsys.geometry.test.TestConstants;

public class PointTest implements TestConstants {

  public static void assertCoordinatesEquals(final Point point, final double... coordinates) {
    Assert.assertEquals("Is Empty", false, point.isEmpty());
    Assert.assertEquals("Geometry Count", 1, point.getGeometryCount());
    Assert.assertNotNull("Not Null First Vertex", point.getVertex(0));
    Assert.assertEquals("Axis Count", coordinates.length, point.getAxisCount());
    Assert.assertEquals("Vertex Count", coordinates.length, point.getAxisCount());
    for (int axisIndex = -1; axisIndex < point.getAxisCount() + 1; axisIndex++) {
      final double value = point.getCoordinate(axisIndex);
      if (axisIndex < 0 || axisIndex >= coordinates.length) {
        if (!Double.isNaN(value)) {
          TestUtil.failNotEquals("Value NaN", Double.NaN, value);
        }
      } else {
        Assert.assertEquals("Coordinate Value", coordinates[axisIndex], value, 0);
      }
    }
  }

  public static void assertEmpty(final Point point) {
    Assert.assertEquals("Is Empty", true, point.isEmpty());
    Assert.assertEquals("Geometry Count", 0, point.getGeometryCount());
    Assert.assertNull("Null First Vertex", point.getVertex(0));
    Assert.assertNull("Null First Segment", point.getSegment(0));

    for (int axisIndex = -1; axisIndex < point.getAxisCount() + 1; axisIndex++) {
      final double value = point.getCoordinate(axisIndex);
      if (!Double.isNaN(value)) {
        TestUtil.failNotEquals("Value NaN", Double.NaN, value);
      }
    }
  }

  public static void assertEquals(final Point point, final double... coordinates) {
    final GeometryFactory geometryFactory = point.getGeometryFactory();

    final GeometryFactory geometryFactory2;
    final int axisCount = geometryFactory.getAxisCount();
    if (geometryFactory.getCoordinateSystem() instanceof ProjectedCoordinateSystem) {
      final ProjectedCoordinateSystem projectedCs = (ProjectedCoordinateSystem)geometryFactory
        .getCoordinateSystem();
      final GeographicCoordinateSystem geographicCoordinateSystem = projectedCs
        .getGeographicCoordinateSystem();
      geometryFactory2 = GeometryFactory
        .floating(geographicCoordinateSystem.getCoordinateSystemId(), axisCount);
    } else {
      geometryFactory2 = GeometryFactory.floating(26910, axisCount);
    }

    assertCoordinatesEquals(point, coordinates);

    final Point clone = point.newPoint();
    assertCoordinatesEquals(clone, coordinates);

    final Point converted = point.convertGeometry(geometryFactory);
    assertCoordinatesEquals(converted, coordinates);
    Assert.assertSame(point, converted);

    final Point convertedOther = point.convertGeometry(geometryFactory2);
    final Point convertedBack = convertedOther.convertGeometry(geometryFactory);
    assertCoordinatesEquals(convertedBack, coordinates);
    Assert.assertNotSame(point, convertedBack);

    final Point copy = point.newGeometry(geometryFactory);
    assertCoordinatesEquals(copy, coordinates);
    Assert.assertNotSame(point, copy);

    final Point copyOther = point.convertGeometry(geometryFactory2);
    final Point copyBack = copyOther.convertGeometry(geometryFactory);
    assertCoordinatesEquals(copyBack, coordinates);
    Assert.assertNotSame(point, copyBack);

    final String string = point.toString();
    final Point pointString = geometryFactory.geometry(string);
    assertCoordinatesEquals(pointString, coordinates);

    final String wkt = point.toEwkt();
    final Point pointWkt = geometryFactory.geometry(wkt);
    assertCoordinatesEquals(pointWkt, coordinates);

  }

  public static void assertVertices(final Point point, final int axisCount,
    final double... coordinates) {
    int vertexCount;
    if (point.isEmpty()) {
      vertexCount = 0;
    } else {
      vertexCount = coordinates.length / axisCount;
    }
    Assert.assertEquals("Vertex Count", vertexCount, point.getVertexCount());
  }

  private void assertEquals(final double[] coordinates, final double[] coordinatesLessNaN,
    final Point pointCoordinatesListAllAxis, final Point pointCoordinatesListExtraAxis,
    final Point pointCoordinatesListLessAxis) {
    assertEquals(pointCoordinatesListAllAxis, coordinates);
    assertEquals(pointCoordinatesListExtraAxis, coordinates);
    assertEquals(pointCoordinatesListLessAxis, coordinatesLessNaN);
  }

  private void assertObjectContsructor(final GeometryFactory geometryFactory,
    final double[] coordinates, final double[] coordinatesLessNaN, final Point pointAll,
    final Point pointExtra, final Point pointLess) {
    final Point pointAllAxis = geometryFactory.point((Object)pointAll);
    final Point pointExtraAxis = geometryFactory.point((Object)pointExtra);
    final Point pointLessAxis = geometryFactory.point((Object)pointLess);

    assertEquals(coordinates, coordinatesLessNaN, pointAllAxis, pointExtraAxis, pointLessAxis);
  }

  @Test
  public void constructEmpty() {
    for (int axisCount = 2; axisCount < 4; axisCount++) {
      final GeometryFactory geometryFactory = GeometryFactory.fixed(26910, axisCount, 1000.0,
        1000.0);

      final Point pointEmpty = geometryFactory.point();
      assertEmpty(pointEmpty);

      final Point pointCoordinatesNull = geometryFactory.point((Point)null);
      assertEmpty(pointCoordinatesNull);

      final Point pointCoordinatesListNull = geometryFactory.point();
      assertEmpty(pointCoordinatesListNull);

      final Point pointObjectNull = geometryFactory.point((Object)null);
      assertEmpty(pointObjectNull);

      final Point pointDoubleArrayNull = geometryFactory.point((double[])null);
      assertEmpty(pointDoubleArrayNull);

      final Point pointDoubleArraySize0 = geometryFactory.point(new double[0]);
      assertEmpty(pointDoubleArraySize0);

      final Point pointDoubleArraySize1 = geometryFactory.point(new double[1]);
      assertEmpty(pointDoubleArraySize1);
    }
  }

  @Test
  public void constructPoint() {
    for (int axisCount = 2; axisCount < 4; axisCount++) {
      int axisCountLess = axisCount;
      if (axisCountLess > 2) {
        axisCountLess--;
      }
      final GeometryFactory geometryFactory = GeometryFactory.fixed(26910, axisCount, 1000.0,
        1000.0);
      final GeometryFactory geometryFactoryExtra = GeometryFactory.floating(26910, axisCount + 1);
      final GeometryFactory geometryFactoryLess = GeometryFactory.floating(26910, axisCountLess);
      final double[] coordinatesExtra = new double[axisCount + 1];
      final double[] coordinates = new double[axisCount];
      final double[] coordinatesLess = new double[axisCountLess];
      final double[] coordinatesLessNaN = new double[axisCount];
      for (int i = 0; i < axisCount; i++) {
        double value;
        switch (i) {
          case 0:
            value = UTM10_X_START;
          break;
          case 1:
            value = UTM10_Y_START;
          break;
          default:
            value = i * 10 + i;
        }
        coordinates[i] = value;
        coordinatesExtra[i] = value;
        coordinatesLessNaN[i] = value;
        if (i < axisCountLess) {
          coordinatesLess[i] = value;
        } else {
          coordinatesLessNaN[i] = Double.NaN;
        }
      }
      coordinatesExtra[coordinatesExtra.length - 1] = 6;

      // double[]
      final Point pointDoubleAllAxis = geometryFactory.point(coordinates);
      final Point pointDoubleExtraAxis = geometryFactory.point(coordinatesExtra);
      final Point pointDoubleLessAxis = geometryFactory.point(coordinatesLess);
      assertEquals(coordinates, coordinatesLessNaN, pointDoubleAllAxis, pointDoubleExtraAxis,
        pointDoubleLessAxis);
      assertObjectContsructor(geometryFactory, coordinates, coordinatesLessNaN, pointDoubleAllAxis,
        pointDoubleExtraAxis, pointDoubleLessAxis);

      // Coordinates
      final Point pointCoordinatesAllAxis = geometryFactory.point(new PointDouble(coordinates));
      final Point pointCoordinatesExtraAxis = geometryFactory
        .point(new PointDouble(coordinatesExtra));
      final Point pointCoordinatesLessAxis = geometryFactory
        .point(new PointDouble(coordinatesLess));
      assertEquals(coordinates, coordinatesLessNaN, pointCoordinatesAllAxis,
        pointCoordinatesExtraAxis, pointCoordinatesLessAxis);
      assertObjectContsructor(geometryFactory, coordinates, coordinatesLessNaN,
        pointCoordinatesAllAxis, pointCoordinatesExtraAxis, pointCoordinatesLessAxis);

      // Object Point
      final Point pointAll = pointDoubleAllAxis;
      final Point pointExtra = geometryFactoryExtra.point(coordinatesExtra);
      final Point pointLess = geometryFactoryLess.point(coordinatesLess);
      assertObjectContsructor(geometryFactory, coordinates, coordinatesLessNaN, pointAll,
        pointExtra, pointLess);
    }
  }

  @Test
  public void testFromFile() {
    TestUtil.doTestGeometry(getClass(), "Point.csv");
  }

  @Test
  public void testSortedPointList() {
    final SortedPointList points1 = new SortedPointList(UTM10_GF_2_FLOATING, 3);
    points1.addPoint(10, 1, 2);
    points1.addPoint(5, 2, 3);
    points1.addPoint(5, 3, 3);
    points1.addPoint(7, 1, 2);
    points1.addPoint(11, 1, 2);
    points1.addPoint(1, 1, 2);
    points1.addPoint(5, 2, 3);
    final double[] coordinates1 = points1.getCoordinates();
    Assert.assertArrayEquals("coordinates",
      new double[] { //
        1, 1, 2, //
        7, 1, 2, //
        10, 1, 2, //
        11, 1, 2, //
        5, 2, 3, //
        5, 3, 3, //
      }, coordinates1, 0);
  }

  @Test
  public void testVertices() {

  }
}
