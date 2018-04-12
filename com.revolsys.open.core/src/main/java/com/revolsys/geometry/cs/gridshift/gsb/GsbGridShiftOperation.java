package com.revolsys.geometry.cs.gridshift.gsb;

import com.revolsys.geometry.cs.gridshift.GridShiftOperation;
import com.revolsys.geometry.cs.projection.CoordinatesOperationPoint;

public class GsbGridShiftOperation implements GridShiftOperation {

  private final GsbGridShiftFile file;

  public GsbGridShiftOperation(final GsbGridShiftFile file) {
    this.file = file;
  }

  @Override
  public boolean shift(final CoordinatesOperationPoint point) {
    final double lon = point.x;
    final double lat = point.y;
    final double lonPositiveWestSeconds = -lon * 3600;
    final double latSeconds = lat * 3600;
    final GsbGridShiftGrid gsbGridShiftGrid = this.file.getGrid(lonPositiveWestSeconds, latSeconds);
    if (gsbGridShiftGrid == null) {
      return false;
    } else {
      final double lonShift = gsbGridShiftGrid.getLonShift(lonPositiveWestSeconds, latSeconds);
      final double latShift = gsbGridShiftGrid.getLatShift(lonPositiveWestSeconds, latSeconds);
      point.x = -(lonPositiveWestSeconds + lonShift) / 3600;
      point.y = (latSeconds + latShift) / 3600;
      return true;
    }
  }

  @Override
  public String toString() {
    return this.file.toString();
  }
}
