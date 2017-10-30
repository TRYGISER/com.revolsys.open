package com.revolsys.geometry.test.old.math;

import com.revolsys.geometry.math.Vector2D;

import junit.framework.TestCase;
import junit.textui.TestRunner;

public class Vector2DTest extends TestCase {

  private static final double TOLERANCE = 1E-5;

  public static void main(final String args[]) {
    TestRunner.run(Vector2DTest.class);
  }

  public Vector2DTest(final String name) {
    super(name);
  }

  void assertEquals(final Vector2D v1, final Vector2D v2) {
    assertTrue(v1.equals(v2));
  }

  void assertEquals(final Vector2D v1, final Vector2D v2, final double tolerance) {
    assertEquals(v1.getX(), v2.getX(), tolerance);
    assertEquals(v1.getY(), v2.getY(), tolerance);
  }

  public void testIsParallel() throws Exception {
    assertTrue(Vector2D.newVector(0, 1).isParallel(Vector2D.newVector(0, 2)));
    assertTrue(Vector2D.newVector(1, 1).isParallel(Vector2D.newVector(2, 2)));
    assertTrue(Vector2D.newVector(-1, -1).isParallel(Vector2D.newVector(2, 2)));

    assertTrue(!Vector2D.newVector(1, -1).isParallel(Vector2D.newVector(2, 2)));
  }

  public void testLength() {
    assertEquals(Vector2D.newVector(0, 1).length(), 1.0, TOLERANCE);
    assertEquals(Vector2D.newVector(0, -1).length(), 1.0, TOLERANCE);
    assertEquals(Vector2D.newVector(1, 1).length(), Math.sqrt(2.0), TOLERANCE);
    assertEquals(Vector2D.newVector(3, 4).length(), 5, TOLERANCE);
  }

  public void testToCoordinate() {
    assertEquals(Vector2D.newVector(Vector2D.newVector(1, 2).toCoordinate()),
      Vector2D.newVector(1, 2), TOLERANCE);
  }
}
