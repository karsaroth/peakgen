package peakgen.generate;

import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.euclidean.threed.line.Lines3D;
import org.apache.commons.geometry.euclidean.threed.line.Segment3D;
import org.apache.commons.geometry.euclidean.threed.mesh.SimpleTriangleMesh;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.RandomGeneratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinfour.common.IQuadEdge;
import org.tinfour.common.PolygonConstraint;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import org.tinfour.standard.IncrementalTin;
import org.tinfour.voronoi.BoundedVoronoiDiagram;
import peakgen.generate.settings.LstSettings;
import peakgen.generate.topology.LstCoordinate;
import peakgen.generate.topology.LstDirectedEdge;
import peakgen.generate.topology.LstGraph;
import peakgen.generate.topology.LstNode;
import peakgen.generate.util.GenerationUtils;
import peakgen.generate.util.Tuples.T2;
import peakgen.generate.util.Tuples.T4;

import javax.vecmath.Point2d;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static peakgen.generate.util.GenerationUtils.PRECISION_MODEL;
import static peakgen.generate.util.GenerationUtils.pointHash;

/**
 * Generates terrain using "Large Scale Terrain Generation from Tectonic Uplift and Fluvial
 * Erosion", implemented in Java<br>
 * <br>
 *
 * <p>Source: Guillaume Cordonnier, Jean Braun, Marie-Paule Cani, Bedrich Benes, Eric Galin, et al..
 * Large Scale Terrain Generation from Tectonic Uplift and Fluvial Erosion. Computer Graphics Forum,
 * 2016, Proc. EUROGRAPHICS 2016, 35 (2), pp.165-175. ff10.1111/cgf.12820ff. ffhal-01262376ff
 */
public class LargeScaleTerrainGenerator {
  public static final String STEP_COUNTER = "StepCounter";
  static final Logger LOGGER = LoggerFactory.getLogger(LargeScaleTerrainGenerator.class);
  final LstSettings terrainSettings;
  final GeologySettings geologySettings;
  final LstGraph rpGraph;
  final LstGraph streamTreeGraph;
  final LstGraph lakeGraph =
      new LstGraph(LstDirectedEdge::new, c -> new LstNode(c, 0.0d, 0.0d));
  final Set<LstDirectedEdge> lakeGraphTree =
      new TreeSet<>(Comparator.comparing(a -> a.getFromNode().getCoordinate()));
  final TreeSet<LstDirectedEdge> lakeBlackRedTree = new TreeSet<>(LstDirectedEdge.LAKE_COMPARATOR);
  int numberOfSteps;
  double maxHeight;

  ArrayList<SimpleTriangle> preCulledTriangles;

  /**
   * Create a new Large Scale Terrain Generator with the given terrain settings, and default
   * geological settings. SL4J logger will log any messages. Note, the construction process is
   * expensive computationally and may take some time, especially for high levels of detail.
   *
   * @param terrainSettings The settings for the terrain generation.
   */
  public LargeScaleTerrainGenerator(LstSettings terrainSettings) {
    this(new GeologySettings(), terrainSettings, LOGGER::info);
  }

  /**
   * Create a new Large Scale Terrain Generator with the given geological and terrain settings.
   * Note, the construction process is expensive computationally and may take some time, especially
   * for high levels of detail.
   *
   * @param geologySettings The settings for geological values and constants.
   * @param terrainSettings The settings for the terrain generation.
   */
  public LargeScaleTerrainGenerator(
      GeologySettings geologySettings, LstSettings terrainSettings, Consumer<String> messages) {
    this.terrainSettings = terrainSettings;
    this.geologySettings = geologySettings;
    var halfSize = this.terrainSettings.size() / 2;
    messages.accept("LST Init Step 1: Generate coordinates.");
    Set<Vertex> coordinates = generatePointDistribution();
    messages.accept("LST Init Step 2: Generate Data Structures.");
    this.rpGraph = generateGraph(coordinates, halfSize, messages);
    setNumberOfSteps(0);
    messages.accept("LST Init Step 3: Create Empty Stream Tree Graph.");
    this.streamTreeGraph =
        new LstGraph(LstDirectedEdge::new, (c) -> rpGraph.findNodeF(c).clone());
  }

