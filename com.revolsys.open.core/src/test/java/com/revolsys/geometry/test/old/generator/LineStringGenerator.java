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
package com.revolsys.geometry.test.old.generator;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.operation.valid.IsValidOp;

/**
 *
 * This class is used to Construct a new line string within the specified bounding box.
 *
 * Sucessive calls to create may or may not return the same geometry topology.
 *
 * @author David Zwiers, Vivid Solutions.
 */
public class LineStringGenerator extends GeometryGenerator {
  /**
   * Create the points in an approximation of an open circle (one edge will not be included).
   *
   * Note: this requires the number of points to be greater than 2.
   *
   * @see #getNumberPoints()
   * @see #setNumberPoints(int)
   */
  public static final int ARC = 0;

  /**
   * Create the points in a horizontal line
   */
  public static final int HORZ = 2;

  /**
   * Number of interations attempting to Construct a new valid line string
   */
  private static final int RUNS = 5;

  /**
   * Create the points in a vertical line
   */
  public static final int VERT = 1;

  private static void fillArc(final double x, final double dx, final double y, final double dy,
    final Point[] coords, final GeometryFactory gf) {
    if (coords.length == 2) {
      throw new IllegalStateException("Too few points for Arc");
    }

    final double theta = 360 / coords.length;
    final double start = theta / 2;

    final double radius = dx < dy ? dx / 3 : dy / 3;

    final double cx = x + dx / 2; // center
    final double cy = y + dy / 2; // center

    for (int i = 0; i < coords.length; i++) {
      final double angle = Math.toRadians(start + theta * i);

      final double fx = Math.sin(angle) * radius; // may be neg.
      final double fy = Math.cos(angle) * radius; // may be neg.

      coords[i] = new PointDoubleXY(gf.makePrecise(0, cx + fx), gf.makePrecise(1, cy + fy));
    }
  }

  private static void fillHorz(final double x, final double dx, final double y, final double dy,
    final Point[] coords, final GeometryFactory gf) {
    final double fy = y + Math.random() * dy;
    double rx = dx; // remainder of x distance
    coords[0] = new PointDoubleXY(gf.makePrecise(0, x), gf.makePrecise(1, fy));
    for (int i = 1; i < coords.length - 1; i++) {
      rx -= Math.random() * rx;
      coords[i] = new PointDoubleXY(gf.makePrecise(0, x + dx - rx), gf.makePrecise(1, fy));
    }
    coords[coords.length - 1] = new PointDoubleXY(gf.makePrecise(0, x + dx), gf.makePrecise(1, fy));
  }

  private static void fillVert(final double x, final double dx, final double y, final double dy,
    final Point[] coords, final GeometryFactory gf) {
    final double fx = x + Math.random() * dx;
    double ry = dy; // remainder of y distance

    coords[0] = new PointDoubleXY(gf.makePrecise(0, fx), gf.makePrecise(1, y));
    for (int i = 1; i < coords.length - 1; i++) {
      ry -= Math.random() * ry;
      coords[i] = new PointDoubleXY(gf.makePrecise(0, fx), gf.makePrecise(1, y + dy - ry));
    }
    coords[coords.length - 1] = new PointDoubleXY(gf.makePrecise(0, fx), gf.makePrecise(1, y + dy));
  }

  protected int generationAlgorithm = 0;

  protected int numberPoints = 2;

  /**
   * @return Returns the generationAlgorithm.
   */
  public int getGenerationAlgorithm() {
    return this.generationAlgorithm;
  }

  /**
   * @return Returns the numberPoints.
   */
  public int getNumberPoints() {
    return this.numberPoints;
  }

  /**
   * As the user increases the number of points, the probability of creating a random valid linestring decreases.
   * Please take not of this when selecting the generation style, and the number of points.
   *
   * May return null if a geometry could not be created.
   *
   * @see #getNumberPoints()
   * @see #setNumberPoints(int)
   * @see #getGenerationAlgorithm()
   * @see #setGenerationAlgorithm(int)
   *
   * @see #VERT
   * @see #HORZ
   * @see #ARC
   *
   * @see com.revolsys.geometry.testold.generator.GeometryGenerator#newIterator()
   *
   * @throws IllegalStateException When the alg is not valid or the number of points is invalid
   * @throws NullPointerException when either the Geometry Factory, or the Bounding Box are undefined.
   */
  @Override
  public Geometry newGeometry() {

    if (this.geometryFactory == null) {
      throw new NullPointerException("GeometryFactoryI is not declared");
    }
    if (this.boundingBox == null || this.boundingBox.isEmpty()) {
      throw new NullPointerException("Bounding Box is not declared");
    }
    if (this.numberPoints < 2) {
      throw new IllegalStateException("Too few points");
    }

    final Point[] coords = new Point[this.numberPoints];

    final double x = this.boundingBox.getMinX(); // base x
    final double dx = this.boundingBox.getMaxX() - x;

    final double y = this.boundingBox.getMinY(); // base y
    final double dy = this.boundingBox.getMaxY() - y;

    for (int i = 0; i < RUNS; i++) {
      switch (getGenerationAlgorithm()) {
        case VERT:
          fillVert(x, dx, y, dy, coords, this.geometryFactory);
        break;
        case HORZ:
          fillHorz(x, dx, y, dy, coords, this.geometryFactory);
        break;
        case ARC:
          fillArc(x, dx, y, dy, coords, this.geometryFactory);
        break;
        default:
          throw new IllegalStateException("Invalid Alg. Specified");
      }

      final LineString ls = this.geometryFactory.lineString(coords);
      final IsValidOp valid = new IsValidOp(ls);
      if (valid.isValid()) {
        return ls;
      }
    }
    return null;
  }

  /**
   * @param generationAlgorithm The generationAlgorithm to set.
   */
  public void setGenerationAlgorithm(final int generationAlgorithm) {
    this.generationAlgorithm = generationAlgorithm;
  }

  /**
   * @param numberPoints The numberPoints to set.
   */
  public void setNumberPoints(final int numberPoints) {
    this.numberPoints = numberPoints;
  }

}
