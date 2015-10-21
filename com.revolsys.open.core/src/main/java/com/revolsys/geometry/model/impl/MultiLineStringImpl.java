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
package com.revolsys.geometry.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryCollection;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.LineString;
import com.revolsys.geometry.model.MultiLineString;
import com.revolsys.geometry.model.prep.PreparedMultiLineString;
import com.revolsys.util.WrappedException;

/**
 * Models a collection of (@link LineString}s.
 * <p>
 * Any collection of LineStrings is a valid MultiLineString.
 *
 *@version 1.7
 */
public class MultiLineStringImpl implements MultiLineString {
  private static final long serialVersionUID = 8166665132445433741L;

  /**
   *  The bounding box of this <code>Geometry</code>.
   */
  private BoundingBox boundingBox;

  /**
   * An object reference which can be used to carry ancillary data defined
   * by the client.
   */
  private Object userData;

  private final GeometryFactory geometryFactory;

  private LineString[] lines;

  public MultiLineStringImpl(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public MultiLineStringImpl(final GeometryFactory geometryFactory, final LineString... lines) {
    this.geometryFactory = geometryFactory;
    if (lines == null || lines.length == 0) {
      this.lines = null;
    } else if (Geometry.hasNullElements(lines)) {
      throw new IllegalArgumentException("geometries must not contain null elements");
    } else {
      this.lines = new LineString[lines.length];
      for (int i = 0; i < lines.length; i++) {
        final LineString line = lines[i];
        this.lines[i] = geometryFactory.lineString(line);
      }
    }
  }

  /**
   * Creates and returns a full copy of this {@link GeometryCollection} object.
   * (including all coordinates contained by it).
   *
   * @return a clone of this instance
   */
  @Override
  public MultiLineString clone() {
    try {
      return (MultiLineString)super.clone();
    } catch (final CloneNotSupportedException e) {
      throw new WrappedException(e);
    }
  }

  /**
   * Tests whether this geometry is structurally and numerically equal
   * to a given <code>Object</code>.
   * If the argument <code>Object</code> is not a <code>Geometry</code>,
   * the result is <code>false</code>.
   * Otherwise, the result is computed using
   * {@link #equals(2,Geometry)}.
   * <p>
   * This method is provided to fulfill the Java contract
   * for value-based object equality.
   * In conjunction with {@link #hashCode()}
   * it provides semantics which are most useful
   * for using
   * <code>Geometry</code>s as keys and values in Java collections.
   * <p>
   * Note that to produce the expected result the input geometries
   * should be in normal form.  It is the caller's
   * responsibility to perform this where required
   * (using {@link Geometry#norm()
   * or {@link #normalize()} as appropriate).
   *
   * @param other the Object to compare
   * @return true if this geometry is exactly equal to the argument
   *
   * @see #equals(2,Geometry)
   * @see #hashCode()
   * @see #norm()
   * @see #normalize()
   */
  @Override
  public boolean equals(final Object other) {
    if (other instanceof Geometry) {
      final Geometry geometry = (Geometry)other;
      return equals(2, geometry);
    } else {
      return false;
    }
  }

  @Override
  public BoundingBox getBoundingBox() {
    if (this.boundingBox == null) {
      if (isEmpty()) {
        this.boundingBox = new BoundingBoxDoubleGf(getGeometryFactory());
      } else {
        this.boundingBox = newBoundingBox();
      }
    }
    return this.boundingBox;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends Geometry> List<V> getGeometries() {
    if (this.lines == null) {
      return new ArrayList<V>();
    } else {
      return (List<V>)new ArrayList<>(Arrays.asList(this.lines));
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V extends Geometry> V getGeometry(final int n) {
    if (this.lines == null) {
      return null;
    } else {
      return (V)this.lines[n];
    }
  }

  @Override
  public int getGeometryCount() {
    if (this.lines == null) {
      return 0;
    } else {
      return this.lines.length;
    }
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  /**
   * Gets the user data object for this geometry, if any.
   *
   * @return the user data object, or <code>null</code> if none set
   */
  @Override
  public Object getUserData() {
    return this.userData;
  }

  /**
   * Gets a hash code for the Geometry.
   *
   * @return an integer value suitable for use as a hashcode
   */

  @Override
  public int hashCode() {
    return getBoundingBox().hashCode();
  }

  @Override
  public boolean isEmpty() {
    return this.lines == null;
  }

  @Override
  public MultiLineString prepare() {
    return new PreparedMultiLineString(this);
  }

  /**
   * A simple scheme for applications to add their own custom data to a Geometry.
   * An example use might be to add an object representing a Point Reference System.
   * <p>
   * Note that user data objects are not present in geometries created by
   * construction methods.
   *
   * @param userData an object, the semantics for which are defined by the
   * application using this Geometry
   */
  @Override
  public void setUserData(final Object userData) {
    this.userData = userData;
  }

  @Override
  public String toString() {
    return toWkt();
  }

}