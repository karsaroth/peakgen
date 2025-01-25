package peakgen.generate.topology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a node in a Terrain Generation graph. It wraps an LstCoordinate that contains static
 * and relative data about the node, such as its location, sea factor, uplift factor, and slope
 * and tracks additional data such as height, catchment area, lake ID, and other key values, both
 * specific to a generation instance and calculated during the generation process. The node also
 * maintains a list of outbound LstDirectedEdges, which represent connections to other nodes in the
 * graph. The node also maintains a list of inbound nodes, which represent connections from other
 * nodes in the graph.
 */
public final class LstNode implements Cloneable {

  public final double uplift;
  public final double maxSlope;

  /**
   * Nodes with an inbound connection to this Node
   */
  final List<LstNode> inboundNodes = new ArrayList<>();

  /**
   * The collection of DirectedEdges that leave this Node
   */
  final List<LstDirectedEdge> outEdges = new ArrayList<>();


  double height = 0.0d;
  double catchmentArea = 0.0d;
  double upstreamCatchmentArea = 0.0d;
  double seaNodeDistance = Double.MAX_VALUE;
  double lakeHeight = Double.MIN_VALUE;
  long lakeId = -1L;
  /**
   * The location of this Node
   */
  private LstCoordinate pt;

  /**
   * Constructs a Node with the given location, uplift and max slope. Both uplift and max slope are
   * ignored if the Node is a sea node.
   *
   * @param pt       the location of the Node
   * @param uplift   the uplift of the Node
   * @param maxSlope the maximum slope of the Node
   */
  public LstNode(LstCoordinate pt, double uplift, double maxSlope) {
    this.pt = pt;
    this.uplift = pt.isSea() ? 0.0 : uplift;
    this.maxSlope = pt.isSea() ? 0.0 : maxSlope;
  }

  /**
   * Constructs a Node with location, uplift, height, catchment area, lake ID and max slope. All the
   * values beside lake ID are ignored if the Node is a sea node.
   *
   * @param pt            the location of the Node
   * @param uplift        the uplift of the Node
   * @param height        the height of the Node
   * @param catchmentArea the catchment area of the Node
   * @param lakeId        the lake ID of the Node
   * @param maxSlope      the maximum slope of the Node
   */
  public LstNode(
      LstCoordinate pt, double uplift, double height, double catchmentArea, long lakeId, double maxSlope
  ) {
    this.pt = pt;
    this.height = pt.isSea() ? 0.0 : height;
    this.uplift = pt.isSea() ? 0.0 : uplift;
    this.catchmentArea = pt.isSea() ? 0.0 : catchmentArea;
    this.lakeId = lakeId;
    this.maxSlope = pt.isSea() ? 0.0 : maxSlope;
  }

  /**
   * Used primarily to clone a node by copying all the values from another node. In this
   * constructor, all values are set without any checks.
   *
   * @param pt                    the location of the Node
   * @param uplift                the uplift of the Node
   * @param height                the height of the Node
   * @param catchmentArea         the catchment area of the Node
   * @param lakeId                the lake ID of the Node
   * @param upstreamCatchmentArea the upstream catchment area of the Node
   * @param maxSlope              the maximum slope of the Node
   * @param lakeHeight            the lake height of the Node
   * @param seaNodeDistance       the sea node distance of the Node
   */
  public LstNode(
      LstCoordinate pt,
      double uplift,
      double height,
      double catchmentArea,
      long lakeId,
      double upstreamCatchmentArea,
      double maxSlope,
      double lakeHeight,
      double seaNodeDistance
  ) {
    this.pt = pt;
    this.height = height;
    this.uplift = uplift;
    this.catchmentArea = catchmentArea;
    this.upstreamCatchmentArea = upstreamCatchmentArea;
    this.lakeId = lakeId;
    this.maxSlope = maxSlope;
    this.lakeHeight = lakeHeight;
    this.seaNodeDistance = seaNodeDistance;
  }

