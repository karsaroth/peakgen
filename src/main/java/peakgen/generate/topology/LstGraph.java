package peakgen.generate.topology;

import javax.vecmath.Point2d;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A graph that can be used to represent different structures for terrain generation. This graph
 * contains a set of nodes and directed edges. The nodes are connected by the directed edges, which
 * are directed from one node to another. The graph can be used to represent both the structure of
 * the terrain and the flow of water across the terrain via streams or lakes.
 */
public final class LstGraph {
  public static final String MAX_HEIGHT = "MaxHeight";
  final Set<LstNode> sinks = Collections.newSetFromMap(new ConcurrentHashMap<>());
  final Set<LstDirectedEdge> directedEdges = Collections.newSetFromMap(new ConcurrentHashMap<>());
  final Map<LstCoordinate, LstNode> invertedNodeMap = new ConcurrentHashMap<>();
  final Map<Point2d, LstCoordinate> pointToCoordinate = new ConcurrentHashMap<>();
  final BiFunction<LstNode, LstNode, LstDirectedEdge> edgeFactory;
  final Function<LstCoordinate, LstNode> nodeFactory;

  /**
   * Constructs an empty graph, and sets the default factories for creating nodes and edges. The
   * default factories will create new instances of LstNode and LstDirectedEdges as required, but
   * can be overridden by some of the add methods.
   *
   * @param edgeFactory The constructor to create an edge if needed
   * @param nodeFactory The constructor to create a node if needed
   */
  public LstGraph(
      BiFunction<LstNode, LstNode, LstDirectedEdge> edgeFactory, Function<LstCoordinate, LstNode> nodeFactory
  ) {
    this.edgeFactory = edgeFactory;
    this.nodeFactory = nodeFactory;
  }

  /**
   * Returns a set of nodes in this graph that have no outgoing edges. Nodes will always start in
   * this list when they are added to the graph, and will be removed when they are connected to
   * other nodes using the add methods on this class.
   *
   * @return the set of Nodes in this graph that have no outgoing edges
   */
  public Set<LstNode> sinks() {
    return sinks;
  }

  /**
   * Returns the LstNode at the given location, or null if not found. Important: these coordinates
   * should have been sourced from the same data structures as the original coordinates in the graph,
   * otherwise double precision errors may cause the lookup to fail (i.e. don't use calculated values
   * for this method).
   *
   * @param point The point, containing coordinates previously used to create a node
   * @return the node found, or null if this graph contains no node at the location
   */
  public LstNode findNodeByPosition(Point2d point) {
    var coordinate = pointToCoordinate.get(point);
    if (coordinate == null) {
      return null;
    } else {
      return findNodeF(coordinate);
    }
  }

  /**
   * Returns the LstNode at the given location, or null if not found (a faster version of findNode
   * avoiding Optional wrapping, which is sometimes more convenient). The find will be based on the
   * 2D location of the node, i.e. the underlying Coordinate.
   *
   * @param pt the location to query as a Coordinate
   * @return the node found, or null if this graph contains no node at the location
   */
  public LstNode findNodeF(LstCoordinate pt) {
    return invertedNodeMap.get(pt);
  }

  /**
   * Adds a new {@link LstDirectedEdge} between the coordinates <code>from</code> and <code>to
   * </code>. The default factories will be used to create the nodes and edges if they do not
   * already exist.
   *
   * @param from The start coordinate, or where the edge is coming from
   * @param to   The end coordinate, or where the edge is going to
   * @return The edge between the two coordinates, either new or existing
   */
  public LstDirectedEdge add(LstCoordinate from, LstCoordinate to) {
    return add(from, to, nodeFactory, edgeFactory);
  }

  /**
   * Adds a new {@link LstDirectedEdge} between the coordinates <code>from</code> and <code>to
   * </code>. If nodes or edge do not already exist in the graph, the provided factories will be
   * used to create them.
   *
   * @param from The start coordinate, or where the edge is coming from
   * @param to   The end coordinate, or where the edge is going to
   * @return The edge between the two coordinates, either new or existing
   */
  public LstDirectedEdge add(
      LstCoordinate from,
      LstCoordinate to,
      Function<LstCoordinate, LstNode> altNodeFactory,
      BiFunction<LstNode, LstNode, LstDirectedEdge> altEdgeFactory
  ) {
    var fromNode = add(from, altNodeFactory);
    var toNode = add(to, altNodeFactory);
    return _add(fromNode, toNode, altEdgeFactory);
  }

