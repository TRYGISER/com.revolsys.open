/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.39
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.ogr;

import org.gdal.osr.CoordinateTransformation;
import org.gdal.osr.SpatialReference;

public class Geometry implements Cloneable {
  public static Geometry CreateFromGML(final String gml) {
    return ogr.CreateGeometryFromGML(gml);
  }

  public static Geometry CreateFromJson(final String json) {
    return ogr.CreateGeometryFromJson(json);
  }

  public static Geometry CreateFromWkb(final byte[] wkb) {
    return ogr.CreateGeometryFromWkb(wkb);
  }

  public static Geometry CreateFromWkt(final String wkt) {
    return ogr.CreateGeometryFromWkt(wkt);
  }

  protected static long getCPtr(final Geometry obj) {
    return obj == null ? 0 : obj.swigCPtr;
  }

  protected static long getCPtrAndDisown(final Geometry obj) {
    if (obj != null) {
      if (obj.nativeObject == null) {
        throw new RuntimeException(
          "Cannot disown an object that was not owned...");
      }
      obj.nativeObject.dontDisposeNativeResources();
      obj.nativeObject = null;
    }
    return getCPtr(obj);
  }

  private final long swigCPtr;

  private GeometryNative nativeObject;

  public Geometry(final int type) {
    this(ogrJNI.new_Geometry__SWIG_1(type), true);
  }

  public Geometry(final int type, final String wkt, final byte[] nLen,
    final String gml) {
    this(ogrJNI.new_Geometry__SWIG_0(type, wkt, nLen, gml), true);
  }

  protected Geometry(final long cPtr, final boolean cMemoryOwn) {
    if (cPtr == 0) {
      throw new RuntimeException();
    }
    this.swigCPtr = cPtr;
    if (cMemoryOwn) {
      this.nativeObject = new GeometryNative(this, cPtr);
    }
  }

