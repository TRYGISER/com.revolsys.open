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
package com.revolsys.geometry.test.function;

import com.revolsys.beans.Classes;
import com.revolsys.geometry.model.Geometry;

/**
 * A base for implementations of
 * {@link GeometryFunction} which provides most
 * of the required structure.
 * Extenders must supply the behaviour for the
 * actual function invocation.
 *
 * @author Martin Davis
 *
 */
public abstract class BaseGeometryFunction implements GeometryFunction, Comparable {
  private static int compareTo(final Class c1, final Class c2) {
    return c1.getName().compareTo(c2.getName());
  }

  protected static Double getDoubleOrNull(final Object[] args, final int index) {
    if (args.length <= index) {
      return null;
    }
    if (args[index] == null) {
      return null;
    }
    return (Double)args[index];
  }

  protected static Integer getIntegerOrNull(final Object[] args, final int index) {
    if (args.length <= index) {
      return null;
    }
    if (args[index] == null) {
      return null;
    }
    return (Integer)args[index];
  }

  public static boolean isBinaryGeomFunction(final GeometryFunction func) {
    return func.getParameterTypes().length >= 1 && func.getParameterTypes()[0] == Geometry.class;
  }

  protected String category = null;

  protected String description;

  protected String name;

  protected String[] parameterNames;

  protected Class[] parameterTypes;

  protected Class returnType;

  public BaseGeometryFunction(final String category, final String name, final String description,
    final String[] parameterNames, final Class[] parameterTypes, final Class returnType) {
    this.category = category;
    this.name = name;
    this.description = description;
    this.parameterNames = parameterNames;
    this.parameterTypes = parameterTypes;
    this.returnType = returnType;
  }

  public BaseGeometryFunction(final String category, final String name,
    final String[] parameterNames, final Class[] parameterTypes, final Class returnType) {
    this.category = category;
    this.name = name;
    this.parameterNames = parameterNames;
    this.parameterTypes = parameterTypes;
    this.returnType = returnType;
  }

  @Override
  public int compareTo(final Object o) {
    final BaseGeometryFunction func = (BaseGeometryFunction)o;
    final int cmp = this.name.compareTo(func.getName());
    if (cmp != 0) {
      return cmp;
    }
    return compareTo(this.returnType, func.getReturnType());
    // TODO: compare parameter lists as well
  }

  /**
   * Two functions are the same if they have the
   * same signature (name, parameter types and return type).
   *
   * @param obj
   * @return true if this object is the same as the <tt>obj</tt> argument
   */
  @Override
  public boolean equals(final Object obj) {
    if (!(obj instanceof GeometryFunction)) {
      return false;
    }
    final GeometryFunction func = (GeometryFunction)obj;
    if (!this.name.equals(func.getName())) {
      return false;
    }
    if (!this.returnType.equals(func.getReturnType())) {
      return false;
    }

    final Class[] funcParamTypes = func.getParameterTypes();
    if (this.parameterTypes.length != funcParamTypes.length) {
      return false;
    }
    for (int i = 0; i < this.parameterTypes.length; i++) {
      if (!this.parameterTypes[i].equals(funcParamTypes[i])) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getCategory() {
    return this.category;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String[] getParameterNames() {
    return this.parameterNames;
  }

  /**
   * Gets the types of the other function arguments,
   * if any.
   *
   * @return the types
   */
  @Override
  public Class[] getParameterTypes() {
    return this.parameterTypes;
  }

  @Override
  public Class getReturnType() {
    return this.returnType;
  }

  @Override
  public String getSignature() {
    final StringBuilder paramTypes = new StringBuilder();
    paramTypes.append("Geometry");
    for (final Class parameterType : this.parameterTypes) {
      paramTypes.append(",");
      paramTypes.append(Classes.className(parameterType));
    }
    final Class clz = this.returnType;
    return this.name + "(" + paramTypes + ")" + " -> " + Classes.className(clz);
  }

  @Override
  public abstract Object invoke(Geometry geom, Object[] args);
}
