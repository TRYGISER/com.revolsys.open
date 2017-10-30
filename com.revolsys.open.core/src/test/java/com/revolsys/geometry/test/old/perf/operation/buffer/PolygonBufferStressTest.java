package com.revolsys.geometry.test.old.perf.operation.buffer;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.util.GeometricShapeFactory;
import com.revolsys.geometry.util.Stopwatch;

/**
 * Stress-tests buffering by repeatedly buffering a geometry
 * using alternate positive and negative distances.
 *
 * In older versions of JTS this used to quickly cause failure due to robustness
 * issues (bad noding causing topology failures).
 * However by ver 1.13 (at least) this test should pass perfectly.
 * This is due to the many heuristics introduced to improve buffer
 * robustnesss.
 *
 *
 * @author Martin Davis
 *
 */
public class PolygonBufferStressTest {

  private static final GeometryFactory geometryFactory = GeometryFactory.floating(0, 2);

  static final int MAX_ITER = 50;

  public static void main(final String[] args) {
    final PolygonBufferStressTest test = new PolygonBufferStressTest();
    test.test();
  }

  Stopwatch sw = new Stopwatch();

  boolean testFailed = false;

  public PolygonBufferStressTest() {
  }

  public void doAlternatingIteratedBuffer(Geometry g, double dist, final int maxCount) {
    int i = 0;
    while (i < maxCount) {
      i++;
      // System.out.println("Iter: " + i
      // + " --------------------------------------------------------");

      dist += 1.0;
      // System.out.println("Pos Buffer (" + dist + ")");
      g = getBuffer(g, dist);
      // System.out.println("Neg Buffer (" + -dist + ")");
      g = getBuffer(g, -dist);
    }
  }

  public void doIteratedBuffer(Geometry g, final double initDist, final double distanceInc,
    final int maxCount) {
    int i = 0;
    double dist = initDist;
    while (i < maxCount) {
      i++;
      // System.out.println("Iter: " + i
      // + " --------------------------------------------------------");

      dist += distanceInc;
      // System.out.println("Buffer (" + dist + ")");
      g = getBuffer(g, dist);
      // if (((Polygon) g).getNumInteriorRing() > 0)
      // return;
    }
  }

  private Geometry getBuffer(final Geometry geom, final double dist) {
    final Geometry buf = geom.buffer(dist);
    // System.out.println(buf);
    // System.out.println(this.sw.getTimeString());
    if (!buf.isValid()) {
      throw new RuntimeException("buffer not valid!");
    }
    return buf;
  }

  private Geometry getSampleGeometry() {
    String wkt;
    // triangle
    // wkt ="POLYGON (( 233 221, 210 172, 262 181, 233 221 ))";

    // star polygon with hole
    wkt = "POLYGON ((260 400, 220 300, 80 300, 180 220, 40 200, 180 160, 60 20, 200 80, 280 20, 260 140, 440 20, 340 180, 520 160, 280 220, 460 340, 300 300, 260 400), (260 320, 240 260, 220 220, 160 180, 220 160, 200 100, 260 160, 300 140, 320 180, 260 200, 260 320))";

    // star polygon with NO hole
    // wkt
    // ="POLYGON ((260 400, 220 300, 80 300, 180 220, 40 200, 180 160, 60 20,
    // 200 80, 280 20, 260 140, 440 20, 340 180, 520 160, 280 220, 460 340, 300
    // 300, 260 400))";

    // star polygon with NO hole, 10x size
    // wkt
    // ="POLYGON ((2600 4000, 2200 3000, 800 3000, 1800 2200, 400 2000, 1800
    // 1600, 600 200, 2000 800, 2800 200, 2600 1400, 4400 200, 3400 1800, 5200
    // 1600, 2800 2200, 4600 3400, 3000 3000, 2600 4000))";

    Geometry g = null;
    try {
      g = geometryFactory.geometry(wkt);
    } catch (final Exception ex) {
      ex.printStackTrace();
      this.testFailed = true;
    }
    return g;
  }

  public void test() {
    final String geomStr;
    final GeometricShapeFactory shapeFact = new GeometricShapeFactory(geometryFactory);

    final Geometry g = getSampleGeometry();

    // Geometry g = GeometricShapeFactory.createArc(geometryFactory, 0, 0,
    // 200.0, 0.0, 6.0,
    // 100);

    // Geometry circle = GeometricShapeFactory.createCircle(geometryFactory, 0,
    // 0, 200,
    // 100);
    // Geometry g = circle;

    // Geometry sq = GeometricShapeFactory.createBox(geometryFactory, 0, 0, 1,
    // 120);
    // Geometry g = sq.difference(circle);

    // Geometry handle = GeometricShapeFactory.createRectangle(geometryFactory,
    // 0, 0, 400,
    // 20, 1);
    // Geometry g = circle.union(handle);

    // System.out.println(g);
    test(g);
  }

  public void test(final Geometry g) {
    final int maxCount = MAX_ITER;
    // doIteratedBuffer(g, 1, -120.01, maxCount);
    // doIteratedBuffer(g, 1, 2, maxCount);
    doAlternatingIteratedBuffer(g, 1, maxCount);
    if (this.testFailed) {
      // System.out.println("FAILED!");
    }
  }
}
