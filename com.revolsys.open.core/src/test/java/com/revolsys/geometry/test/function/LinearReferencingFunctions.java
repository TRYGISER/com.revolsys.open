package com.revolsys.geometry.test.function;

import com.revolsys.geometry.linearref.LengthIndexedLine;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.Point;

public class LinearReferencingFunctions {
  public static Geometry extractLine(final Geometry g, final double start, final double end) {
    final LengthIndexedLine ll = new LengthIndexedLine(g);
    return ll.extractLine(start, end);
  }

  public static Geometry extractPoint(final Geometry g, final double index) {
    final LengthIndexedLine ll = new LengthIndexedLine(g);
    final Point p = ll.extractPoint(index);
    return g.getGeometryFactory().point(p);
  }

  public static Geometry project(final Geometry g, final Geometry g2) {
    final LengthIndexedLine ll = new LengthIndexedLine(g);
    final double index = ll.project(g2.getPoint());
    final Point p = ll.extractPoint(index);
    return g.getGeometryFactory().point(p);
  }

  public static double projectIndex(final Geometry g, final Geometry g2) {
    final LengthIndexedLine ll = new LengthIndexedLine(g);
    return ll.project(g2.getPoint());
  }

}
