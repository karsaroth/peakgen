package peakgen.generate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tinfour.common.Vertex;
import peakgen.generate.settings.LstSettings;
import peakgen.generate.topology.LstCoordinate;
import peakgen.generate.topology.LstDirectedEdge;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static peakgen.generate.TestUtil.*;
import static peakgen.generate.util.GenerationUtils.PRECISION_MODEL;

class LargeScaleTerrainGeneratorTest {
  private static final long TEST_SEED_1 = "TestSeed".hashCode();
  private LstSettings mockTerrainSettings;
  private GeologySettings mockGeologySettings;
  private LargeScaleTerrainGenerator generator;

  @BeforeEach void setUp() {
    mockTerrainSettings = mock(LstSettings.class);
    mockGeologySettings = mock(GeologySettings.class);
    when(mockTerrainSettings.size()).thenReturn(40);
    when(mockTerrainSettings.lod()).thenReturn(20);
    when(mockTerrainSettings.random()).thenReturn(new Random(TEST_SEED_1));
    when(mockTerrainSettings.getData(anyDouble(), anyDouble(), anyInt())).then(invocationOnMock -> {
      var x = (double) invocationOnMock.getArgument(0);
      var y = (double) invocationOnMock.getArgument(1);
      if ((x > 10 && x < 15) || (y > -5 && y < 0)) {
        // Create two "sea zones" for testing.
        return new LstCoordinate(x, y, 0.0d, 0.0d, 0.0d);
      } else if (Math.abs(x) >= 20 || Math.abs(y) >= 20) {
        // Don't include border points.
        return new LstCoordinate(x, y, 0.0d, 0.0d, 0.0d);
      } else {
        return new LstCoordinate(x, y, 0.5d, 0.5d, 0.3d);
      }
    });
    when(mockGeologySettings.deltaT()).thenReturn(250000.0);
    when(mockGeologySettings.maxU()).thenReturn(5.01e-4);
    when(mockGeologySettings.minU()).thenReturn(0.0);
    when(mockGeologySettings.k()).thenReturn(5.61e-7);
    when(mockGeologySettings.m()).thenReturn(0.5);
    when(mockGeologySettings.maxSlopeRadians()).thenReturn(Math.toRadians(58));
    when(mockGeologySettings.minSlopeRadians()).thenReturn(Math.toRadians(6));
    generator = new LargeScaleTerrainGenerator(mockGeologySettings, mockTerrainSettings, s -> {
    });
  }

  @Test void generatePointDistribution_createsNonEmptySet() {
    var points = generator.generatePointDistribution();
    assertFalse(points.isEmpty());
    assertEquals(16, points.size());
  }

  @Test void generateGraph_createsNonGraph() {
    Set<Vertex> points = new HashSet<>();
    points.add(c(0, 0));
    points.add(c(1, 1));
    points.add(c(1, 2));
    var rpg = generator.generateGraph(points, 500, (s) -> {});
    assertNotNull(rpg);
    assertFalse(rpg
        .getNodes()
        .isEmpty());
    assertFalse(rpg
        .getEdges()
        .isEmpty());
  }

  @Test void generateGraph_checkEdgeFiltering() {
    // This set will contain "mainland", "island" and "sea" coordinates.
    Set<Vertex> points = new HashSet<>();
    points.add(c(1, 1));
    points.add(c(1, 3));
    points.add(c(6, 2));
    points.add(c(7, 5));
    points.add(c(16, 3)); // Island Node
    points.add(c(12, -2)); // Sea Node
    points.add(c(17, 1)); // Island Node
    points.add(c(18, 5)); // Island Node
    points.add(c(5, -2)); // Sea Node
    var rpg = generator.generateGraph(points, 20, (s) -> {});
    assertNotNull(rpg);
    Function<LstDirectedEdge, Boolean> islandMainland = line -> {
      var coordA = line
          .getFromNode()
          .getCoordinate();
      var coordB = line
          .getToNode()
          .getCoordinate();
      return ((coordA.x < 10 && coordB.x > 15) || (coordA.x > 15 && coordB.x < 10));
    };
    for (var edge : rpg.getEdges()) {
      // Ignore bounds connecting linestrings
      var from = edge
          .getFromNode()
          .getCoordinate();
      var to = edge
          .getToNode()
          .getCoordinate();
      if (Math.abs(from.x) < 20 && Math.abs(from.y) < 20 && Math.abs(to.x) < 20 && Math.abs(to.y) < 20) {
        // Check each linestring for anticipated invalid lines
        // No sea-to-sea connections
        assertFalse(from.isSea() && to.isSea());
        // No island-to-mainland connections
        assertFalse(islandMainland.apply(edge));
      }
    }
  }

