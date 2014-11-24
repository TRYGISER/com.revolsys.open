/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 1.3.39
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.ogr;

public class ProgressCallback {
  protected static long getCPtr(final ProgressCallback obj) {
    return obj == null ? 0 : obj.swigCPtr;
  }

  private long swigCPtr;

  protected boolean swigCMemOwn;

  public ProgressCallback() {
    this(ogrJNI.new_ProgressCallback(), true);
  }

  protected ProgressCallback(final long cPtr, final boolean cMemoryOwn) {
    this.swigCMemOwn = cMemoryOwn;
    this.swigCPtr = cPtr;
  }

  public synchronized void delete() {
    if (this.swigCPtr != 0 && this.swigCMemOwn) {
      this.swigCMemOwn = false;
      ogrJNI.delete_ProgressCallback(this.swigCPtr);
    }
    this.swigCPtr = 0;
  }

  @Override
  protected void finalize() {
    delete();
  }

  public int run(final double dfComplete, final String pszMessage) {
    return ogrJNI.ProgressCallback_run(this.swigCPtr, this, dfComplete,
      pszMessage);
  }

}