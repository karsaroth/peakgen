package peakgen.generate.topology;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static peakgen.generate.TestUtil.lstn;

class LstDirectedEdgeTest {

  @Test
  void createEdgeWithValidNodes() {
    LstNode from = lstn(0, 0);
    LstNode to = lstn(1, 1);
    LstDirectedEdge edge = new LstDirectedEdge(from, to);

    assertEquals(from, edge.getFromNode());
    assertEquals(to, edge.getToNode());
  }

  @Test
  void setAndGetSaddleNodes() {
    LstNode from = lstn(0, 0);
    LstNode to = lstn(1, 1);
    LstDirectedEdge edge = new LstDirectedEdge(from, to);
    LstNode saddleFrom = lstn(0.5, 0.5);
    LstNode saddleTo = lstn(0.75, 0.75);

    edge.setSaddleNodeFrom(saddleFrom);
    edge.setSaddleNodeTo(saddleTo);

    assertEquals(saddleFrom, edge.getSaddleNodeFrom());
    assertEquals(saddleTo, edge.getSaddleNodeTo());
  }

  @Test
  void setAndGetPassHeight() {
    LstNode from = lstn(0, 0);
    LstNode to = lstn(1, 1);
    LstDirectedEdge edge = new LstDirectedEdge(from, to);
    double passHeight = 10.0;

    edge.setPassHeight(passHeight);

    assertEquals(passHeight, edge.getPassHeight());
  }

  @Test
  void setAndGetInsertOrder() {
    LstNode from = lstn(0, 0);
    LstNode to = lstn(1, 1);
    LstDirectedEdge edge = new LstDirectedEdge(from, to);
    long insertOrder = 5L;

    edge.setInsertOrder(insertOrder);

    assertEquals(insertOrder, edge.getInsertOrder());
  }

  @Test
  void compareEdgesByDirection() {
    LstNode from = lstn(0, 0);
    LstNode to1 = lstn(1, 1);
    LstNode to2 = lstn(1, 0);
    LstDirectedEdge edge1 = new LstDirectedEdge(from, to1);
    LstDirectedEdge edge2 = new LstDirectedEdge(from, to2);

    assertTrue(edge1.compareTo(edge2) > 0);
  }

  @Test
  void compareEdgesWithSameDirection() {
    LstNode from = lstn(0, 0);
    LstNode to1 = lstn(1, 1);
    LstNode to2 = lstn(1, 1);
    LstDirectedEdge edge1 = new LstDirectedEdge(from, to1);
    LstDirectedEdge edge2 = new LstDirectedEdge(from, to2);

    assertEquals(0, edge1.compareTo(edge2));
  }

  @Test
  void compareEdgesWithOppositeDirection() {
    LstNode from1 = lstn(0, 0);
    LstNode to1 = lstn(1, 1);
    LstNode from2 = lstn(1, 1);
    LstNode to2 = lstn(0, 0);
    LstDirectedEdge edge1 = new LstDirectedEdge(from1, to1);
    LstDirectedEdge edge2 = new LstDirectedEdge(from2, to2);

    assertTrue(edge1.compareTo(edge2) != 0);
  }

  @Test
  void createSymmetricEdge() {
    LstNode from = lstn(0, 0);
    LstNode to = lstn(1, 1);
    LstDirectedEdge edge = new LstDirectedEdge(from, to);
    LstDirectedEdge symEdge = new LstDirectedEdge(to, from);

    edge.setSym(symEdge);

    assertEquals(symEdge, edge.getSym());
    assertNull(symEdge.getSym());
  }

  @Test
  void unbindEdge() {
    LstNode from = lstn(0, 0);
    LstNode to = lstn(1, 1);
    LstDirectedEdge edge = new LstDirectedEdge(from, to);
    LstDirectedEdge symEdge = new LstDirectedEdge(to, from);
    edge.setSym(symEdge);
    symEdge.setSym(edge);

    edge.unbind();

    assertNull(edge.getSym());
    assertNull(symEdge.getSym());
    assertFalse(from.outEdges().contains(edge));
    assertFalse(to.inboundNodes().contains(from));
  }

  @Test
  void edgeEquality() {
    LstNode from1 = lstn(0, 0);
    LstNode to1 = lstn(1, 1);
    LstNode from2 = lstn(0, 0);
    LstNode to2 = lstn(1, 1);
    LstDirectedEdge edge1 = new LstDirectedEdge(from1, to1);
    LstDirectedEdge edge2 = new LstDirectedEdge(from2, to2);

    assertEquals(edge1, edge2);
  }

  @Test
  void edgeInequality() {
    LstNode from1 = lstn(0, 0);
    LstNode to1 = lstn(1, 1);
    LstNode from2 = lstn(0, 0);
    LstNode to2 = lstn(2, 2);
    LstDirectedEdge edge1 = new LstDirectedEdge(from1, to1);
    LstDirectedEdge edge2 = new LstDirectedEdge(from2, to2);

    assertNotEquals(edge1, edge2);
  }

  @Test
  void edgeHashCode() {
    LstNode from = lstn(0, 0);
    LstNode to = lstn(1, 1);
    LstDirectedEdge edge = new LstDirectedEdge(from, to);

    int expectedHashCode = Objects.hash(from, to);
    assertEquals(expectedHashCode, edge.hashCode());
  }
}
