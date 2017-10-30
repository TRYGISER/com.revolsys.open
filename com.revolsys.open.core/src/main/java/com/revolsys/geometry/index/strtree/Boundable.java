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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * A spatial object in an AbstractSTRtree.
 *
 * @version 1.7
 */
public interface Boundable<B, I> extends Iterable<Boundable<B, I>> {
  void boundablesAtLevel(int level, Collection<Boundable<B, I>> boundables);

  /**
   * Returns a representation of space that encloses this Boundable, preferably
   * not much bigger than this Boundable's boundary yet fast to test for intersection
   * with the bounds of other Boundables. The class of object returned depends
   * on the subclass of AbstractSTRtree.
   * @return an BoundingBox (for STRtrees), an Interval (for SIRtrees), or other object
   * (for other subclasses of AbstractSTRtree)
   */
  B getBounds();

  default int getChildCount() {
    return 0;
  }

  default List<Boundable<B, I>> getChildren() {
    return Collections.emptyList();
  }

  default int getDepth() {
    return 0;
  }

  default I getItem() {
    return null;
  }

  default int getItemCount() {
    return 1;
  }

  default boolean isEmpty() {
    return false;
  }

  default boolean isNode() {
    return false;
  }

  @Override
  default Iterator<Boundable<B, I>> iterator() {
    return Collections.emptyIterator();
  }

  void query(AbstractSTRtree<B, ?, ?> tree, final B searchBounds, final Consumer<? super I> action);

  default boolean remove(final AbstractSTRtree<B, ?, ?> tree, final B searchBounds, final I item) {
    return false;
  }
}
