package com.revolsys.geometry.test.old.perf.dissolve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.revolsys.geometry.dissolve.LineDissolver;
import com.revolsys.geometry.graph.linemerge.LineMerger;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.test.old.algorithm.InteriorPointTest;
import com.revolsys.geometry.test.old.perf.PerformanceTestCase;
import com.revolsys.geometry.test.old.perf.PerformanceTestRunner;
import com.revolsys.geometry.wkb.ParseException;

public class LineDissolvePerfTest extends PerformanceTestCase {
  public static void main(final String args[]) {
    PerformanceTestRunner.run(LineDissolvePerfTest.class);
  }

  Collection data;

  public LineDissolvePerfTest(final String name) {
    super(name);
    setRunSize(new int[] {
      1, 2, 3, 4, 5
    });
    setRunIterations(1);
  }

  private Geometry dissolveLines(final Collection lines) {
    final Geometry linesGeom = extractLines(lines);
    return dissolveLines(linesGeom);
  }

  private Geometry dissolveLines(final Geometry lines) {
    final Geometry dissolved = lines.union();
    final List<LineString> mergedColl = LineMerger.merge(dissolved);
    final Geometry merged = lines.getGeometryFactory().buildGeometry(mergedColl);
    return merged;
  }

  Geometry extractLines(final Collection geoms) {
    GeometryFactory factory = null;
    final List lines = new ArrayList();
    for (final Iterator i = geoms.iterator(); i.hasNext();) {
      final Geometry g = (Geometry)i.next();
      if (factory == null) {
        factory = g.getGeometryFactory();
      }
      lines.addAll(g.getGeometryComponents(LineString.class));
    }
    return factory.buildGeometry(geoms);
  }

  public void runBruteForce_World() {
    final Geometry result = dissolveLines(this.data);
    // System.out.println(Memory.allString());
  }

  public void runDissolver_World() {
    final LineDissolver dis = new LineDissolver();
    dis.add(this.data);
    final Geometry result = dis.getResult();
    // System.out.println();
    // System.out.println(Memory.allString());
  }

  @Override
  public void setUp() throws IOException, ParseException {
    // System.out.println("Loading data...");
    this.data = InteriorPointTest.getTestGeometries("world.wkt");
  }

}
