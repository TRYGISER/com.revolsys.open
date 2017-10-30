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
package com.revolsys.geometry.test.old.index;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.revolsys.geometry.index.strtree.AbstractNode;
import com.revolsys.geometry.index.strtree.Boundable;
import com.revolsys.geometry.index.strtree.ItemBoundable;
import com.revolsys.geometry.index.strtree.STRtree;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.Geometry;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.Point;
import com.revolsys.geometry.model.impl.BoundingBoxDoubleXY;
import com.revolsys.geometry.model.impl.PointDoubleXY;
import com.revolsys.geometry.test.old.util.SerializationUtil;

import junit.framework.TestCase;

/**
 * @version 1.7
 */
public class STRtreeTest extends TestCase {
  public static void main(final String[] args) {
    final String[] testCaseName = {
      STRtreeTest.class.getName()
    };
    junit.textui.TestRunner.main(testCaseName);
  }

  private final GeometryFactory factory = GeometryFactory.DEFAULT_3D;

  public STRtreeTest(final String Name_) {
    super(Name_);
  }

  private void doTestCreateParentsFromVerticalSlice(final int childCount, final int nodeCapacity,
    final int expectedChildrenPerParentBoundable, final int expectedChildrenOfLastParent) {
    final STRtreeDemo.TestTree t = new STRtreeDemo.TestTree(nodeCapacity);
    final List parentBoundables = t.newParentBoundablesFromVerticalSlice(itemWrappers(childCount),
      0);
    for (int i = 0; i < parentBoundables.size() - 1; i++) {// -1
      final AbstractNode parentBoundable = (AbstractNode)parentBoundables.get(i);
      assertEquals(expectedChildrenPerParentBoundable, parentBoundable.getChildCount());
    }
    final AbstractNode lastParent = (AbstractNode)parentBoundables.get(parentBoundables.size() - 1);
    assertEquals(expectedChildrenOfLastParent, lastParent.getChildCount());
  }

  private void doTestVerticalSlices(final int itemCount, final int sliceCount,
    final int expectedBoundablesPerSlice, final int expectedBoundablesOnLastSlice) {
    final STRtreeDemo.TestTree t = new STRtreeDemo.TestTree(2);
    final List<List<Boundable<BoundingBox, Object>>> slices = t
      .verticalSlices(itemWrappers(itemCount), sliceCount);
    assertEquals(sliceCount, slices.size());
    for (int i = 0; i < sliceCount - 1; i++) {// -1
      assertEquals(expectedBoundablesPerSlice, slices.get(i).size());
    }
    assertEquals(expectedBoundablesOnLastSlice, slices.get(sliceCount - 1).size());
  }

  private List itemWrappers(final int size) {
    final ArrayList itemWrappers = new ArrayList();
    for (int i = 0; i < size; i++) {
      itemWrappers.add(new ItemBoundable(new BoundingBoxDoubleXY(0, 0, 0, 0), new Object()));
    }
    return itemWrappers;
  }

  public void testCreateParentsFromVerticalSlice() {
    doTestCreateParentsFromVerticalSlice(3, 2, 2, 1);
    doTestCreateParentsFromVerticalSlice(4, 2, 2, 2);
    doTestCreateParentsFromVerticalSlice(5, 2, 2, 1);
  }

  public void testDisallowedInserts() {
    final STRtree t = new STRtree(5);
    t.insertItem(new BoundingBoxDoubleXY(0, 0, 0, 0), new Object());
    t.insertItem(new BoundingBoxDoubleXY(0, 0, 0, 0), new Object());
    t.getItems(BoundingBox.empty());
    try {
      t.insertItem(new BoundingBoxDoubleXY(0, 0, 0, 0), new Object());
      assertTrue(false);
    } catch (final AssertionError e) {
      assertTrue(true);
    }
  }

  public void testEmptyTreeUsingItemVisitorQuery() {
    final STRtree tree = new STRtree();
    tree.query(new BoundingBoxDoubleXY(0, 1, 0, 1), (item) -> {
      assertTrue("Should never reach here", true);
    });
  }

  public void testEmptyTreeUsingListQuery() {
    final STRtree tree = new STRtree();
    final List list = tree.getItems(new BoundingBoxDoubleXY(0, 1, 0, 1));
    assertTrue(list.isEmpty());
  }

  public void testQuery() throws Throwable {
    final ArrayList geometries = new ArrayList();
    geometries.add(this.factory.lineString(new Point[] {
      new PointDoubleXY(0, 0), new PointDoubleXY(10, 10)
    }));
    geometries.add(this.factory.lineString(new Point[] {
      new PointDoubleXY(20, 20), new PointDoubleXY(30, 30)
    }));
    geometries.add(this.factory.lineString(new Point[] {
      new PointDoubleXY(20, 20), new PointDoubleXY(30, 30)
    }));
    final STRtreeDemo.TestTree t = new STRtreeDemo.TestTree(4);
    for (final Iterator i = geometries.iterator(); i.hasNext();) {
      final Geometry g = (Geometry)i.next();
      t.insertItem(g.getBoundingBox(), new Object());
    }
    t.build();
    try {
      assertEquals(1, t.getItems(new BoundingBoxDoubleXY(5, 5, 6, 6)).size());
      assertEquals(0, t.getItems(new BoundingBoxDoubleXY(20, 0, 30, 10)).size());
      assertEquals(2, t.getItems(new BoundingBoxDoubleXY(25, 25, 26, 26)).size());
      assertEquals(3, t.getItems(new BoundingBoxDoubleXY(0, 0, 100, 100)).size());
    } catch (final Throwable x) {
      STRtreeDemo.printSourceData(geometries, System.out);
      STRtreeDemo.printLevels(t, System.out);
      throw x;
    }
  }

  public void testSerialization() throws Exception {
    final SpatialIndexTester tester = new SpatialIndexTester();
    tester.setSpatialIndex(new STRtree(4));
    tester.init();

    STRtree tree = (STRtree)tester.getSpatialIndex();
    // create the index before serialization
    tree.getItems(BoundingBox.empty());

    final byte[] data = SerializationUtil.serialize(tree);
    tree = (STRtree)SerializationUtil.deserialize(data);

    tester.setSpatialIndex(tree);
    tester.run();
    assertTrue(tester.isSuccess());
  }

  public void testSpatialIndex() throws Exception {
    final SpatialIndexTester tester = new SpatialIndexTester();
    tester.setSpatialIndex(new STRtree(4));
    tester.init();
    tester.run();
    assertTrue(tester.isSuccess());
  }

  public void testVerticalSlices() {
    doTestVerticalSlices(3, 2, 2, 1);
    doTestVerticalSlices(4, 2, 2, 2);
    doTestVerticalSlices(5, 3, 2, 1);
  }

}
