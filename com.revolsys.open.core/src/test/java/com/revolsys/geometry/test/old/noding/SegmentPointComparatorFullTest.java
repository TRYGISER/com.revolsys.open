package com.revolsys.geometry.test.old.noding;

import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.model.segment.LineSegment;
import com.revolsys.geometry.model.segment.LineSegmentDouble;
import com.revolsys.geometry.noding.Octant;
import com.revolsys.geometry.noding.SegmentPointComparator;
import com.revolsys.util.number.Doubles;

import junit.framework.TestCase;

/**
 * Test IntersectionSegment#compareNodePosition using an exhaustive set
 * of test cases
 *
 * @version 1.7
 */
public class SegmentPointComparatorFullTest extends TestCase {

  public static void main(final String[] args) {
    junit.textui.TestRunner.run(SegmentPointComparatorFullTest.class);
  }

  public SegmentPointComparatorFullTest(final String name) {
    super(name);
  }

  private void checkNodePosition(final LineSegment seg, final Point p0, final Point p1,
    final int expectedPositionValue) {
    final int octant = Octant.octant(seg.getP0(), seg.getP1());
    final int posValue = SegmentPointComparator.compare(octant, p0, p1);
    // System.out.println(octant + " " + p0 + " " + p1 + " " + posValue);
    assertTrue(posValue == expectedPositionValue);
  }

  private void checkPointsAtDistance(final LineSegment seg, final double dist0,
    final double dist1) {
    final Point p0 = computePoint(seg, dist0);
    final Point p1 = computePoint(seg, dist1);
    if (p0.equals(p1)) {
      checkNodePosition(seg, p0, p1, 0);
    } else {
      checkNodePosition(seg, p0, p1, -1);
      checkNodePosition(seg, p1, p0, 1);
    }
  }

  private void checkSegment(final double x, final double y) {
    final Point seg0 = new PointDoubleXY(0, 0);
    final Point seg1 = new PointDoubleXY(x, y);
    final LineSegment seg = new LineSegmentDouble(seg0, seg1);

    for (int i = 0; i < 4; i++) {
      final double dist = i;

      final double gridSize = 1;

      checkPointsAtDistance(seg, dist, dist + 1.0 * gridSize);
      checkPointsAtDistance(seg, dist, dist + 2.0 * gridSize);
      checkPointsAtDistance(seg, dist, dist + 3.0 * gridSize);
      checkPointsAtDistance(seg, dist, dist + 4.0 * gridSize);
    }
  }

  private Point computePoint(final LineSegment seg, final double dist) {
    final double dx = seg.getP1().getX() - seg.getP0().getX();
    final double dy = seg.getP1().getY() - seg.getP0().getY();
    final double len = seg.getLength();
    final Point pt = new PointDoubleXY(Doubles.makePrecise(1.0, dist * dx / len),
      Doubles.makePrecise(1.0, dist * dy / len));
    return pt;
  }

  public void testQuadrant0() {
    checkSegment(100, 0);
    checkSegment(100, 50);
    checkSegment(100, 100);
    checkSegment(100, 150);
    checkSegment(0, 100);
  }

  public void testQuadrant1() {
    checkSegment(-100, 0);
    checkSegment(-100, 50);
    checkSegment(-100, 100);
    checkSegment(-100, 150);
  }

  public void testQuadrant2() {
    checkSegment(-100, 0);
    checkSegment(-100, -50);
    checkSegment(-100, -100);
    checkSegment(-100, -150);
  }

  public void testQuadrant4() {
    checkSegment(100, -50);
    checkSegment(100, -100);
    checkSegment(100, -150);
    checkSegment(0, -100);
  }

}
