
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
package com.revolsys.jts.operation.valid;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.jts.algorithm.CGAlgorithms;
import com.revolsys.jts.geom.BoundingBox;
import com.revolsys.jts.geom.Coordinate;
import com.revolsys.jts.geom.Coordinates;
import com.revolsys.jts.geom.LinearRing;
import com.revolsys.jts.geomgraph.GeometryGraph;
import com.revolsys.jts.index.sweepline.SweepLineIndex;
import com.revolsys.jts.index.sweepline.SweepLineInterval;
import com.revolsys.jts.index.sweepline.SweepLineOverlapAction;
import com.revolsys.jts.util.Assert;

/**
 * Tests whether any of a set of {@link LinearRing}s are
 * nested inside another ring in the set, using a {@link SweepLineIndex}
 * index to speed up the comparisons.
 *
 * @version 1.7
 */
public class SweeplineNestedRingTester
{

  private GeometryGraph graph;  // used to find non-node vertices
  private List rings = new ArrayList();
  //private Envelope totalEnv = new Envelope();
  private SweepLineIndex sweepLine;
  private Coordinates nestedPt = null;

  public SweeplineNestedRingTester(GeometryGraph graph)
  {
    this.graph = graph;
  }

  public Coordinates getNestedPoint() { return nestedPt; }

  public void add(LinearRing ring)
  {
    rings.add(ring);
  }

  public boolean isNonNested()
  {
    buildIndex();

    OverlapAction action = new OverlapAction();

    sweepLine.computeOverlaps(action);
    return action.isNonNested;
  }

  private void buildIndex()
  {
    sweepLine = new SweepLineIndex();

    for (int i = 0; i < rings.size(); i++) {
      LinearRing ring = (LinearRing) rings.get(i);
      BoundingBox env = ring.getBoundingBox();
      SweepLineInterval sweepInt = new SweepLineInterval(env.getMinX(), env.getMaxX(), ring);
      sweepLine.add(sweepInt);
    }
  }

  private boolean isInside(LinearRing innerRing, LinearRing searchRing)
  {
    Coordinates[] innerRingPts = innerRing.getCoordinateArray();
    Coordinates[] searchRingPts = searchRing.getCoordinateArray();

    if (! innerRing.getBoundingBox().intersects(searchRing.getBoundingBox()))
      return false;

    Coordinates innerRingPt = IsValidOp.findPtNotNode(innerRingPts, searchRing, graph);
    Assert.isTrue(innerRingPt != null, "Unable to find a ring point not a node of the search ring");

    boolean isInside = CGAlgorithms.isPointInRing(innerRingPt, searchRingPts);
    if (isInside) {
      nestedPt = innerRingPt;
      return true;
    }
    return false;
  }


  class OverlapAction
    implements SweepLineOverlapAction
  {
    boolean isNonNested = true;

  public void overlap(SweepLineInterval s0, SweepLineInterval s1)
  {
    LinearRing innerRing = (LinearRing) s0.getItem();
    LinearRing searchRing = (LinearRing) s1.getItem();
    if (innerRing == searchRing) return;

    if (isInside(innerRing, searchRing))
      isNonNested = false;
  }

  }
}