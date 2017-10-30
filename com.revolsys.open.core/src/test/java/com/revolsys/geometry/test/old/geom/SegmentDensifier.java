package com.revolsys.geometry.test.old.geom;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.PointList;
import com.revolsys.geometry.model.impl.PointDoubleXY;

/**
 * Densifies a LineString
 *
 * @version 1.7
 */
public class SegmentDensifier {
  private final LineString inputLine;

  private PointList newCoords;

  public SegmentDensifier(final LineString line) {
    this.inputLine = line;
  }

  public Geometry densify(final double segLength) {
    this.newCoords = new PointList();

    final LineString seq = this.inputLine;

    this.newCoords.add(seq.getPoint(0));

    for (int i = 0; i < seq.getVertexCount() - 1; i++) {
      final Point p0 = seq.getPoint(i);
      final Point p1 = seq.getPoint(i + 1);
      densify(p0, p1, segLength);
    }
    final Point[] newPts = this.newCoords.toPointArray();
    return this.inputLine.getGeometryFactory().lineString(newPts);
  }

  private void densify(final Point p0, final Point p1, final double segLength) {
    final double origLen = p1.distancePoint(p0);
    final int nPtsToAdd = (int)Math.floor(origLen / segLength);

    final double delx = p1.getX() - p0.getX();
    final double dely = p1.getY() - p0.getY();

    final double segLenFrac = segLength / origLen;
    for (int i = 0; i <= nPtsToAdd; i++) {
      final double addedPtFrac = i * segLenFrac;
      final Point pt = new PointDoubleXY(p0.getX() + addedPtFrac * delx,
        p0.getY() + addedPtFrac * dely);
      this.newCoords.add(pt, false);
    }
    this.newCoords.add(new PointDoubleXY(p1), false);
  }
}
