package com.revolsys.gis.model.geometry.operation.relate;

import java.util.Iterator;
import java.util.List;

import com.revolsys.gis.model.geometry.operation.geomgraph.Edge;
import com.revolsys.gis.model.geometry.operation.geomgraph.EdgeEnd;
import com.revolsys.gis.model.geometry.operation.geomgraph.EdgeEndBuilder;
import com.revolsys.gis.model.geometry.operation.geomgraph.EdgeIntersection;
import com.revolsys.gis.model.geometry.operation.geomgraph.GeometryGraph;
import com.revolsys.gis.model.geometry.operation.geomgraph.Node;
import com.revolsys.gis.model.geometry.operation.geomgraph.NodeMap;
import com.vividsolutions.jts.geom.Location;

/**
 * @version 1.7
 */

/**
 * Implements the simple graph of Nodes and EdgeEnd which is all that is
 * required to determine topological relationships between Geometries.
 * Also supports building a topological graph of a single Geometry, to
 * allow verification of valid topology.
 * <p>
 * It is <b>not</b> necessary to create a fully linked
 * PlanarGraph to determine relationships, since it is sufficient
 * to know how the Geometries interact locally around the nodes.
 * In fact, this is not even feasible, since it is not possible to compute
 * exact intersection points, and hence the topology around those nodes
 * cannot be computed robustly.
 * The only Nodes that are created are for improper intersections;
 * that is, nodes which occur at existing vertices of the Geometries.
 * Proper intersections (e.g. ones which occur between the interior of line segments)
 * have their topology determined implicitly, without creating a Node object
 * to represent them.
 *
 * @version 1.7
 */
public class RelateNodeGraph {

  private final NodeMap nodes = new NodeMap(new RelateNodeFactory());

  public RelateNodeGraph() {
  }

  public void build(final GeometryGraph geomGraph) {
    // compute nodes for intersections between previously noded edges
    computeIntersectionNodes(geomGraph, 0);
    /**
     * Copy the labelling for the nodes in the parent Geometry.  These override
     * any labels determined by intersections.
     */
    copyNodesAndLabels(geomGraph, 0);

    /**
     * Build EdgeEnds for all intersections.
     */
    final EdgeEndBuilder eeBuilder = new EdgeEndBuilder();
    final List eeList = eeBuilder.computeEdgeEnds(geomGraph.getEdgeIterator());
    insertEdgeEnds(eeList);

    // Debug.println("==== NodeList ===");
    // Debug.print(nodes);
  }

  /**
   * Insert nodes for all intersections on the edges of a Geometry.
   * Label the created nodes the same as the edge label if they do not already have a label.
   * This allows nodes created by either self-intersections or
   * mutual intersections to be labelled.
   * Endpoint nodes will already be labelled from when they were inserted.
   * <p>
   * Precondition: edge intersections have been computed.
   */
  public void computeIntersectionNodes(final GeometryGraph geomGraph,
    final int argIndex) {
    for (final Iterator edgeIt = geomGraph.getEdgeIterator(); edgeIt.hasNext();) {
      final Edge e = (Edge)edgeIt.next();
      final int eLoc = e.getLabel().getLocation(argIndex);
      for (final Iterator eiIt = e.getEdgeIntersectionList().iterator(); eiIt.hasNext();) {
        final EdgeIntersection ei = (EdgeIntersection)eiIt.next();
        final RelateNode n = (RelateNode)nodes.addNode(ei.coord);
        if (eLoc == Location.BOUNDARY) {
          n.setLabelBoundary(argIndex);
        } else {
          if (n.getLabel().isNull(argIndex)) {
            n.setLabel(argIndex, Location.INTERIOR);
          }
        }
        // Debug.println(n);
      }
    }
  }

  /**
   * Copy all nodes from an arg geometry into this graph.
   * The node label in the arg geometry overrides any previously computed
   * label for that argIndex.
   * (E.g. a node may be an intersection node with
   * a computed label of BOUNDARY,
   * but in the original arg Geometry it is actually
   * in the interior due to the Boundary Determination Rule)
   */
  public void copyNodesAndLabels(final GeometryGraph geomGraph,
    final int argIndex) {
    for (final Iterator nodeIt = geomGraph.getNodeIterator(); nodeIt.hasNext();) {
      final Node graphNode = (Node)nodeIt.next();
      final Node newNode = nodes.addNode(graphNode.getCoordinate());
      newNode.setLabel(argIndex, graphNode.getLabel().getLocation(argIndex));
      // node.print(System.out);
    }
  }

  public Iterator getNodeIterator() {
    return nodes.iterator();
  }

  public void insertEdgeEnds(final List ee) {
    for (final Iterator i = ee.iterator(); i.hasNext();) {
      final EdgeEnd e = (EdgeEnd)i.next();
      nodes.add(e);
    }
  }

}