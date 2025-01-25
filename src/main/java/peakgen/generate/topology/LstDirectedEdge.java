package peakgen.generate.topology;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a directed edge in a Terrain Generation graph. The edge is directed from the "from"
 * node to the "to" node. The edge also maintains a symmetric edge, which is directed in the
 * opposite direction. The symmetric edge is not required to be present. LSTDirectedEdge contains
 * further information relevant to terrain generation, such as the pass height, river radius, and lake saddle nodes,
 * it will also adjust references in the nodes it is connected to.
 */
public class LstDirectedEdge implements Comparable<LstDirectedEdge> {
  public static final Comparator<LstDirectedEdge> LAKE_COMPARATOR = (a, b) -> {
    // This comparator should order edges by increasing pass height, then by increasing uplift
    // of the two nodes, then by insert order.
    var passHeightComp = Double.compare(a.getPassHeight(), b.getPassHeight());
    if (passHeightComp != 0) {
      return passHeightComp;
    }
    var toUpliftComp = Double.compare(
        a
            .getToNode()
            .uplift(),
        b
            .getToNode()
            .uplift()
    );
    if (toUpliftComp != 0) {
      return toUpliftComp;
    }
    var fromUpliftComp = Double.compare(
        a
            .getFromNode()
            .uplift(),
        b
            .getFromNode()
            .uplift()
    );
    if (fromUpliftComp != 0) {
      return fromUpliftComp;
    }
    return Long.compare(a.insertOrder, b.insertOrder);
  };
  public final LstNode from;
  public final LstNode to;
  public final LstCoordinate p0, p1;
  protected final double dx, dy;
  protected final double angle;
  public LstNode saddleNodeFrom;
  protected long insertOrder;
  protected double passHeight;
  protected LstNode saddleNodeTo;
  protected LstDirectedEdge sym = null; // optional

  /**
   * Creates a new LstDirectedEdge from the "from" node to the "to" node. Both from and to nodes
   * will have reference lists adjusted.
   *
   * @param from the node the edge is directed from
   * @param to   the node the edge is directed to
   */
  public LstDirectedEdge(LstNode from, LstNode to) {
    this.from = from;
    this.to = to;
    p0 = from.getCoordinate();
    p1 = to.getCoordinate();
    dx = p1.x - p0.x;
    dy = p1.y - p0.y;
    angle = Math.atan2(dy, dx);
    from.addOutEdge(this);
    to.addInboundNode(from);
  }

  /**
   * Gets a corresponding saddle node "from" for this LstDirectedEdge, as found in another graph. A
   * saddle node is the lowest connecting node in one lake that connects to another lake.
   *
   * @return the saddle node "from" for this LstDirectedEdge, may be null
   */
  public LstNode getSaddleNodeFrom() {
    return saddleNodeFrom;
  }

  /**
   * Sets a corresponding saddle node "from" for this LstDirectedEdge, as found in another graph. A
   * saddle node is the lowest connecting node in one lake that connects to another lake.
   *
   * @param saddleNodeA the saddle node "from" for this LstDirectedEdge, may be null
   */
  public void setSaddleNodeFrom(LstNode saddleNodeA) {
    this.saddleNodeFrom = saddleNodeA;
  }

  /**
   * Gets a corresponding saddle node "to" for this LstDirectedEdge, as found in another graph. A
   * saddle node is the lowest connecting node in one lake that connects to another lake.
   *
   * @return the saddle node "to" for this LstDirectedEdge
   */
  public LstNode getSaddleNodeTo() {
    return saddleNodeTo;
  }

  /**
   * Sets a corresponding saddle node "to" for this LstDirectedEdge, as found in another graph. A
   * saddle node is the lowest connecting node in one lake that connects to another lake.
   *
   * @param saddleNodeB the saddle node "to" for this LstDirectedEdge
   */
  public void setSaddleNodeTo(LstNode saddleNodeB) {
    this.saddleNodeTo = saddleNodeB;
  }

  /**
   * The pass height is the height at which water can flow from one lake to another following a
   * saddle edge from the "saddleNodeFrom" to the "saddleNodeTo".
   *
   * @return the pass height of this LstDirectedEdge
   */
  public Double getPassHeight() {
    return passHeight;
  }

  /**
   * The pass height is the height at which water can flow from one lake to another following a
   * saddle edge from the "saddleNodeFrom" to the "saddleNodeTo".
   *
   * @param passHeight the pass height of this LstDirectedEdge
   */
  public void setPassHeight(Double passHeight) {
    this.passHeight = passHeight;
  }

