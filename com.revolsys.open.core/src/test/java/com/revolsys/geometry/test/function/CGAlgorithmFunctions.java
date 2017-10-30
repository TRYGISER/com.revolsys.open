package com.revolsys.geometry.test.function;

import com.revolsys.geometry.algorithm.CGAlgorithmsDD;
import com.revolsys.geometry.algorithm.RobustLineIntersector;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.coordinates.list.CoordinatesListUtil;

public class CGAlgorithmFunctions {
  public static int orientationIndex(final Geometry segment, final Geometry ptGeom) {
    if (segment.getVertexCount() != 2 || ptGeom.getVertexCount() != 1) {
      throw new IllegalArgumentException("A must have two points and B must have one");
    }
    final Point[] segPt = CoordinatesListUtil.getPointArray(segment);

    final Point p = ptGeom.getPoint();
    final int index = CGAlgorithmsDD.orientationIndex(segPt[0], segPt[1], p);
    return index;
  }

  public static int orientationIndexDD(final Geometry segment, final Geometry ptGeom) {
    if (segment.getVertexCount() != 2 || ptGeom.getVertexCount() != 1) {
      throw new IllegalArgumentException("A must have two points and B must have one");
    }
    final Point[] segPt = CoordinatesListUtil.getPointArray(segment);

    final Point p = ptGeom.getPoint();
    final int index = CGAlgorithmsDD.orientationIndex(segPt[0], segPt[1], p);
    return index;
  }

  public static Geometry segmentIntersection(final Geometry g1, final Geometry g2) {
    final Point[] pt1 = CoordinatesListUtil.getPointArray(g1);
    final Point[] pt2 = CoordinatesListUtil.getPointArray(g2);
    final RobustLineIntersector ri = new RobustLineIntersector();
    ri.computeIntersectionPoints(pt1[0], pt1[1], pt2[0], pt2[1]);
    switch (ri.getIntersectionCount()) {
      case 0:
        // no intersection => return empty point
        return g1.getGeometryFactory().point();
      case 1:
        // return point
        return g1.getGeometryFactory().point(ri.getIntersection(0));
      case 2:
        // return line
        return g1.getGeometryFactory().lineString(new Point[] {
          ri.getIntersection(0), ri.getIntersection(1)
        });
    }
    return null;
  }

  public static Geometry segmentIntersectionDD(final Geometry g1, final Geometry g2) {
    final Point[] pt1 = CoordinatesListUtil.getPointArray(g1);
    final Point[] pt2 = CoordinatesListUtil.getPointArray(g2);

    // first check if there actually is an intersection
    final RobustLineIntersector ri = new RobustLineIntersector();
    ri.computeIntersectionPoints(pt1[0], pt1[1], pt2[0], pt2[1]);
    if (!ri.hasIntersection()) {
      // no intersection => return empty point
      return g1.getGeometryFactory().point();
    }

    final Point intPt = CGAlgorithmsDD.intersection(pt1[0], pt1[1], pt2[0], pt2[1]);
    return g1.getGeometryFactory().point(intPt);
  }

  public static boolean segmentIntersects(final Geometry g1, final Geometry g2) {
    final Point[] pt1 = CoordinatesListUtil.getPointArray(g1);
    final Point[] pt2 = CoordinatesListUtil.getPointArray(g2);
    final RobustLineIntersector ri = new RobustLineIntersector();
    ri.computeIntersectionPoints(pt1[0], pt1[1], pt2[0], pt2[1]);
    return ri.hasIntersection();
  }

}