  /**
   * Generate a point distribution for the terrain, using Poisson sampling to introduce some
   * randomness.
   *
   * @return A set of coordinates representing the point distribution over non-ocean areas.
   */
  public Set<Vertex> generatePointDistribution() {
    var m = Math.round(Math.sqrt(terrainSettings.lod()));
    var jumpMean = terrainSettings.size() / m;
    var halfSize = terrainSettings.size() / 2;
    var poisson =
        new PoissonDistribution(
            RandomGeneratorFactory.createRandomGenerator(terrainSettings.random()),
            jumpMean * 2,
            PoissonDistribution.DEFAULT_EPSILON,
            PoissonDistribution.DEFAULT_MAX_ITERATIONS);
    return LongStream.range(0, m)
        .mapToObj(
            i ->
                LongStream.range(0, m)
                    .mapToObj(
                        j -> {
                          var x =
                              Math.min(
                                  Math.max(
                                      (-halfSize + 1),
                                      (-halfSize + 1)
                                          + ((i * jumpMean) + poisson.sample() - jumpMean)),
                                  halfSize - 1);
                          var y =
                              Math.min(
                                  Math.max(
                                      (-halfSize + 1),
                                      (-halfSize + 1)
                                          + ((j * jumpMean) + poisson.sample() - jumpMean)),
                                  halfSize - 1);
                          return new Vertex(x, y, 0.0d);
                        }))
        .flatMap(Function.identity())
        .collect(Collectors.toSet());
  }

  /**
   * Generate the initial random planar graph for the terrain, using a conforming Delaunay
   * triangulation.
   *
   * @param points The set of points to use for the graph.
   * @param halfSize The halfSize (max distance from origin) of the terrain
   * @return A MultiLineString representing the initial random planar graph.
   */
  public LstGraph generateGraph(Set<Vertex> points, int halfSize, Consumer<String> messages) {
    var m = Math.round(Math.sqrt(terrainSettings.lod()));
    var jumpMean = terrainSettings.size() / m;
    IncrementalTin tin = new IncrementalTin(jumpMean);
    messages.accept(
        "Lst Init Step 2.1: Generate Constrained Delaunay Triangulation from %d points."
            .formatted(points.size()));
    tin.add(points.stream().toList(), null);
    tin.addConstraints(
        List.of(
            new PolygonConstraint(
                new Vertex(-halfSize, -halfSize, 0.0d),
                new Vertex(halfSize, -halfSize, 0.0d),
                new Vertex(halfSize, halfSize, 0.0d),
                new Vertex(-halfSize, halfSize, 0.0d))),
        true);
    messages.accept("Lst Init Step 2.2: Generate corresponding Voronoi Diagram.");
    var voronoi = new BoundedVoronoiDiagram(tin);
    var catchment = new HashMap<Integer, Double>();
    for (var p : voronoi.getPolygons()) {
      var v = p.getVertex();
      catchment.put(pointHash(v.getX(), v.getY()), p.getArea());
    }
    messages.accept("Lst Init Step 2.3: Generate base graph with catchment areas.");
    var graph = generateNewBaseGraph(c -> catchment.get(c.hashCode()));
    messages.accept("Lst Init Step 2.4: Save pre-culled triangles for later use.");
    preCulledTriangles = new ArrayList<>(tin.countTriangles().getCount());
    tin.triangles().forEach(preCulledTriangles::add);

    messages.accept("Lst Init Step 2.5: Generate Terrain Data for each node.");
    for (IQuadEdge edge : tin.edges()) {
      var pointA = edge.getA();
      var pointB = edge.getB();
      var pointAClone = terrainSettings.getData(pointA.x, pointA.y, halfSize);
      var pointBClone = terrainSettings.getData(pointB.x, pointB.y, halfSize);

      // Case 1: point A or point B is in the ocean, but not both.
      if (pointAClone.isSea() ^ pointBClone.isSea()) {
        graph.addBiDirectional(
            terrainSettings.getData(pointA.x, pointA.y, halfSize),
            terrainSettings.getData(pointB.x, pointB.y, halfSize));
        // Check for the negative of:
        // Case 2: Both point A and point B are in the ocean (if pointAClone is true
        // because of XOR above, at this point pointBClone must also be true).
        // Case 3: Neither point A nor point B are in the ocean, but the edge crosses the ocean.
      } else if (!pointAClone.isSea()
          && !GenerationUtils.sampleForSea(
              pointAClone,
              pointBClone,
              (int) Math.max(2, Math.min(50, Math.floor(edge.getLength()))),
              (x, y) -> terrainSettings.getData(x, y, 0))) {
        graph.addBiDirectional(
            terrainSettings.getData(pointA.x, pointA.y, halfSize),
            terrainSettings.getData(pointB.x, pointB.y, halfSize));
      }
      // If Case 2 or 3, discard the edge.
    }
    return graph;
  }