  /**
   * Gets the order in which this LstDirectedEdge was inserted into the graph. As set by the graph
   * it was inserted into. This is used in some generation calculations but may not always be set.
   *
   * @return the order in which this LstDirectedEdge was inserted into the graph.
   */
  public long getInsertOrder() {
    return insertOrder;
  }

  /**
   * Sets the order in which this LstDirectedEdge was inserted into the graph. This can be used to
   * help with ordering in some generation calculations, but has no inherent meaning on the
   * LstDirectedEdge itself.
   *
   * @param insertOrder the order in which this LstDirectedEdge was inserted into the graph.
   */
  public void setInsertOrder(long insertOrder) {
    this.insertOrder = insertOrder;
  }

  /**
   * Gets this LstDirectedEdge's symmetric LstDirectedEdge, which runs in the opposite direction.
   * While still connecting the same nodes, the from and to nodes will be the opposite of this. The
   * graph this LstDirectedEdge is in may not have a symmetric edge, and if not, this will be null.
   * Setting this value is the responsibility of the graph.
   *
   * @return this LstDirectedEdge's symmetric LstDirectedEdge or null if it does not exist.
   */
  public LstDirectedEdge getSym() {
    return sym;
  }

  /**
   * Sets this LstDirectedEdge's symmetric LstDirectedEdge, which runs in the opposite direction.
   * While still connecting the same nodes, the from and to nodes will be the opposite of this.
   * Setting this value is the responsibility of the graph.
   */
  public void setSym(LstDirectedEdge sym) {
    this.sym = sym;
  }

  /**
   * Gets the length of this LstDirectedEdge in three dimensions (including height).
   *
   * @return the length of this LstDirectedEdge in three dimensions
   */
  public double get3DLengthSquared() {
    return Math.pow(from.height() - to.height(), 2) + (dx * dx) + (dy * dy);
  }

  /**
   * Gets the length of this LstDirectedEdge in three dimensions (including height) between two
   * specified heights, which can be useful if the calculated heights need to be constrained between
   * different values than those calculated during terrain generation.
   *
   * @param fromZ the height of the "from" node
   * @param toZ   the height of the "to" node
   * @return the length of this LstDirectedEdge in three dimensions between the two specified
   * heights
   */
  public double get3DLengthSquared(double fromZ, double toZ) {
    return Math.pow(fromZ - toZ, 2) + (dx * dx) + (dy * dy);
  }

  /**
   * Removes this LstDirectedEdge from any related structures to aid in garbage collection.
   */
  void unbind() {
    if (sym != null) {
      sym.sym = null;
    }
    this.sym = null;
    from.remove(this);
    to.removeInboundNode(from);
  }

  @Override public int hashCode() {
    return Objects.hash(from, to);
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof LstDirectedEdge other) {
      return other
          .getFromNode()
          .equals(from) &&
          other
              .getToNode()
              .equals(to);
    }
    return false;
  }

  /**
   * Since this is a directed edge, it links from one node to another, and not the reverse. This
   * method returns the node that this LstDirectedEdge is directed from (i.e. the first node).
   *
   * @return the node that this LstDirectedEdge is directed from, as set in the constructor.
   */
  public LstNode getFromNode() {
    return from;
  }

  /**
   * Since this is a directed edge, it links from one node to another, and not the reverse. This
   * method returns the node that this LstDirectedEdge is directed to (i.e. the second node).
   *
   * @return the node that this LstDirectedEdge is directed to, as set in the constructor.
   */
  public LstNode getToNode() {
    return to;
  }

  /**
   * Does a comparison of two edges based on their direction. Primarily used for sorting edges in
   * a consistent order.
   */
  public int compareTo(LstDirectedEdge de) {
    return Double.compare(angle, de.angle);
  }

  /**
   * Converts this edge to a normalized vector in 2D space (ignores height).
   *
   * @return a new Vector2d based on the difference between the two nodes, normalized.
   */
  public Vector2d normalizedVector2d() {
    var vectored = new Vector2d(dx, dy);
    vectored.normalize();
    return vectored;
  }

  /**
   * Converts this edge to a normalized vector in 3D space (includes height).
   *
   * @return a new Vector3d based on the difference between the two nodes, normalized.
   */
  public Vector3d normalizedVector3d() {
    var vectored = new Vector3d(dx, dy, from.height() - to.height());
    vectored.normalize();
    return vectored;
  }

  /**
   * Gets the angle of this edge in 2D space, relative to the X axis in radians,
   * between -PI and PI.
   */
  public double angle() {
    return angle;
  }
}
