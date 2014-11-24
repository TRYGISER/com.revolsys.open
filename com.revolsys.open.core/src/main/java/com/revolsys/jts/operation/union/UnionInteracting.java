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

package com.revolsys.jts.operation.union;

import java.util.ArrayList;
import java.util.List;

import com.revolsys.jts.geom.Geometry;
import com.revolsys.jts.geom.GeometryFactory;
import com.revolsys.jts.geom.util.GeometryCombiner;

/**
 * Experimental code to union MultiPolygons
 * with processing limited to the elements which actually interact.
 * 
 * Not currently used, since it doesn't seem to offer much of a performance advantage.
 * 
 * @author mbdavis
 *
 */
public class UnionInteracting {
  public static Geometry union(final Geometry g0, final Geometry g1) {
    final UnionInteracting uue = new UnionInteracting(g0, g1);
    return uue.union();
  }

  private final GeometryFactory geomFactory;

  private final Geometry g0;

  private final Geometry g1;

  private final boolean[] interacts0;

  private final boolean[] interacts1;

  public UnionInteracting(final Geometry g0, final Geometry g1) {
    this.g0 = g0;
    this.g1 = g1;
    geomFactory = g0.getGeometryFactory();
    interacts0 = new boolean[g0.getGeometryCount()];
    interacts1 = new boolean[g1.getGeometryCount()];
  }

  private Geometry bufferUnion(final Geometry g0, final Geometry g1) {
    final GeometryFactory factory = g0.getGeometryFactory();
    final Geometry gColl = factory.geometryCollection(g0, g1);
    final Geometry unionAll = gColl.buffer(0.0);
    return unionAll;
  }

  private void computeInteracting() {
    for (int i = 0; i < g0.getGeometryCount(); i++) {
      final Geometry elem = g0.getGeometry(i);
      interacts0[i] = computeInteracting(elem);
    }
  }

  private boolean computeInteracting(final Geometry elem0) {
    boolean interactsWithAny = false;
    for (int i = 0; i < g1.getGeometryCount(); i++) {
      final Geometry elem1 = g1.getGeometry(i);
      final boolean interacts = elem1.getBoundingBox().intersects(
        elem0.getBoundingBox());
      if (interacts) {
        interacts1[i] = true;
      }
      if (interacts) {
        interactsWithAny = true;
      }
    }
    return interactsWithAny;
  }

  private Geometry extractElements(final Geometry geom,
    final boolean[] interacts, final boolean isInteracting) {
    final List extractedGeoms = new ArrayList();
    for (int i = 0; i < geom.getGeometryCount(); i++) {
      final Geometry elem = geom.getGeometry(i);
      if (interacts[i] == isInteracting) {
        extractedGeoms.add(elem);
      }
    }
    return geomFactory.buildGeometry(extractedGeoms);
  }

  public Geometry union() {
    computeInteracting();

    // check for all interacting or none interacting!

    final Geometry int0 = extractElements(g0, interacts0, true);
    final Geometry int1 = extractElements(g1, interacts1, true);

    // System.out.println(int0);
    // System.out.println(int1);

    if (int0.isEmpty() || int1.isEmpty()) {
      System.out.println("found empty!");
      // computeInteracting();
    }
    // if (! int0.isValid()) {
    // System.out.println(int0);
    // throw new RuntimeException("invalid geom!");
    // }

    final Geometry union = int0.union(int1);
    // Geometry union = bufferUnion(int0, int1);

    final Geometry disjoint0 = extractElements(g0, interacts0, false);
    final Geometry disjoint1 = extractElements(g1, interacts1, false);

    final Geometry overallUnion = GeometryCombiner.combine(union, disjoint0,
      disjoint1);

    return overallUnion;

  }

}