  /**
   * Provides the corresponding location of this node as defined by its coordinate. The coordinate
   * class groups the x and y values of the node, as well as encapsulating some other terrain
   * generation specific values. To aid in some calculations, the coordinate only represents two
   * dimensions.
   *
   * @return the location of this node as provided during construction.
   */
  public LstCoordinate coordinate() {
    return pt;
  }

  /**
   * A shorter form of {@link #coordinate()} that provides the location of this node.
   *
   * @return the location of this node
   */
  public LstCoordinate pt() {
    return pt;
  }

  /**
   * Unlike the underlying coordinate, the node holds the maximum slope as calculated based on
   * geology settings, rather than a relative value. The value stored here is measured in radians,
   * and should be between 0 and PI/2.
   *
   * @return the maximum slope
   */
  public double maxSlope() {
    return maxSlope;
  }

  /**
   * Provides the pre-calculated catchment area of this node, assumed to be in square meters. This
   * value only represents the catchment area of this specific node, and ignores any upstream nodes.
   *
   * @return the catchment area of this node in square meters
   */
  public double catchmentArea() {
    return catchmentArea;
  }

  /**
   * Provides the pre-calculated catchment area of this node, and all upstream nodes, assumed to be
   * in square meters. This value represents the total catchment area of this node and is useful for
   * some calculations.
   *
   * @return the total catchment area of this node and all upstream nodes in square meters
   */
  public double totalCatchmentArea() {
    return upstreamCatchmentArea + catchmentArea;
  }

  /**
   * Provides the precalculated catchment area of only the upstream nodes of this node, assumed to
   * be in square meters.
   *
   * @return the catchment area of upstream nodes
   */
  public double upstreamCatchmentArea() {
    return upstreamCatchmentArea;
  }

  /**
   * Sets the precalculated catchment area of only the upstream nodes of this node, assumed to be in
   * square meters.
   *
   * @param upstreamCatchmentArea the catchment area of all upstream nodes to this node
   */
  public void setUpstreamCatchmentArea(double upstreamCatchmentArea) {
    this.upstreamCatchmentArea = upstreamCatchmentArea;
  }

  /**
   * Provides a precalculated distance to the nearest sea node, assumed to be in meters. This value
   * is usually set by summing distances between each node and the nearest sea node, rather than via
   * a direct calculation.
   *
   * @return the distance to the nearest sea node via connected nodes, in meters
   */
  public double seaNodeDistance() {
    return seaNodeDistance;
  }

  /**
   * Sets the distance to the nearest sea node assumed to be in meters. This value is usually set by
   * summing distances between each node and the nearest sea node, rather than via a direct
   * calculation.
   *
   * @param seaNodeDistance the distance to the nearest sea node
   */
  public void setSeaNodeDistance(double seaNodeDistance) {
    this.seaNodeDistance = seaNodeDistance;
  }

  /**
   * Provides the height of this node, assumed to be in meters. This is used rather than a Z
   * coordinate to save on certain calculations, allowing 2-dimensional calculations to be used in
   * some cases.
   *
   * @return the height of this node in meters
   */
  public double height() {
    return height;
  }

  /**
   * Sets the height of this node, assumed to be in meters. This is used rather than a Z coordinate
   * to save on certain calculations, allowing 2-dimensional calculations to be used in some cases.
   *
   * @param height the height of this node in meters
   */
  public void setHeight(double height) {
    this.height = pt.isSea() ? 0.0d : height;
  }

  /**
   * Unlike the underlying coordinate, provides the uplift of this node in meters per year, rather
   * than a relative value. Nodes will be moved upwards by this value each year during generation,
   * although they may also be moved downwards due to erosion, or maximum slope constraints.
   *
   * @return the uplift of this node in meters per year
   */
  public double uplift() {
    return uplift;
  }

  /**
   * This value can be set to aid in grouping nodes into lakes (i.e. nodes that drain into either a
   * sink node, or a sea node). If it is not set, the value will be -1.
   *
   * @return the lake ID as set externally.
   */
  public long lakeId() {
    return lakeId;
  }

