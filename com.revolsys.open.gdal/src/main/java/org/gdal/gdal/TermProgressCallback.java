/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.5
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package org.gdal.gdal;

public class TermProgressCallback extends ProgressCallback {
  private long swigCPtr;

  protected TermProgressCallback(long cPtr, boolean cMemoryOwn) {
    super(gdalJNI.TermProgressCallback_SWIGUpcast(cPtr), cMemoryOwn);
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TermProgressCallback obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        gdalJNI.delete_TermProgressCallback(swigCPtr);
      }
      swigCPtr = 0;
    }
    super.delete();
  }

  public TermProgressCallback() {
    this(gdalJNI.new_TermProgressCallback(), true);
  }

  public int run(double dfComplete, String pszMessage) {
    return gdalJNI.TermProgressCallback_run(swigCPtr, this, dfComplete, pszMessage);
  }

}