  /**
   * Adds a new {@link LstNode} at the given location. If a node already exists at that location, it
   * will be returned. Otherwise, a new node will be created and added to the graph using the
   * provided factory.
   *
   * @param pt The location to add the node as an LstCoordinate
   * @return The node at the location, either new or existing
   */
  public LstNode add(LstCoordinate pt, Function<LstCoordinate, LstNode> nodeFactory) {
    if (invertedNodeMap.containsKey(pt)) {
      return invertedNodeMap.get(pt);
    }
    var newNode = nodeFactory.apply(pt);
    _add(newNode);
    sinks.add(newNode);
    return newNode;
  }

  /**
   * Adds a new {@link LstDirectedEdge} between the coordinates <code>from</code> and <code>to
   * </code>. This version of the method assumes that the nodes already exist in the graph, but
   * ensures that sym values are set on any existing or new edges.
   *
   * @param from The start Node
   * @param to   The end Node
   * @return The edge between the two coordinates
   */
  LstDirectedEdge _add(
      LstNode from, LstNode to, BiFunction<LstNode, LstNode, LstDirectedEdge> altEdgeFactory
  ) {
    return findEdge(from.getCoordinate(), to.getCoordinate()).orElseGet(() -> {
      var newEdge = altEdgeFactory.apply(from, to);
      _add(newEdge);
      findEdge(to.getCoordinate(), from.getCoordinate()).ifPresent((s) -> {
        newEdge.setSym(s);
        s.setSym(newEdge);
      });
      sinks.remove(from);
      return newEdge;
    });
  }

  /**
   * Adds a node to the underlying node map only, replacing any that is already at that location,
   * ideally this method won't be called unless you are sure the node doesn't already exist, the set
   * of sinks is not updated by this method.
   *
   * @param node the node to add to the graph.
   */
  void _add(LstNode node) {
    invertedNodeMap.put(node.getCoordinate(), node);
    pointToCoordinate.put(node.getCoordinate().point, node.getCoordinate());
  }

  /**
   * Finds the edge between the two coordinates, if it exists, and returns the result wrapped in an
   * Optional. This method is based on the 2D location of the nodes, i.e. the underlying Coordinate.
   *
   * @param a The start coordinate of the edge
   * @param b The end coordinate of the edge
   * @return The edge between the two coordinates, if it exists, or an empty Optional if it does
   * not.
   */
  public Optional<LstDirectedEdge> findEdge(LstCoordinate a, LstCoordinate b) {
    return findNode(a).map(nodeA -> {
      var nodeAOutEdges = nodeA.outEdges();
      for (LstDirectedEdge edge : nodeAOutEdges) {
        if (edge
            .getToNode()
            .getCoordinate()
            .equals(b)) {
          return edge;
        }
      }
      return null;
    });
  }

  /**
   * Adds the Edge to this Graph. Since this is a set-based structure, this method will not add the
   * edge if it is already present. Ideally however, this method should not be called unless you are
   * sure the edge doesn't already exist. Values like sym and node connections are not modified by this
   * method.
   */
  void _add(LstDirectedEdge dirEdge) {
    directedEdges.add(dirEdge);
  }

  /**
   * Returns the Node at the given location, or an empty Optional if not found. This method is based
   * on the 2D location of the nodes, i.e. the underlying Coordinate.
   *
   * @param pt the location to query as an LstCoordinate
   * @return the node found or an empty Optional if this graph contains no node at the location
   */
  public Optional<LstNode> findNode(LstCoordinate pt) {
    return Optional.ofNullable(invertedNodeMap.get(pt));
  }

