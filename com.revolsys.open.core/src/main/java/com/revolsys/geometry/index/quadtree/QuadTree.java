package com.revolsys.geometry.index.quadtree;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.revolsys.geometry.index.SpatialIndex;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.util.ExitLoopException;
import com.revolsys.visitor.CreateListVisitor;
import com.revolsys.visitor.SingleObjectVisitor;

public class QuadTree<T> implements SpatialIndex<T>, Serializable {
  private static final long serialVersionUID = 1L;

  public static double[] ensureExtent(final double[] bounds, final double minExtent) {
    double minX = bounds[0];
    double maxX = bounds[2];
    double minY = bounds[1];
    double maxY = bounds[3];
    if (minX != maxX && minY != maxY) {
      return bounds;
    } else {
      if (minX == maxX) {
        minX = minX - minExtent / 2.0;
        maxX = minX + minExtent / 2.0;
      }
      if (minY == maxY) {
        minY = minY - minExtent / 2.0;
        maxY = minY + minExtent / 2.0;
      }
      return new double[] {
        minX, minY, maxX, maxY
      };
    }
  }

  private GeometryFactory geometryFactory = GeometryFactory.DEFAULT_3D;

  private double minExtent;

  private final double absoluteMinExtent;

  private double minExtentTimes2;

  private AbstractQuadTreeNode<T> root;

  private int size = 0;

  private boolean useEquals = false;

  public QuadTree(final GeometryFactory geometryFactory) {
    this(geometryFactory, new QuadTreeNode<>());
  }

  protected QuadTree(final GeometryFactory geometryFactory, final AbstractQuadTreeNode<T> root) {
    if (geometryFactory == null) {
      this.geometryFactory = GeometryFactory.DEFAULT_3D;
    } else {
      this.geometryFactory = geometryFactory;
    }
    if (this.geometryFactory.isFloating()) {
      this.absoluteMinExtent = 0.00000001;
    } else {
      this.absoluteMinExtent = this.geometryFactory.getResolutionXy();
    }
    if (this.absoluteMinExtent < 0.5) {
      this.minExtent = 0.5;
    } else {
      this.minExtent = this.absoluteMinExtent;
    }
    this.minExtentTimes2 = this.minExtent * 2;
    this.root = root;
  }

  public void clear() {
    this.root.clear();
    this.size = 0;
  }

  public int depth() {
    return this.root.depth();
  }

  protected boolean equalsItem(final T item1, final T item2) {
    if (item1 == item2) {
      return true;
    } else if (this.useEquals) {
      return item1.equals(item2);
    } else {
      return false;
    }
  }

  // TODO forEach and remove in one call

  @Override
  public void forEach(final Consumer<? super T> action) {
    try {
      this.root.forEach(this, action);
    } catch (final ExitLoopException e) {
    }
  }

  @Override
  public void forEach(final double x, final double y, final Consumer<? super T> action) {
    this.root.forEach(this, x, y, action);
  }

  @Override
  public void forEach(final double minX, final double minY, final double maxX, final double maxY,
    final Consumer<? super T> action) {
    this.root.forEach(this, minX, minY, maxX, maxY, action);
  }

  public List<T> getAll() {
    final CreateListVisitor<T> visitor = new CreateListVisitor<>();
    forEach(visitor);
    return visitor.getList();
  }

  public T getFirst(final BoundingBox boundingBox, final Predicate<T> filter) {
    final SingleObjectVisitor<T> visitor = new SingleObjectVisitor<>(filter);
    forEach(boundingBox, visitor);
    return visitor.getObject();
  }

  public T getFirstBoundingBox(final Geometry geometry, final Predicate<T> filter) {
    if (geometry == null) {
      return null;
    } else {
      final BoundingBox boundingBox = geometry.getBoundingBox();
      return getFirst(boundingBox, filter);
    }
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    return this.geometryFactory;
  }

  public double getMinExtent() {
    return this.minExtent;
  }

  public double getMinExtentTimes2() {
    return this.minExtentTimes2;
  }

  @Override
  public int getSize() {
    return this.size;
  }

  @Override
  public void insertItem(final BoundingBox boundingBox, final T item) {
    final BoundingBox convertedBoundingBox = convertBoundingBox(boundingBox);
    if (convertedBoundingBox == null || convertedBoundingBox.isEmpty()) {
      throw new IllegalArgumentException("Item bounding box " + boundingBox
        + " must not be null or empty in coordinate system: " + getCoordinateSystemId());
    } else {
      final double minX = convertedBoundingBox.getMinX();
      final double minY = convertedBoundingBox.getMinY();
      final double maxX = convertedBoundingBox.getMaxX();
      final double maxY = convertedBoundingBox.getMaxY();

      insertItem(minX, minY, maxX, maxY, item);
    }
  }

  public void insertItem(final double minX, final double minY, final double maxX, final double maxY,
    final T item) {
    final double delX = maxX - minX;
    if (delX < this.minExtent && delX > 0) {
      this.minExtent = this.geometryFactory.makeXyPrecise(delX);
      if (this.minExtent < this.absoluteMinExtent) {
        this.minExtent = this.absoluteMinExtent;
        this.minExtentTimes2 = this.minExtent * 2;
      }
    }
    final double delY = maxY - minY;
    if (delY < this.minExtent & delY > 0) {
      this.minExtent = this.geometryFactory.makeXyPrecise(delY);
      if (this.minExtent < this.absoluteMinExtent) {
        this.minExtent = this.absoluteMinExtent;
        this.minExtentTimes2 = this.minExtent * 2;
      }
    }

    if (this.root.insertRoot(this, minX, minY, maxX, maxY, item)) {
      this.size++;
    }
  }

  public void insertItem(final double x, final double y, final T item) {
    insertItem(x, y, x, y, item);
  }

  public List<T> queryBoundingBox(final Geometry geometry) {
    return getItems(geometry);
  }

  @Override
  public boolean removeItem(BoundingBox boundingBox, final T item) {
    boundingBox = convertBoundingBox(boundingBox);
    if (boundingBox != null && !boundingBox.isEmpty()) {
      final double minX = boundingBox.getMinX();
      final double minY = boundingBox.getMinY();
      final double maxX = boundingBox.getMaxX();
      final double maxY = boundingBox.getMaxY();

      return removeItem(minX, minY, maxX, maxY, item);
    } else {
      return false;
    }
  }

  public boolean removeItem(final double minX, final double minY, final double maxX,
    final double maxY, final T item) {
    final boolean removed = this.root.removeItem(this, minX, minY, maxX, maxY, item);
    if (removed) {
      this.size--;
    }
    return removed;
  }

  public void setGeometryFactory(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public void setUseEquals(final boolean useEquals) {
    this.useEquals = useEquals;
  }

  public int size() {
    return getSize();
  }
}
