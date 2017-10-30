package com.revolsys.geometry.test.old.perf.math;

import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.util.Stopwatch;

/**
 * Test performance of evaluating TriangleImpl predicate computations
 * using
 * various extended precision APIs.
 *
 * @author Martin Davis
 *
 */
public class InCirclePerf {

  public static void main(final String[] args) throws Exception {
    final InCirclePerf test = new InCirclePerf();
    test.run();
  }

  Point pa = new PointDoubleXY(687958.05, 7460725.97);

  Point pb = new PointDoubleXY(687957.43, 7460725.93);

  Point pc = new PointDoubleXY(687957.58, 7460721);

  Point pp = new PointDoubleXY(687958.13, 7460720.99);

  public InCirclePerf() {
  }

  public void run() {
    // System.out.println("InCircle perf");
    final int n = 1000000;
    final double doubleTime = runDouble(n);
    final double ddSelfTime = runDDSelf(n);
    final double ddSelf2Time = runDDSelf2(n);
    final double ddTime = runDD(n);
    // double ddSelfTime = runDoubleDoubleSelf(10000000);

    // System.out.println("DD VS double performance factor = " + ddTime
    // / doubleTime);
    // System.out.println("DDSelf VS double performance factor = " + ddSelfTime
    // / doubleTime);
    // System.out.println("DDSelf2 VS double performance factor = " +
    // ddSelf2Time
    // / doubleTime);
  }

  public double runDD(final int nIter) {
    final Stopwatch sw = new Stopwatch();
    for (int i = 0; i < nIter; i++) {
      TriPredicate.isInCircleDD(this.pa, this.pb, this.pc, this.pp);
    }
    sw.stop();
    // System.out.println("DD: nIter = " + nIter + " time = "
    // + sw.getTimeString());
    return sw.getTime() / (double)nIter;
  }

  public double runDDSelf(final int nIter) {
    final Stopwatch sw = new Stopwatch();
    for (int i = 0; i < nIter; i++) {
      TriPredicate.isInCircleDD2(this.pa, this.pb, this.pc, this.pp);
    }
    sw.stop();
    // System.out.println("DD-Self: nIter = " + nIter + " time = "
    // + sw.getTimeString());
    return sw.getTime() / (double)nIter;
  }

  public double runDDSelf2(final int nIter) {
    final Stopwatch sw = new Stopwatch();
    for (int i = 0; i < nIter; i++) {
      TriPredicate.isInCircleDD3(this.pa, this.pb, this.pc, this.pp);
    }
    sw.stop();
    // System.out.println("DD-Self2: nIter = " + nIter + " time = "
    // + sw.getTimeString());
    return sw.getTime() / (double)nIter;
  }

  public double runDouble(final int nIter) {
    final Stopwatch sw = new Stopwatch();
    for (int i = 0; i < nIter; i++) {
      TriPredicate.isInCircle(this.pa, this.pb, this.pc, this.pp);
    }
    sw.stop();
    // System.out.println("double: nIter = " + nIter + " time = "
    // + sw.getTimeString());
    return sw.getTime() / (double)nIter;
  }

}
