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
package com.revolsys.geometry.index.strtree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import com.revolsys.util.Emptyable;

/**
 * A node of an {@link AbstractSTRtree}. A node is one of:
 * <ul>
 * <li>empty
 * <li>an <i>interior node</i> containing child {@link AbstractNode}s
 * <li>a <i>leaf node</i> containing data items ({@link ItemBoundable}s).
 * </ul>
 * A node stores the bounds of its children, and its level within the index tree.
 *
 * @version 1.7
 */
public abstract class AbstractNode<B, I> implements Emptyable, Boundable<B, I>, Serializable {
  private static final long serialVersionUID = 6493722185909573708L;

  private B bounds = null;

  private final List<Boundable<B, I>> children = new ArrayList<>();

  private int level;

  /**
   * Default constructor required for serialization.
   */
  public AbstractNode() {
  }

  /**
   * Constructs an AbstractNode at the given level in the tree
   * @param level 0 if this node is a leaf, 1 if a parent of a leaf, and so on; the
   * root node will have the highest level
   */
  public AbstractNode(final int level) {
    this.level = level;
  }

  /**
   * Adds either an AbstractNode, or if this is a leaf node, a data object
   * (wrapped in an ItemBoundable)
   */
  public void addChild(final Boundable<B, I> child) {
    this.children.add(child);
  }

  /**
   * @param level -1 to get items
   */
  @Override
  public void boundablesAtLevel(final int level, final Collection<Boundable<B, I>> boundables) {
    if (getLevel() == level) {
      boundables.add(this);
    } else {
      for (final Boundable<B, I> boundable : this) {
        boundable.boundablesAtLevel(level, boundables);
      }
    }
  }

  /**
   * Returns a representation of space that encloses this Boundable,
   * preferably not much bigger than this Boundable's boundary yet fast to
   * test for intersection with the bounds of other Boundables. The class of
   * object returned depends on the subclass of AbstractSTRtree.
   *
   * @return an BoundingBox (for STRtrees), an Interval (for SIRtrees), or other
   *         object (for other subclasses of AbstractSTRtree)
   * @see AbstractSTRtree.IntersectsOp
   */
  protected abstract B computeBounds();

  /**
   * Gets the bounds of this node
   *
   * @return the object representing bounds in this index
   */
  @Override
  public B getBounds() {
    if (this.bounds == null) {
      this.bounds = computeBounds();
    }
    return this.bounds;
  }

  /**
   * Gets the count of the {@link Boundable}s at this node.
   *
   * @return the count of boundables at this node
   */
  @Override
  public int getChildCount() {
    return this.children.size();
  }

  /**
   * Returns either child {@link AbstractNode}s, or if this is a leaf node, real data (wrapped
   * in {@link ItemBoundable}s).
   */
  @Override
  public List<Boundable<B, I>> getChildren() {
    return this.children;
  }

  @Override
  public int getDepth() {
    int maxChildDepth = 0;
    for (final Boundable<B, I> childBoundable : this) {
      final int childDepth = childBoundable.getDepth();
      if (childDepth > maxChildDepth) {
        maxChildDepth = childDepth;
      }
    }
    return maxChildDepth + 1;
  }

  @Override
  public int getItemCount() {
    int itemCount = 0;
    for (final Boundable<B, I> childBoundable : this) {
      itemCount += childBoundable.getItemCount();
    }
    return itemCount;
  }

  /**
   * Returns 0 if this node is a leaf, 1 if a parent of a leaf, and so on; the
   * root node will have the highest level
   */
  public int getLevel() {
    return this.level;
  }

  /**
   * Tests whether there are any {@link Boundable}s at this node.
   *
   * @return true if there are boundables at this node
   */
  @Override
  public boolean isEmpty() {
    return this.children.isEmpty();
  }

  @Override
  public boolean isNode() {
    return true;
  }

  @Override
  public Iterator<Boundable<B, I>> iterator() {
    return this.children.iterator();
  }

  @Override
  public void query(final AbstractSTRtree<B, ?, ?> tree, final B searchBounds,
    final Consumer<? super I> action) {
    if (tree.intersects(getBounds(), searchBounds)) {
      for (final Boundable<B, I> child : this) {
        child.query(tree, searchBounds, action);
      }
    }
  }

  @Override
  public boolean remove(final AbstractSTRtree<B, ?, ?> tree, final B searchBounds, final I item) {
    // first try removing item from this node
    if (removeItem(item)) {
      return true;
    } else {
      for (final Iterator<Boundable<B, I>> iterator = this.children.iterator(); iterator
        .hasNext();) {
        final Boundable<B, I> child = iterator.next();
        if (child.isNode()) {
          if (tree.intersects(child.getBounds(), searchBounds)) {
            if (child.remove(tree, searchBounds, item)) {
              if (child.isEmpty()) {
                iterator.remove();
              }
              return true;
            }
          }
        }
      }
      return false;
    }
  }

  private boolean removeItem(final I item) {
    for (final Iterator<Boundable<B, I>> iterator = this.children.iterator(); iterator.hasNext();) {
      final Boundable<B, I> child = iterator.next();
      if (child.getItem() == item) {
        iterator.remove();
        return true;
      }
    }
    return false;
  }
}