  public int AddGeometry(final Geometry other) {
    return ogrJNI.Geometry_AddGeometry(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public int AddGeometryDirectly(final Geometry other_disown) {
    final int ret = ogrJNI.Geometry_AddGeometryDirectly(this.swigCPtr, this,
      Geometry.getCPtrAndDisown(other_disown), other_disown);
    if (other_disown != null) {
      other_disown.addReference(this);
    }
    return ret;
  }

  public void AddPoint(final double x, final double y) {
    ogrJNI.Geometry_AddPoint__SWIG_1(this.swigCPtr, this, x, y);
  }

  public void AddPoint(final double x, final double y, final double z) {
    ogrJNI.Geometry_AddPoint__SWIG_0(this.swigCPtr, this, x, y, z);
  }

  public void AddPoint_2D(final double x, final double y) {
    ogrJNI.Geometry_AddPoint_2D(this.swigCPtr, this, x, y);
  }

  /* Ensure that the GC doesn't collect any parent instance set from Java */
  protected void addReference(final Object reference) {
  }

  public double Area() {
    return ogrJNI.Geometry_Area(this.swigCPtr, this);
  }

  public void AssignSpatialReference(final SpatialReference reference) {
    ogrJNI.Geometry_AssignSpatialReference(this.swigCPtr, this,
      SpatialReference.getCPtr(reference), reference);
  }

  public Geometry Boundary() {
    final long cPtr = ogrJNI.Geometry_Boundary(this.swigCPtr, this);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public Geometry Buffer(final double distance) {
    final long cPtr = ogrJNI.Geometry_Buffer__SWIG_1(this.swigCPtr, this,
      distance);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public Geometry Buffer(final double distance, final int quadsecs) {
    final long cPtr = ogrJNI.Geometry_Buffer__SWIG_0(this.swigCPtr, this,
      distance, quadsecs);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public Geometry Centroid() {
    final long cPtr = ogrJNI.Geometry_Centroid(this.swigCPtr, this);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  @Override
  public Object clone() {
    return Clone();
  }

  public Geometry Clone() {
    final long cPtr = ogrJNI.Geometry_Clone(this.swigCPtr, this);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public void CloseRings() {
    ogrJNI.Geometry_CloseRings(this.swigCPtr, this);
  }

  public boolean Contains(final Geometry other) {
    return ogrJNI.Geometry_Contains(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public Geometry ConvexHull() {
    final long cPtr = ogrJNI.Geometry_ConvexHull(this.swigCPtr, this);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public boolean Crosses(final Geometry other) {
    return ogrJNI.Geometry_Crosses(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public void delete() {
    if (this.nativeObject != null) {
      this.nativeObject.delete();
      this.nativeObject = null;
    }
  }

  public Geometry Difference(final Geometry other) {
    final long cPtr = ogrJNI.Geometry_Difference(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public boolean Disjoint(final Geometry other) {
    return ogrJNI.Geometry_Disjoint(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public double Distance(final Geometry other) {
    return ogrJNI.Geometry_Distance(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public void Empty() {
    ogrJNI.Geometry_Empty(this.swigCPtr, this);
  }

  public boolean Equal(final Geometry other) {
    return ogrJNI.Geometry_Equal(this.swigCPtr, this, Geometry.getCPtr(other),
      other);
  }

  @Override
  public boolean equals(final Object obj) {
    boolean equal = false;
    if (obj instanceof Geometry) {
      equal = Equal((Geometry)obj);
    }
    return equal;
  }

  public boolean Equals(final Geometry other) {
    return ogrJNI.Geometry_Equals(this.swigCPtr, this, Geometry.getCPtr(other),
      other);
  }

  public String ExportToGML() {
    return ogrJNI.Geometry_ExportToGML__SWIG_1(this.swigCPtr, this);
  }

  public String ExportToGML(final java.util.Vector options) {
    return ogrJNI.Geometry_ExportToGML__SWIG_0(this.swigCPtr, this, options);
  }

  public String ExportToJson() {
    return ogrJNI.Geometry_ExportToJson__SWIG_1(this.swigCPtr, this);
  }

  public String ExportToJson(final java.util.Vector options) {
    return ogrJNI.Geometry_ExportToJson__SWIG_0(this.swigCPtr, this, options);
  }

  public String ExportToKML() {
    return ogrJNI.Geometry_ExportToKML__SWIG_1(this.swigCPtr, this);
  }

  public String ExportToKML(final String altitude_mode) {
    return ogrJNI.Geometry_ExportToKML__SWIG_0(this.swigCPtr, this,
      altitude_mode);
  }

  public byte[] ExportToWkb() {
    return ogrJNI.Geometry_ExportToWkb__SWIG_1(this.swigCPtr, this);
  }

  public int ExportToWkb(final byte[] wkbArray, final int byte_order) {
    if (wkbArray == null) {
      throw new NullPointerException();
    }
    final byte[] srcArray = ExportToWkb(byte_order);
    if (wkbArray.length < srcArray.length) {
      throw new RuntimeException("Array too small");
    }

    System.arraycopy(srcArray, 0, wkbArray, 0, srcArray.length);

    return 0;
  }

  public byte[] ExportToWkb(final int byte_order) {
    return ogrJNI.Geometry_ExportToWkb__SWIG_0(this.swigCPtr, this, byte_order);
  }

  public String ExportToWkt() {
    return ogrJNI.Geometry_ExportToWkt__SWIG_1(this.swigCPtr, this);
  }

  public int ExportToWkt(final String[] argout) {
    return ogrJNI.Geometry_ExportToWkt__SWIG_0(this.swigCPtr, this, argout);
  }

  public void FlattenTo2D() {
    ogrJNI.Geometry_FlattenTo2D(this.swigCPtr, this);
  }

  public double GetArea() {
    return ogrJNI.Geometry_GetArea(this.swigCPtr, this);
  }

  public Geometry GetBoundary() {
    final long cPtr = ogrJNI.Geometry_GetBoundary(this.swigCPtr, this);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public int GetCoordinateDimension() {
    return ogrJNI.Geometry_GetCoordinateDimension(this.swigCPtr, this);
  }

  public int GetDimension() {
    return ogrJNI.Geometry_GetDimension(this.swigCPtr, this);
  }

  public void GetEnvelope(final double[] argout) {
    ogrJNI.Geometry_GetEnvelope(this.swigCPtr, this, argout);
  }

  public void GetEnvelope3D(final double[] argout) {
    ogrJNI.Geometry_GetEnvelope3D(this.swigCPtr, this, argout);
  }

  public int GetGeometryCount() {
    return ogrJNI.Geometry_GetGeometryCount(this.swigCPtr, this);
  }

  public String GetGeometryName() {
    return ogrJNI.Geometry_GetGeometryName(this.swigCPtr, this);
  }

  public Geometry GetGeometryRef(final int geom) {
    final long cPtr = ogrJNI.Geometry_GetGeometryRef(this.swigCPtr, this, geom);
    Geometry ret = null;
    if (cPtr != 0) {
      ret = new Geometry(cPtr, false);
      ret.addReference(this);
    }
    return ret;
  }

  public int GetGeometryType() {
    return ogrJNI.Geometry_GetGeometryType(this.swigCPtr, this);
  }

  public double[] GetPoint(final int iPoint) {
    final double[] coords = new double[3];
    GetPoint(iPoint, coords);
    return coords;
  }

  public void GetPoint(final int iPoint, final double[] argout) {
    ogrJNI.Geometry_GetPoint(this.swigCPtr, this, iPoint, argout);
  }

  public double[] GetPoint_2D(final int iPoint) {
    final double[] coords = new double[2];
    GetPoint_2D(iPoint, coords);
    return coords;
  }

  public void GetPoint_2D(final int iPoint, final double[] argout) {
    ogrJNI.Geometry_GetPoint_2D(this.swigCPtr, this, iPoint, argout);
  }

  public int GetPointCount() {
    return ogrJNI.Geometry_GetPointCount(this.swigCPtr, this);
  }

  public double[][] GetPoints() {
    return ogrJNI.Geometry_GetPoints__SWIG_1(this.swigCPtr, this);
  }

  public double[][] GetPoints(final int nCoordDimension) {
    return ogrJNI.Geometry_GetPoints__SWIG_0(this.swigCPtr, this,
      nCoordDimension);
  }

  public SpatialReference GetSpatialReference() {
    final long cPtr = ogrJNI.Geometry_GetSpatialReference(this.swigCPtr, this);
    return cPtr == 0 ? null : new SpatialReference(cPtr, true);
  }

  public double GetX() {
    return ogrJNI.Geometry_GetX__SWIG_1(this.swigCPtr, this);
  }

  public double GetX(final int point) {
    return ogrJNI.Geometry_GetX__SWIG_0(this.swigCPtr, this, point);
  }

  public double GetY() {
    return ogrJNI.Geometry_GetY__SWIG_1(this.swigCPtr, this);
  }

  public double GetY(final int point) {
    return ogrJNI.Geometry_GetY__SWIG_0(this.swigCPtr, this, point);
  }

  public double GetZ() {
    return ogrJNI.Geometry_GetZ__SWIG_1(this.swigCPtr, this);
  }

  public double GetZ(final int point) {
    return ogrJNI.Geometry_GetZ__SWIG_0(this.swigCPtr, this, point);
  }

  @Override
  public int hashCode() {
    return (int)this.swigCPtr;
  }

  public boolean Intersect(final Geometry other) {
    return ogrJNI.Geometry_Intersect(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public Geometry Intersection(final Geometry other) {
    final long cPtr = ogrJNI.Geometry_Intersection(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public boolean Intersects(final Geometry other) {
    return ogrJNI.Geometry_Intersects(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public boolean IsEmpty() {
    return ogrJNI.Geometry_IsEmpty(this.swigCPtr, this);
  }

  public boolean IsRing() {
    return ogrJNI.Geometry_IsRing(this.swigCPtr, this);
  }

  public boolean IsSimple() {
    return ogrJNI.Geometry_IsSimple(this.swigCPtr, this);
  }

  public boolean IsValid() {
    return ogrJNI.Geometry_IsValid(this.swigCPtr, this);
  }

  public double Length() {
    return ogrJNI.Geometry_Length(this.swigCPtr, this);
  }

  public boolean Overlaps(final Geometry other) {
    return ogrJNI.Geometry_Overlaps(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public Geometry PointOnSurface() {
    final long cPtr = ogrJNI.Geometry_PointOnSurface(this.swigCPtr, this);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public void Segmentize(final double dfMaxLength) {
    ogrJNI.Geometry_Segmentize(this.swigCPtr, this, dfMaxLength);
  }

  public void SetCoordinateDimension(final int dimension) {
    ogrJNI.Geometry_SetCoordinateDimension(this.swigCPtr, this, dimension);
  }

  public void SetPoint(final int point, final double x, final double y) {
    ogrJNI.Geometry_SetPoint__SWIG_1(this.swigCPtr, this, point, x, y);
  }

  public void SetPoint(final int point, final double x, final double y,
    final double z) {
    ogrJNI.Geometry_SetPoint__SWIG_0(this.swigCPtr, this, point, x, y, z);
  }

  public void SetPoint_2D(final int point, final double x, final double y) {
    ogrJNI.Geometry_SetPoint_2D(this.swigCPtr, this, point, x, y);
  }

  public Geometry Simplify(final double tolerance) {
    final long cPtr = ogrJNI.Geometry_Simplify(this.swigCPtr, this, tolerance);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public Geometry SimplifyPreserveTopology(final double tolerance) {
    final long cPtr = ogrJNI.Geometry_SimplifyPreserveTopology(this.swigCPtr,
      this, tolerance);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public Geometry SymDifference(final Geometry other) {
    final long cPtr = ogrJNI.Geometry_SymDifference(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public Geometry SymmetricDifference(final Geometry other) {
    final long cPtr = ogrJNI.Geometry_SymmetricDifference(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public boolean Touches(final Geometry other) {
    return ogrJNI.Geometry_Touches(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
  }

  public int Transform(final CoordinateTransformation trans) {
    return ogrJNI.Geometry_Transform(this.swigCPtr, this,
      CoordinateTransformation.getCPtr(trans), trans);
  }

  public int TransformTo(final SpatialReference reference) {
    return ogrJNI.Geometry_TransformTo(this.swigCPtr, this,
      SpatialReference.getCPtr(reference), reference);
  }

  public Geometry Union(final Geometry other) {
    final long cPtr = ogrJNI.Geometry_Union(this.swigCPtr, this,
      Geometry.getCPtr(other), other);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public Geometry UnionCascaded() {
    final long cPtr = ogrJNI.Geometry_UnionCascaded(this.swigCPtr, this);
    return cPtr == 0 ? null : new Geometry(cPtr, true);
  }

  public boolean Within(final Geometry other) {
    return ogrJNI.Geometry_Within(this.swigCPtr, this, Geometry.getCPtr(other),
      other);
  }

  public int WkbSize() {
    return ogrJNI.Geometry_WkbSize(this.swigCPtr, this);
  }

}