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
package com.revolsys.geometry.test.geomop;

import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.test.testrunner.Result;

/**
 * A {@link GeometryOperation} which logs
 * the input and output from another
 * {@link GeometryOperation}.
 * The log is sent to {@link System#out}.
 *
 * @author mbdavis
 *
 */
public class LoggingGeometryOperation implements GeometryOperation {
  private GeometryOperation geomOp = new GeometryMethodOperation();

  public LoggingGeometryOperation() {

  }

  public LoggingGeometryOperation(final GeometryOperation geomOp) {
    this.geomOp = geomOp;
  }

  @Override
  public Class getReturnType(final String opName) {
    return GeometryMethodOperation.getGeometryReturnType(opName);
  }

  @Override
  public Result invoke(final String opName, final Geometry geometry, final Object[] args)
    throws Exception {
    System.out.println("Operation <" + opName + ">");
    System.out.println("Geometry: " + geometry);
    for (int i = 0; i < args.length; i++) {
      System.out.println("Arg[" + i + "]: " + args[i]);
    }
    final Result result = this.geomOp.invoke(opName, geometry, args);
    System.out.println("Result==> " + result.toFormattedString());
    return result;
  }
}
