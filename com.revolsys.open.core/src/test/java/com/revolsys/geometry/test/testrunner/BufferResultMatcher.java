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
package com.revolsys.geometry.test.testrunner;

import com.revolsys.geometry.algorithm.distance.DiscreteHausdorffDistance;
import com.revolsys.geometry.model.Geometry;

import junit.framework.Assert;

/**
 * A {@link ResultMatcher} which compares the results of
 * buffer operations for equality, up to the given tolerance.
 * All other operations are delagated to the
 * standard {@link EqualityResultMatcher} algorithm.
 *
 * @author mbdavis
 *
 */
@SuppressWarnings("deprecation")
public class BufferResultMatcher implements ResultMatcher {
  private static final double MAX_HAUSDORFF_DISTANCE_FACTOR = 100;

  private static final double MAX_RELATIVE_AREA_DIFFERENCE = 1.0E-3;

  /**
   * The minimum distance tolerance which will be used.
   * This is required because densified vertices do no lie precisely on their parent segment.
   */
  private static final double MIN_DISTANCE_TOLERANCE = 1.0e-8;

  private final ResultMatcher defaultMatcher = new EqualityResultMatcher();

  public boolean isBoundaryHausdorffDistanceInTolerance(final Geometry actualBuffer,
    final Geometry expectedBuffer, final double distance) {
    final Geometry actualBdy = actualBuffer.getBoundary();
    final Geometry expectedBdy = expectedBuffer.getBoundary();

    final DiscreteHausdorffDistance haus = new DiscreteHausdorffDistance(actualBdy, expectedBdy);
    haus.setDensifyFraction(0.25);
    final double maxDistanceFound = haus.orientedDistance();
    double expectedDistanceTol = Math.abs(distance) / MAX_HAUSDORFF_DISTANCE_FACTOR;
    if (expectedDistanceTol < MIN_DISTANCE_TOLERANCE) {
      expectedDistanceTol = MIN_DISTANCE_TOLERANCE;
    }
    if (maxDistanceFound > expectedDistanceTol) {
      return false;
    }
    return true;
  }

  public boolean isBufferResultMatch(final Geometry actualBuffer, final Geometry expectedBuffer,
    final double distance) {
    if (actualBuffer.isEmpty() && expectedBuffer.isEmpty()) {
      return true;
    }

    /**
     * MD - need some more checks here - symDiffArea won't catch very small holes ("tears")
     * near the edge of computed buffers (which can happen in current version of JTS (1.8)).
     * This can probably be handled by testing
     * that every point of the actual buffer is at least a certain distance away from the
     * geometry boundary.
     */
    if (!isSymDiffAreaInTolerance(actualBuffer, expectedBuffer)) {
      return false;
    }

    if (!isBoundaryHausdorffDistanceInTolerance(actualBuffer, expectedBuffer, distance)) {
      return false;
    }

    return true;
  }

  /**
   * Tests whether the two results are equal within the given
   * tolerance.  The input parameters are not considered.
   *
   * @return true if the actual and expected results are considered equal
   */
  @Override
  public boolean isMatch(final Geometry geom, final String opName, final Object[] args,
    final Result actualResult, final Result expectedResult, final double tolerance) {
    if (!opName.equalsIgnoreCase("buffer")) {
      return this.defaultMatcher.isMatch(geom, opName, args, actualResult, expectedResult,
        tolerance);
    }

    final double distance = Double.parseDouble((String)args[0]);
    final boolean match = isBufferResultMatch(((GeometryResult)actualResult).getGeometry(),
      ((GeometryResult)expectedResult).getGeometry(), distance);
    if (!match) {
      Assert.failNotEquals(opName, expectedResult.getResult(), actualResult.getResult());
    }
    return match;
  }

  public boolean isSymDiffAreaInTolerance(final Geometry actualBuffer,
    final Geometry expectedBuffer) {
    final double area = expectedBuffer.getArea();
    final Geometry diff = actualBuffer.symDifference(expectedBuffer);
    // System.out.println(diff);
    final double areaDiff = diff.getArea();

    // can't get closer than difference area = 0 ! This also handles case when
    // symDiff is empty
    if (areaDiff <= 0.0) {
      return true;
    }

    double frac = Double.POSITIVE_INFINITY;
    if (area > 0.0) {
      frac = areaDiff / area;
    }

    return frac < MAX_RELATIVE_AREA_DIFFERENCE;
  }

}
