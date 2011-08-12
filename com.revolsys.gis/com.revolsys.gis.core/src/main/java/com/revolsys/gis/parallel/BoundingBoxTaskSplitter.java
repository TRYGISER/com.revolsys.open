package com.revolsys.gis.parallel;

import com.revolsys.gis.cs.BoundingBox;
import com.revolsys.parallel.process.AbstractProcess;

public abstract class BoundingBoxTaskSplitter extends AbstractProcess {
  private BoundingBox boundingBox;

  private int numX = 10;

  private int numY = 10;

  public abstract void execute(BoundingBox cellBoundingBox);

  public BoundingBox getBoundingBox() {
    return boundingBox;
  }

  public int getNumX() {
    return numX;
  }

  public int getNumY() {
    return numY;
  }

  public void run() {
    preRun();
    try {
      if (boundingBox != null) {
        final double xInc = boundingBox.getWidth() / numX;
        final double yInc = boundingBox.getHeight() / numY;
        double x = boundingBox.getMinX();
        for (int i = 0; i < numX; i++) {
          double y = boundingBox.getMinY();
          for (int j = 0; j < numX; j++) {
            final BoundingBox cellBoundingBox = new BoundingBox(
              boundingBox.getGeometryFactory(), x, y, x + xInc, y + yInc);
            execute(cellBoundingBox);
            y += yInc;
          }
          x += xInc;
        }
      }
    } finally {
      postRun();
    }
  }

  protected void preRun() {
  }

  protected void postRun() {
  }

  public void setBoundingBox(final BoundingBox boundingBox) {
    this.boundingBox = boundingBox;
  }

  public void setNumX(final int numX) {
    this.numX = numX;
  }

  public void setNumY(final int numY) {
    this.numY = numY;
  }

}
