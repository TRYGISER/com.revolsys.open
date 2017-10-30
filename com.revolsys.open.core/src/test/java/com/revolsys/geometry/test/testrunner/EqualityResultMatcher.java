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

import com.revolsys.geometry.model.Geometry;

import junit.framework.Assert;

/**
 * A {@link ResultMatcher} which compares result for equality,
 * up to the given tolerance.
 *
 * @author mbdavis
 *
 */
@SuppressWarnings("deprecation")
public class EqualityResultMatcher implements ResultMatcher {
  /**
   * Tests whether the two results are equal within the given
   * tolerance.  The input parameters are not considered.
   *
   * @return true if the actual and expected results are considered equal
   */
  @Override
  public boolean isMatch(final Geometry geom, final String opName, final Object[] args,
    final Result actualResult, final Result expectedResult, final double tolerance) {
    final boolean equals = actualResult.equals(expectedResult, tolerance);
    if (!equals) {
      Assert.failNotEquals(opName, expectedResult.getResult(), actualResult.getResult());
    }
    return equals;
  }

}
