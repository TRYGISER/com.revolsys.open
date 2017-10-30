package com.revolsys.geometry.test.old.perf.triangulate;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.elevation.tin.quadedge.QuadEdgeDelaunayTinBuilder;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.util.Stopwatch;

public class VoronoiPerfTest {
  final static GeometryFactory geometryFactory = GeometryFactory.DEFAULT_3D;

  final static double SIDE_LEN = 10.0;

  public static void main(final String args[]) {
    final VoronoiPerfTest test = new VoronoiPerfTest();
    test.run();
  }

  List randomPoints(final int nPts) {
    final List pts = new ArrayList();

    final int nSide = (int)Math.sqrt(nPts) + 1;

    for (int i = 0; i < nSide; i++) {
      for (int j = 0; j < nSide; j++) {
        final double x = i * SIDE_LEN + SIDE_LEN * Math.random();
        final double y = j * SIDE_LEN + SIDE_LEN * Math.random();
        pts.add(new PointDoubleXY(x, y));
      }
    }
    return pts;
  }

  public void run() {
    run(10);
    run(100);
    run(1000);
    run(10000);
    run(100000);
    run(1000000);
  }

  public void run(final int nPts) {
    final List pts = randomPoints(nPts);
    final Stopwatch sw = new Stopwatch();
    final QuadEdgeDelaunayTinBuilder builder = new QuadEdgeDelaunayTinBuilder(geometryFactory);
    builder.insertVertices(pts);

    final Geometry g = builder.getEdges();
    // System.out.println("# pts: " + pts.size() + " -- " +
    // sw.getTimeString());
    // System.out.println(g);
  }
}
