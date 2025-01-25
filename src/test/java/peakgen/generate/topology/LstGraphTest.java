package peakgen.generate.topology;

import org.junit.jupiter.api.Test;
import peakgen.generate.TestUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static peakgen.generate.TestUtil.lstc;

class LstGraphTest {

  @Test
  void addNodeToGraph() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate coord = lstc(0, 0);
    LstNode node = graph.add(coord);

    assertTrue(graph.getNodes().contains(node));
    assertEquals(coord, node.getCoordinate());
  }

  @Test
  void addEdgeToGraph() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate from = lstc(0, 0);
    LstCoordinate to = lstc(1, 1);
    LstDirectedEdge edge = graph.add(from, to);

    assertTrue(graph.getEdges().contains(edge));
    assertEquals(from, edge.getFromNode().getCoordinate());
    assertEquals(to, edge.getToNode().getCoordinate());
  }

  @Test
  void findNodeInGraph() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate coord = lstc(0, 0);
    LstNode node = graph.add(coord);

    Optional<LstNode> foundNode = graph.findNode(coord);
    assertTrue(foundNode.isPresent());
    assertEquals(node, foundNode.get());
  }

  @Test
  void findEdgeInGraph() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate from = lstc(0, 0);
    LstCoordinate to = lstc(1, 1);
    LstDirectedEdge edge = graph.add(from, to);

    Optional<LstDirectedEdge> foundEdge = graph.findEdge(from, to);
    assertTrue(foundEdge.isPresent());
    assertEquals(edge, foundEdge.get());
  }

  @Test
  void removeNodeFromGraph() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate coord = lstc(0, 0);
    LstNode node = graph.add(coord);

    graph.remove(node);
    assertFalse(graph.getNodes().contains(node));
  }

  @Test
  void removeEdgeFromGraph() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate from = lstc(0, 0);
    LstCoordinate to = lstc(1, 1);
    LstDirectedEdge edge = graph.add(from, to);

    graph.remove(edge);
    assertFalse(graph.getEdges().contains(edge));
  }

  @Test
  void addBiDirectionalWithSaddlesEdges() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate from = lstc(0, 0);
    LstCoordinate to = lstc(1, 1);
    graph.addBiDirectional(from, to);

    Optional<LstDirectedEdge> edge1 = graph.findEdge(from, to);
    Optional<LstDirectedEdge> edge2 = graph.findEdge(to, from);

    assertTrue(edge1.isPresent());
    assertTrue(edge2.isPresent());
    assertEquals(edge1.get().getSym(), edge2.get());
    assertEquals(edge2.get().getSym(), edge1.get());
  }

  @Test
  void clearGraph() {
    LstGraph graph = new LstGraph(LstDirectedEdge::new, TestUtil::lstn);
    LstCoordinate coord1 = lstc(0, 0);
    LstCoordinate coord2 = lstc(1, 1);
    graph.add(coord1);
    graph.add(coord2);
    graph.add(coord1, coord2);

    graph.clear();
    assertTrue(graph.getNodes().isEmpty());
    assertTrue(graph.getEdges().isEmpty());
  }
}