  /**
   * Build an LST specific planar graph that will slot appropriate data into the nodes as they are
   * created
   *
   * @param catchment A mapping of coordinates to catchment values.
   * @return An LST specific planar graph with each node initialized with the provided values.
   */
  public LstGraph generateNewBaseGraph(Function<LstCoordinate, Double> catchment) {
    return new LstGraph(
        LstDirectedEdge::new,
        (c) -> {
          if (c.isSea()) {
            return new LstNode(c, 0.0, 0.0, 0.0, -1, 0.0);
          }
          return new LstNode(
              c,
              GenerationUtils.lerp(
                  geologySettings.minU(), geologySettings.maxU(), c.upliftFactor()),
              0.0,
              catchment.apply(c),
              -1,
              c.slopeFactor());
        });
  }

  /**
   * Create a new Large Scale Terrain Generator with the given terrain settings, and default
   * geological settings.
   *
   * @param terrainSettings The settings for the terrain generation.
   */
  public LargeScaleTerrainGenerator(LstSettings terrainSettings, Consumer<String> messages) {
    this(new GeologySettings(), terrainSettings, messages);
  }

  /**
   * Get the geological settings used by the terrain generator.
   *
   * @return The geological settings.
   */
  public GeologySettings getGeologySettings() {
    return geologySettings;
  }

  /**
   * Get the terrain settings used by the terrain generator.
   *
   * @return The terrain settings.
   */
  public LstSettings getTerrainSettings() {
    return terrainSettings;
  }

  /**
   * Get the random planar graph generated by the terrain generator.
   *
   * @return The random planar graph.
   */
  public LstGraph getRpg() {
    return this.rpGraph;
  }

  /**
   * Get the current stream tree(s) generated by the terrain generator.
   *
   * @return The current stream tree(s).
   */
  public LstGraph getCurrentStreamTrees() {
    return streamTreeGraph;
  }

  /**
   * Get the pre-culled triangles from the random planar graph, apply the current height values to
   * the coordinates, and generate a new 3D geometry with the updated height values for the z-axis.
   * If the coordinate was culled during the generation process, the height value will be set based
   * on the terrain settings data values, using a default max depth of -1500.0d.
   *
   * @return A 3D geometry representing the terrain.
   */
  public SimpleTriangleMesh generateTriangularMesh() {
    var meshBuilder = SimpleTriangleMesh.builder(PRECISION_MODEL);
    var vertexMap = new HashMap<Vertex, Integer>();
    for (var triangle: preCulledTriangles) {
      var v0 = triangle.getVertexC();
      var v1 = triangle.getVertexB();
      var v2 = triangle.getVertexA();
      var v0Index = vertexMap.computeIfAbsent(v0, k -> meshBuilder.addVertex(vectorByData(v0)));
      var v1Index = vertexMap.computeIfAbsent(v1, k -> meshBuilder.addVertex(vectorByData(v1)));
      var v2Index = vertexMap.computeIfAbsent(v2, k -> meshBuilder.addVertex(vectorByData(v2)));
      meshBuilder.addFace(v0Index, v1Index, v2Index);
    }
    return meshBuilder.build();
  }

  Vector3D vectorByData(Vertex v) {
    var node = rpGraph.findNodeByPosition(new Point2d(v.x, v.y));
    if (node != null) {
      return Vector3D.of(node.getCoordinate().x, node.getCoordinate().y, node.height());
    } else {
      var seaFactor = terrainSettings.getData(v.x, v.y, 0).seaFactor;
      var height = GenerationUtils.lerp(0.0d, -1500.0d, Math.abs(seaFactor));
      return Vector3D.of(v.x, v.y, height);
    }
  }