  /**
   * Adds two new {@link LstDirectedEdge} between the coordinates <code>from</code> and <code>to
   * </code>, and between <code>to</code> and <code>from</code>. Additionally, the saddle nodes
   * provided are cloned and set on the edges as well. The provided factories will be used to create
   * the nodes and edges if they do not already exist (excluding the cloned saddle nodes). <br>
   * The point of this method is to allow creating a graph that corresponds to another graph, and in
   * this library is used to link lakes together while maintaining which saddle each link
   * corresponds to. Once it has been determined that two lakes will connect, the saddle nodes can
   * be retrieved from this graph and added into a stream tree to link the lakes together if
   * desired.
   *
   * @param sinkA The start coordinate
   * @param sinkB The end coordinate
   */
  public void addBiDirectionalWithSaddles(
      LstCoordinate sinkA,
      LstCoordinate sinkB,
      LstNode saddleNodeA,
      LstNode saddleNodeB,
      Function<LstCoordinate, LstNode> altNodeFactory,
      BiFunction<LstNode, LstNode, LstDirectedEdge> altEdgeFactory
  ) {
    var sinkNodeA = add(sinkA, altNodeFactory);
    var sinkNodeB = add(sinkB, altNodeFactory);
    var lakeGraphEdgeAB = _add(sinkNodeA, sinkNodeB, altEdgeFactory);
    var lakeGraphEdgeBA = _add(sinkNodeB, sinkNodeA, altEdgeFactory);

    lakeGraphEdgeAB.setSaddleNodeFrom(saddleNodeA.clone());
    lakeGraphEdgeAB.setSaddleNodeTo(saddleNodeB.clone());
    lakeGraphEdgeBA.setSaddleNodeFrom(saddleNodeB.clone());
    lakeGraphEdgeBA.setSaddleNodeTo(saddleNodeA.clone());
  }

  /**
   * Adds two new {@link LstDirectedEdge} between the coordinates <code>from</code> and <code>to
   * </code>, and between <code>to</code> and <code>from</code>. The default factories will be used
   * to create the nodes and edges.
   *
   * @param from The start coordinate
   * @param to   The end coordinate
   */
  public void addBiDirectional(LstCoordinate from, LstCoordinate to) {
    var fromNode = add(from);
    var toNode = add(to);
    _add(fromNode, toNode);
    _add(toNode, fromNode);
  }

  /**
   * Adds a new {@link LstNode} at the given location. If a node already exists at that location, it
   * will be returned. Otherwise, a new node will be created and added to the graph using the
   * default factory.
   *
   * @param pt The coordinate representing the 2d location of the node
   * @return The node at the location, either new or existing
   */
  public LstNode add(LstCoordinate pt) {
    return add(pt, nodeFactory);
  }

  /**
   * Adds a new {@link LstDirectedEdge} between the coordinates <code>from</code> and <code>to
   * </code>. This version of the method will not attempt to add the nodes to the graph.
   *
   * @param from The start Node
   * @param to   The end Node
   */
  void _add(LstNode from, LstNode to) {
    _add(from, to, edgeFactory);
  }

  /**
   * Returns a collection of nodes in this graph, based on the underlying map of nodes. The map
   * itself supports concurrency.
   *
   * @return the collection of nodes in this graph's node map
   */
  public Collection<LstNode> getNodes() {
    return invertedNodeMap.values();
  }

  /**
   * Returns the underlying collection of directed edges in this graph. The collection itself
   * supports concurrency.
   *
   * @return the collection of directed edges in this graph
   */
  public Collection<LstDirectedEdge> getEdges() {
    return directedEdges;
  }

  /**
   * Removes an LstDirectedEdge from its from-Node, from any sym, and from this graph. This method
   * does not remove the Nodes associated with the LstDirectedEdge, even if the removal of the
   * LstDirectedEdge reduces the degree of a Node to zero.
   */
  public void remove(LstDirectedEdge de) {
    de.unbind();
    directedEdges.remove(de);
    if (de
        .getFromNode()
        .outEdges()
        .isEmpty()) {
      sinks.add(de.getFromNode());
    }
  }

  /**
   * Removes a node from the graph, along with any associated LstDirectedEdges
   */
  public void remove(LstNode node) {
    // unhook all outbound directed edges
    List<LstDirectedEdge> outEdges = node.outEdges();
    for (LstDirectedEdge de : outEdges) {
      // remove the associated sym edge, if any
      var sym = de.getSym();
      de.unbind();
      directedEdges.remove(de);
      if (sym != null) {
        sym.unbind();
        directedEdges.remove(sym);
      }
    }
    // unhook all inbound directed edges
    for (var n : node.inboundNodes()) {
      for (LstDirectedEdge de : n.outEdges()) {
        if (de
            .getToNode()
            .equals(node)) {
          de.unbind();
          directedEdges.remove(de);
        }
      }
    }
    // remove the node from the graph
    invertedNodeMap.remove(node.getCoordinate());
  }

  /**
   * Removes all nodes and edges from this graph, calling remove on both nodes and edges. This is
   * essentially a reset
   */
  public void clear() {
    sinks.forEach(LstNode::unbind);
    sinks.clear();
    directedEdges.forEach(LstDirectedEdge::unbind);
    directedEdges.clear();
    invertedNodeMap.forEach((c, n) -> n.unbind());
    invertedNodeMap.clear();
  }
}