  /**
   * This value can be set to aid in grouping nodes into lakes (i.e. nodes that drain into either a
   * sink node, or a sea node).
   *
   * @param lakeId the lake ID
   */
  public void setLakeId(long lakeId) {
    this.lakeId = lakeId;
  }

  /**
   * Will set the height of this node to 0, and set the node to be a sea node. This is essentially
   * overriding the LstSettings originally calculated for this node, and is only used normally when
   * no sea nodes exist in a graph (and one is needed).
   */
  public void switchToSea() {
    this.height = 0.0d;
    this.pt = new LstCoordinate(pt.x, pt.y, 0.0d, 0.0d, 0.0d);
  }

  /**
   * Returns a collection of all outbound connections from this node in the corresponding graph.
   *
   * @return the internal list of outbound edges, which is sorted by angle with the positive x-axis. Modifying this
   * list will modify the node's connections.
   */
  public List<LstDirectedEdge> outEdges() {
    return outEdges;
  }

  /**
   * Adds an edge to this Node's outbound edges, and sorts the edges by angle with the positive x-axis. This does
   * not impact the inbound nodes values.
   *
   * @param de the edge to add to this Node's outbound edges
   */
  public void addOutEdge(LstDirectedEdge de) {
    outEdges.add(de);
    Collections.sort(outEdges);
  }

  /**
   * To aid in navigating a graph in reverse, add a node that is known to link to this node
   * via an inbound edge.
   *
   * @param node the node to add to the inbound nodes list.
   */
  public void addInboundNode(LstNode node) {
    inboundNodes.add(node);
  }

  /**
   * If a connection is removed from the graph resulting in an inbound connection between this
   * node and another node being severed, this method can be used to remove the
   * node from the list of inbound nodes for consistency when navigating the graph in reverse.
   *
   * @param node the node to remove
   */
  public void removeInboundNode(LstNode node) {
    inboundNodes.remove(node);
  }

  /**
   * To aid in navigating a graph in reverse, graphs will maintain a list of nodes that link to this
   * node via an inbound edge. This method returns the list of those nodes.
   *
   * @return the list of nodes connecting to this node via an inbound edge, as maintained by the node's
   * corresponding graph.
   */
  public List<LstNode> inboundNodes() {
    return inboundNodes;
  }

  /**
   * Drops an outbound edge from this node and re-sorts the remainder. The remove function is not
   * called on the edge, and the "to" node's inbound list is not updated, as this might be
   * an unwanted side effect. The graph should take these steps if desired.
   */
  public void remove(LstDirectedEdge de) {
    outEdges.remove(de);
    Collections.sort(outEdges);
  }

  /**
   * Returns the two-dimensional location of this node as a LstCoordinate. Binding multiple nodes
   * to the same coordinates can help in some calculations
   *
   * @return the LstCoordinate representing the location of this node as provided during
   * construction.
   */
  public LstCoordinate getCoordinate() {
    return pt;
  }

  /**
   * Cleans up references in preparation for garbage collection, removing all outbound edges and
   * inbound nodes from this node's tracking lists.
   */
  public void unbind() {
    this.inboundNodes.clear();
    this.outEdges.clear();
  }

  /**
   * A deep (complete) clone of this LstNode, including all values. The coordinate is cloned as
   * well. Since a new node is created, the node will not be connected to any other nodes via
   * DirectedEdges.
   *
   * @return a deep clone of this LstNode.
   */
  public LstNode deepClone() {
    return new LstNode(
        pt.clone(),
        uplift,
        maxSlope,
        catchmentArea,
        lakeId,
        upstreamCatchmentArea,
        maxSlope,
        lakeHeight,
        seaNodeDistance
    );
  }

  @Override public int hashCode() {
    return pt.hashCode();
  }

  @Override public boolean equals(Object other) {
    if (other instanceof LstNode lstNode) {
      return pt.equals(lstNode.pt);
    }
    return false;
  }

  @Override public LstNode clone() {
    return new LstNode(pt, uplift, height, catchmentArea, lakeId, maxSlope);
  }
}