  /**
   * Generate a 3D geometry representing the stream tree(s) generated by the terrain generator.
   *
   * @return A 3D geometry representing the stream tree(s).
   */
  public Collection<Segment3D> generateStreamTreeCollection() {
    var edges = streamTreeGraph.getEdges();
    return edges.stream()
        .map(
            e ->
                Lines3D.segmentFromPoints(
                    Vector3D.of(e.p0.x, e.p0.y, e.from.height()),
                    Vector3D.of(e.p1.x, e.p1.y, e.to.height()),
                    PRECISION_MODEL))
        .collect(Collectors.toList());
  }

  /**
   * Generate the terrain using the Large Scale Terrain Generation algorithm. This will call the
   * {@link LargeScaleTerrainGenerator#generateSingleStep()} method until the stop condition is met.
   *
   * @param stopCondition The condition to stop the generation process, based on the current state of
   *                      the generator (e.g. number of steps taken, max height etc.)
   */
  public void generate(Predicate<LargeScaleTerrainGenerator> stopCondition) {
    while (!stopCondition.test(this)) {
      generateSingleStep();
    }
  }

  /**
   * Generate a single step of the Large Scale Terrain Generation algorithm. This will compute the
   * stream tree, build the lake graph, compute the lake tree, add saddles to the stream tree, and
   * apply uplift, stream power, and thermal shock.
   */
  public void generateSingleStep() {
    streamTreeGraph.clear();
    setNumberOfSteps(getNumberOfSteps() + 1);
    computeStreamTree();
    buildLakeGraph();
    computeLakeTree();
    addSaddlesToStreamTree();
    applyUpliftStreamPowerThermalShock();
  }

  /**
   * Provides the recorded number of steps taken to generate the terrain. The value is stored as
   * User Data on the Random Planar Graph.
   *
   * @return The number of steps taken to generate the terrain.
   */
  public int getNumberOfSteps() {
    return this.numberOfSteps;
  }

  void setNumberOfSteps(int steps) {
    this.numberOfSteps = steps;
  }

  public double getMaxHeight() {
    return maxHeight;
  }

  void setMaxHeight(double maxHeight) {
    this.maxHeight = maxHeight;
  }

  /**
   * Step 1: Compute the stream tree for the current state of the terrain. (Simpler and hopefully
   * faster than previous versions)
   */
  void computeStreamTree() {
    for (LstNode node : rpGraph.getNodes()) {
      streamTreeGraph.add(node.pt(), c -> node.clone());
      if (node.pt().isSea()) {
        // Sea level node, no need to add edges.
        continue;
      }
      // From this node, find an edge to the lowest neighbor.
      var edgeStar = node.outEdges().iterator();
      if (!edgeStar.hasNext()) {
        throw new IllegalArgumentException(
            "Isolated node in RPG: No edges found for coordinate %s"
                .formatted(node.getCoordinate()));
      }
      var lowest = edgeStar.next();
      while (edgeStar.hasNext()) {
        var current = edgeStar.next();
        // KEY: Lowest neighbour
        if (lowest.getToNode().height() > current.getToNode().height()) {
          lowest = current;
        }
      }
      var lowestDestination = lowest.getToNode();
      // We've found a lower neighbor, so we should add this edge to the stream tree.
      // Otherwise, this is a local minimum, so it should be the end of the stream. Similarly to the
      // sink case.
      if (lowestDestination.height() < node.height()) {
        streamTreeGraph.add(node.pt(), lowestDestination.pt());
      }
    }
  }

