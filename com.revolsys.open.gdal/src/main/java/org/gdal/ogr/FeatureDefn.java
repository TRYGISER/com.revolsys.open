/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.5
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.ogr;

public class FeatureDefn {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected FeatureDefn(long cPtr, boolean cMemoryOwn) {
    if (cPtr == 0)
        throw new RuntimeException();
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }
  
  protected static long getCPtr(FeatureDefn obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        ogrJNI.delete_FeatureDefn(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  private Object parentReference;

  protected static long getCPtrAndDisown(FeatureDefn obj) {
    if (obj != null)
    {
        obj.swigCMemOwn= false;
        obj.parentReference = null;
    }
    return getCPtr(obj);
  }

  /* Ensure that the GC doesn't collect any parent instance set from Java */
  protected void addReference(Object reference) {
    parentReference = reference;
  }

  public boolean equals(Object obj) {
    boolean equal = false;
    if (obj instanceof FeatureDefn)
      equal = (((FeatureDefn)obj).swigCPtr == this.swigCPtr);
    return equal;
  }

  public int hashCode() {
     return (int)swigCPtr;
  }


  public FeatureDefn(String name_null_ok) {
    this(ogrJNI.new_FeatureDefn__SWIG_0(name_null_ok), true);
  }

  public FeatureDefn() {
    this(ogrJNI.new_FeatureDefn__SWIG_1(), true);
  }

  public String GetName() {
    return ogrJNI.FeatureDefn_GetName(swigCPtr, this);
  }

  public int GetFieldCount() {
    return ogrJNI.FeatureDefn_GetFieldCount(swigCPtr, this);
  }

  public FieldDefn GetFieldDefn(int i) {
    long cPtr = ogrJNI.FeatureDefn_GetFieldDefn(swigCPtr, this, i);
    FieldDefn ret = null;
    if (cPtr != 0) {
      ret = new FieldDefn(cPtr, false);
      ret.addReference(this);
    }
    return ret;
  }

  public int GetFieldIndex(String name) {
    return ogrJNI.FeatureDefn_GetFieldIndex(swigCPtr, this, name);
  }

  public void AddFieldDefn(FieldDefn defn) {
    ogrJNI.FeatureDefn_AddFieldDefn(swigCPtr, this, FieldDefn.getCPtr(defn), defn);
  }

  public int GetGeomFieldCount() {
    return ogrJNI.FeatureDefn_GetGeomFieldCount(swigCPtr, this);
  }

  public GeomFieldDefn GetGeomFieldDefn(int i) {
    long cPtr = ogrJNI.FeatureDefn_GetGeomFieldDefn(swigCPtr, this, i);
    return (cPtr == 0) ? null : new GeomFieldDefn(cPtr, false);
  }

  public int GetGeomFieldIndex(String name) {
    return ogrJNI.FeatureDefn_GetGeomFieldIndex(swigCPtr, this, name);
  }

  public void AddGeomFieldDefn(GeomFieldDefn defn) {
    ogrJNI.FeatureDefn_AddGeomFieldDefn(swigCPtr, this, GeomFieldDefn.getCPtr(defn), defn);
  }

  public int DeleteGeomFieldDefn(int idx) {
    return ogrJNI.FeatureDefn_DeleteGeomFieldDefn(swigCPtr, this, idx);
  }

  public int GetGeomType() {
    return ogrJNI.FeatureDefn_GetGeomType(swigCPtr, this);
  }

  public void SetGeomType(int geom_type) {
    ogrJNI.FeatureDefn_SetGeomType(swigCPtr, this, geom_type);
  }

  public int GetReferenceCount() {
    return ogrJNI.FeatureDefn_GetReferenceCount(swigCPtr, this);
  }

  public int IsGeometryIgnored() {
    return ogrJNI.FeatureDefn_IsGeometryIgnored(swigCPtr, this);
  }

  public void SetGeometryIgnored(int bIgnored) {
    ogrJNI.FeatureDefn_SetGeometryIgnored(swigCPtr, this, bIgnored);
  }

  public int IsStyleIgnored() {
    return ogrJNI.FeatureDefn_IsStyleIgnored(swigCPtr, this);
  }

  public void SetStyleIgnored(int bIgnored) {
    ogrJNI.FeatureDefn_SetStyleIgnored(swigCPtr, this, bIgnored);
  }

  public int IsSame(FeatureDefn other_defn) {
    return ogrJNI.FeatureDefn_IsSame(swigCPtr, this, FeatureDefn.getCPtr(other_defn), other_defn);
  }

}