  @Test @SuppressWarnings("unchecked") void generate_singleStep() {
    var nodeCount = generator
        .getRpg()
        .getNodes()
        .size();
    assertNotEquals(0, nodeCount);
    var edgeCount = generator
        .getRpg()
        .getEdges()
        .size();
    assertNotEquals(0, edgeCount);
    for (var node : generator
        .getRpg()
        .getNodes()) {
      assertEquals(0, node.height());
    }
    Predicate<LargeScaleTerrainGenerator> stopCondition = mock(Predicate.class);
    when(stopCondition.test(any())).thenReturn(true);
    generator.generate(stopCondition);
    verify(stopCondition, times(1)).test(any());
    for (var node : generator
        .getRpg()
        .getNodes()) {
      assertEquals(0, node.height());
    }
    generator.generate(g -> generator.getNumberOfSteps() > 0);
    generator
        .getRpg()
        .getNodes()
        .forEach(node -> {
          if (node
              .getCoordinate()
              .isSea()) {
            assertEquals(0.0d, node.height());
          } else {
            assertNotEquals(0.0d, node.height());
          }
        });
  }

  @Test void getNumberOfSteps_returnsCorrectStepCount() {
    generator.setNumberOfSteps(5);
    assertEquals(5, generator.getNumberOfSteps());
  }

  @Test void setNumberOfSteps_updatesStepCount() {
    generator.setNumberOfSteps(10);
    assertEquals(10, generator.getNumberOfSteps());
  }

  @Test void applyThermalShockHeuristicPredetermined_appliesCorrectly() {
    double result = generator.applyThermalShockHeuristicPredetermined(Math.toRadians(78.7), 100.0, 50.0, 10, 0.5);
    assertEquals(56.24869351909327d, result);
  }

  @Test void generateTriangularMesh_createsValid3dMesh() {
    generator.generate(g -> generator.getNumberOfSteps() > 0);
    var mesh = generator.generateTriangularMesh();
    var pct = generator.preCulledTriangles;
    assertNotNull(mesh);
    assertNotEquals(0, mesh.getFaceCount());
    assertEquals(pct.size(), mesh.getFaceCount());
    for (var face : mesh.faces()) {
      assertTrue(
          pct
              .stream()
              .anyMatch(t -> faceIsTriangle(face, t)),
          "Face (" + verticesToString(face) + ") not found " + "in " + "pre-culled triangles."
      );
    }
  }

  @Test void generateStreamTreeMesh_createsValid3dStreamTrees() {
    generator.generate(g -> generator.getNumberOfSteps() > 0);
    var tree = generator.generateStreamTreeCollection();
    var cst = generator.getCurrentStreamTrees();
    assertNotNull(tree);
    assertNotEquals(0, tree.size());
    for (var segment : tree) {
      assertTrue(cst
          .getEdges()
          .stream()
          .anyMatch(e -> PRECISION_MODEL.eq(
              e.p0.x,
              segment
                  .getStartPoint()
                  .getX()
          ) && PRECISION_MODEL.eq(
              e.p0.y,
              segment
                  .getStartPoint()
                  .getY()
          ) && PRECISION_MODEL.eq(
              e.p1.x,
              segment
                  .getEndPoint()
                  .getX()
          ) && PRECISION_MODEL.eq(
              e.p1.y,
              segment
                  .getEndPoint()
                  .getY()
          )), "Segment (" + segment.toString() + ") not found in current stream trees.");
    }
  }
}