  /** Step 2(A): Find the lakes in the current state of the terrain. */
  void buildLakeGraph() {
    var lakeId = -1L;
    LinkedList<LstNode> processing = new LinkedList<>();
    HashMap<Long, T2<LstNode, LinkedList<LstNode>>> allSinks = new HashMap<>();
    if (streamTreeGraph.sinks().isEmpty()) {
      throw new IllegalStateException("No sinks/lakes found in stream tree graph.");
    }
    for (var s : streamTreeGraph.sinks()) {
      var lake = ++lakeId;
      // First assign each sink a unique lake id.
      s.setLakeId(lake);
      processing.push(s);
      allSinks.put(lake, T2.of(s, new LinkedList<>()));
    }

    while (!processing.isEmpty()) {
      var node = processing.poll();
      var nodeLakeId = node.lakeId();
      var lakeList = allSinks.get(nodeLakeId).second();
      var rpNode = rpGraph.findNodeF(node.pt());
      rpNode.setLakeId(nodeLakeId);
      lakeList.add(node);
      for (var upstream : node.inboundNodes()) {
        // Populate the lake id for each node in the stream tree.
        upstream.setLakeId(nodeLakeId);
        processing.push(upstream);
      }
    }

    // Now we can build the lake graph.
    for (var sinkTreeA : allSinks.values()) {
      var sinkNodeA = sinkTreeA.first();
      var sinkNodeATreeNodes = sinkTreeA.second();

      var saddleNodes = new HashMap<Long, T4<LstNode, LstNode, LstNode, LstNode>>();
      // Find any nodes connected directly to a node in this stream tree
      // that are in a different lake. These are saddles.
      for (LstNode treeNode : sinkNodeATreeNodes) {
        // Look for connections in rpGraph, not the disconnected stream trees.
        var rpNode = rpGraph.findNodeF(treeNode.getCoordinate());
        for (var e : rpNode.outEdges()) {
          // KEY: Lowest Saddle
          var toNode = e.getToNode();
          var toNodeLakeId = toNode.lakeId();
          if (toNodeLakeId != rpNode.lakeId()) {
            // Need the disconnected stream tree sink node!
            var sinkNodeB = allSinks.get(toNodeLakeId).first();
            if (sinkNodeA.pt().isSea() && sinkNodeB.pt().isSea()) {
              // If both are sea nodes, we don't want to add this edge to the lake graph.
              // I think the paper missed this possibility.
              continue;
            }
            if (saddleNodes.containsKey(toNodeLakeId)) {
              var existing = saddleNodes.get(toNodeLakeId);
              if (Math.max(rpNode.height(), toNode.height())
                  < Math.max(existing.first().height(), existing.second().height())) {
                saddleNodes.put(toNodeLakeId, T4.of(rpNode, toNode, sinkNodeA, sinkNodeB));
              }
            } else {
              saddleNodes.put(toNodeLakeId, T4.of(rpNode, toNode, sinkNodeA, sinkNodeB));
            }
          }
        }
      }

      // Find the lowest saddle node pair, and add that pair's sinks to the lake graph.
      // (With bidirectional edges, the next step will determine direction)
      for (Entry<Long, T4<LstNode, LstNode, LstNode, LstNode>> saddle : saddleNodes.entrySet()) {
        var saddleNodeA = saddle.getValue().first();
        var saddleNodeB = saddle.getValue().second();
        var saddleSinkNodeA = saddle.getValue().third();
        var saddleSinkNodeB = saddle.getValue().fourth();
        var passHeight = Math.max(saddleNodeA.height(), saddleNodeB.height());
        lakeGraph.addBiDirectionalWithSaddles(
            saddleSinkNodeA.getCoordinate(),
            saddleSinkNodeB.getCoordinate(),
            saddleNodeA,
            saddleNodeB,
            c -> {
              if (c.equals(saddleSinkNodeA.getCoordinate())
                  || c.equals(saddleSinkNodeB.getCoordinate())) {
                return new LstNode(c, 0.0d, 0.0d);
              } else {
                throw new IllegalArgumentException(
                    "Coordinate %s not found in saddleSinkNodeA or saddleSinkNodeB".formatted(c));
              }
            },
            (from, to) -> {
              var e = new LstDirectedEdge(from, to);
              e.setPassHeight(passHeight);
              return e;
            });
      }
    }
  }

  /*
   * Step 2(B): Compute the lake tree from the lakes in the lake graph.
   */
  void computeLakeTree() {
    if (lakeGraph.getNodes().isEmpty()) {
      return;
    }
    var insertNo = 0L;
    var candidates = lakeGraph.getNodes().stream().filter(n -> n.getCoordinate().isSea()).toList();

    if (candidates.isEmpty()) {
      LOGGER.warn("No sea nodes found in lake graph, converting one at random...");
      var skipCount = terrainSettings.random().nextInt(lakeGraph.getNodes().size() - 1);
      var randomNode =
          lakeGraph.getNodes().stream()
              .skip(skipCount)
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "No sea nodes found in lake graph, and couldn't find a random node to switch."));
      randomNode.switchToSea();
      candidates = List.of(randomNode);
    }
    for (LstNode candidate : candidates) {
      var lakeUpstreamConnections = candidate.outEdges();
      for (LstDirectedEdge lakeUpstreamConnection : lakeUpstreamConnections) {
        var lakeDownstreamConnection = lakeUpstreamConnection.getSym();
        if (lakeDownstreamConnection != null) {
          lakeDownstreamConnection.setInsertOrder(insertNo++);
          lakeBlackRedTree.add(lakeDownstreamConnection);
        }
      }
    }
    while (!lakeBlackRedTree.isEmpty()) {
      var currentDownstreamConnection = lakeBlackRedTree.pollFirst();
      if (lakeGraphTree.contains(currentDownstreamConnection)) {
        continue;
      }
      var added = lakeGraphTree.add(currentDownstreamConnection);
      if (added) {
        var fromNode = currentDownstreamConnection.getFromNode();
        var lakeUpstreamConnections = fromNode.outEdges();
        for (LstDirectedEdge lakeUpstreamConnection : lakeUpstreamConnections) {
          if (lakeUpstreamConnection.equals(currentDownstreamConnection)) {
            continue;
          }
          var lakeDownstreamConnection = lakeUpstreamConnection.getSym();
          if (lakeDownstreamConnection != null
              && !lakeDownstreamConnection.getFromNode().pt().isSea()) {
            lakeDownstreamConnection.setInsertOrder(insertNo++);
            lakeBlackRedTree.add(lakeDownstreamConnection);
          }
        }
      }
    }
  }

  /*
   * Step 2(C): Join the stream trees with the lakes to form the fully connected
   * stream tree.
   */
  void addSaddlesToStreamTree() {
    lakeGraphTree.forEach(
        saddle -> {
          var toNode = saddle.getSaddleNodeTo();
          if (toNode == null) {
            throw new IllegalStateException(
                "Saddle Node B not found in saddle edge from: %s, to: %s"
                    .formatted(
                        saddle.getFromNode().getCoordinate(), saddle.getToNode().getCoordinate()));
          }
          streamTreeGraph.add(saddle.getFromNode().pt(), toNode.pt());
        });
    lakeGraphTree.clear();
    lakeGraph.clear();
  }

  /*
   * Step 3: Determine erosion and slope values for each node, using a combination of the
   * node's catchment area and the catchment areas of upstream nodes for erosion. Once done
   * compute the stream power equation for each node.
   * Also apply a thermal shock heuristic to prevent unrealistically sharp slopes.
   */
  void applyUpliftStreamPowerThermalShock() {
    var newMaxHeight = 0.0d;
    // For each stream tree, calculate the erosion values for each node.

    for (LstNode sink : streamTreeGraph.sinks()) {
      // First build an ordered set of nodes by starting at the sink and traversing the tree
      // breadth-first.
      var orderedNodes = new LinkedList<LstNode>();
      var inboundNodes = sink.inboundNodes();
      var inbound = new LinkedList<>(inboundNodes);
      orderedNodes.add(sink);
      while (!inbound.isEmpty()) {
        var nextNode = inbound.poll();
        var nextInbound = nextNode.inboundNodes();
        inbound.addAll(nextInbound);
        orderedNodes.add(nextNode);
      }
      // Now calculate the erosion values for each node.
      while (!orderedNodes.isEmpty()) {
        var node = orderedNodes.removeLast();
        // KEY: Catchment area
        var drainage = node.inboundNodes().stream().mapToDouble(LstNode::totalCatchmentArea).sum();
        node.setUpstreamCatchmentArea(drainage);
      }

      var nodesToCalculate = new LinkedList<>(sink.inboundNodes());
      // Now calculate the stream power equation for each node.
      var calculatedNodes = new HashSet<LstCoordinate>();
      calculatedNodes.add(sink.getCoordinate());
      while (!nodesToCalculate.isEmpty()) {
        var node = nodesToCalculate.poll();
        var nodeCoordinate = node.getCoordinate();
        var rpGraphNode =
            rpGraph
                .findNode(nodeCoordinate)
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Stream Graph Node %s not found in RPGraph when calculating stream power equation."
                                .formatted(nodeCoordinate)));
        if (!rpGraphNode.pt().isSea()) {
          var downstreamList = node.outEdges();
          if (downstreamList.size() != 1) {
            throw new IllegalStateException(
                "Node %s has %d downstream edges, should have 1."
                    .formatted(nodeCoordinate, downstreamList.size()));
          }
          var downstream = downstreamList.getFirst();
          if (!calculatedNodes.contains(downstream.getToNode().getCoordinate())) {
            throw new IllegalStateException(
                "Downstream node %s not calculated before calculating stream power equation."
                    .formatted(downstream.getFromNode().pt()));
          }
          var downstreamNode =
              rpGraph
                  .findNode(downstream.getToNode().getCoordinate())
                  .orElseThrow(
                      () ->
                          new IllegalStateException(
                              "Stream Graph Downstream Node %s not found in RPGraph when calculating stream power equation."
                                  .formatted(downstream.getFromNode().pt())));

          var newHeight = calculateNewHeight(node, rpGraphNode, downstreamNode);
          var lowestNeighbour =
              rpGraphNode.inboundNodes().stream()
                  .min(Comparator.comparingDouble(LstNode::height))
                  .orElseThrow();
          var lnLength = rpGraphNode.getCoordinate().distanceFrom(lowestNeighbour.getCoordinate());
          var isEdgeNode = lowestNeighbour.pt().isSea();
          if (isEdgeNode) {
            lnLength = 1.0d;
          }
          var angle = Math.atan2(newHeight - lowestNeighbour.height(), lnLength);
          newHeight =
              applyThermalShockHeuristicPredetermined(
                  angle, newHeight, lowestNeighbour.height(), lnLength, rpGraphNode.maxSlope());
          rpGraphNode.setHeight(newHeight);
          newMaxHeight = Math.max(newMaxHeight, newHeight);
        }
        calculatedNodes.add(nodeCoordinate);
        node.inboundNodes().forEach(nodesToCalculate::push);
      }
    }
    setMaxHeight(newMaxHeight);
  }

  private double calculateNewHeight(
      LstNode streamTreeNode, LstNode rpGraphNode, LstNode downstreamNode) {
    var drainage = streamTreeNode.totalCatchmentArea();
    var dsl = rpGraphNode.pt().distanceFrom(downstreamNode.pt());
    var uplift = rpGraphNode.uplift();
    // KEY: Stream Power Equation
    var kDrainageOverDsl = (geologySettings.k() * Math.pow(drainage, geologySettings.m())) / dsl;
    return (rpGraphNode.height()
            + (geologySettings.deltaT() * (uplift + (kDrainageOverDsl * downstreamNode.height()))))
        / (1 + (geologySettings.deltaT() * kDrainageOverDsl));
  }

  /**
   * Apply a thermal shock heuristic using a predetermined max slope.
   *
   * @param angleRadians Angle to apply heuristic to
   * @param height Height of the current node
   * @param downstreamHeight Height of the lowest downstream node
   * @param length Length of the edge between the current and downstream node
   * @param slopeNoise The predetermined slope to use for the heuristic
   * @return The new height of the current node after applying the heuristic
   */
  public double applyThermalShockHeuristicPredetermined(
      double angleRadians,
      double height,
      double downstreamHeight,
      double length,
      double slopeNoise) {
    if (height <= downstreamHeight) {
      return height;
    }
    var max_slope =
        GenerationUtils.lerp(
            geologySettings.minSlopeRadians(), geologySettings.maxSlopeRadians(), slopeNoise);
    if (angleRadians > max_slope) {
      return downstreamHeight + (length * Math.tan(max_slope));
    }
    return height;
  }

  /**
   * Generate useful final terrain data using parts of the Large Scale Terrain Generation algorithm.
   * This will compute the stream tree, build the lake graph, and compute the lake tree only.
   *
   * @param stage A consumer of stage messages.
   */
  public void postGenerationStep(Consumer<String> stage) {
    stage.accept("Partial Generation Step");
    streamTreeGraph.clear();
    computeStreamTree();
    buildLakeGraph();
    computeLakeTree();
  }
}
