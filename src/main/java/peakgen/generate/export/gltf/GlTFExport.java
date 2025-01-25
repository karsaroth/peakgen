package peakgen.generate.export.gltf;

import io.github.chadj2.mesh.MeshGltfWriter;
import io.github.chadj2.mesh.MeshGltfWriter.GltfFormat;
import io.github.chadj2.mesh.MeshVertex;
import io.github.chadj2.mesh.TreeBuilder;
import io.github.chadj2.mesh.TriangleBuilder;
import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.euclidean.threed.line.Segment3D;
import org.apache.commons.geometry.euclidean.threed.mesh.SimpleTriangleMesh;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import peakgen.generate.LargeScaleTerrainGenerator;
import peakgen.generate.export.TerrainExporter;

import javax.vecmath.Point3f;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Export terrain and stream data to glTF 2.0 format. The resulting files will contain the terrain
 * or stream data, but is not textured or coloured in any way beyond the default glTF material. The
 * triangle and line vertex locations will be mapped as-is, and by default will represent their
 * 3-dimensional distance from the generator's origin point (0,0,0) in meters. Additionally, the
 * terrain will include the full graph, including all sea nodes, and the sea nodes will have their
 * height mapped from sea level 0.0 to -1500.0 meters, depending on the original LstSettings data
 * values (the values will normally be between 0.0 and -1.0).
 */
public final class GlTFExport implements TerrainExporter {
  public static final String RPG_NAME = "terrain";
  public static final String STREAMS_NAME = "streams";
  static final Logger LOGGER = LoggerFactory.getLogger(GlTFExport.class);
  final Supplier<MeshGltfWriter> writerSupplier;

  public GlTFExport() {
    this.writerSupplier = MeshGltfWriter::new;
  }

  public GlTFExport(Supplier<MeshGltfWriter> writerSupplier) {
    this.writerSupplier = writerSupplier;
  }

  @Override public void export(LargeScaleTerrainGenerator generator, String path) throws IOException {
    var dir = getTargetDir(path);
    exportTriangles(generator.generateTriangularMesh(), dir);
    exportLines(generator.generateStreamTreeCollection(), dir);
  }

  void exportTriangles(SimpleTriangleMesh mesh, File dir) throws IOException {
    var writer = writerSupplier.get();
    writer.setBasePath(dir);
    HashMap<Vector3D, MeshVertex> vertexMap = new HashMap<>();
    var sceneFile = new File(dir, RPG_NAME + "." + GltfFormat.glb.name());
    var triangleBuilder = new TriangleBuilder(RPG_NAME, false);

    LOGGER.info("Exporting {} triangles to {}", mesh.getFaceCount(), dir);
    mesh.faces().forEach(f -> {
      var vertices = f.getVertices()
          .stream()
          .map(v -> vertexMap.computeIfAbsent(v, k -> {
            try {
              return triangleBuilder.newVertex(new Point3f((float) k.getX(), (float) k.getZ(), (float) k.getY()));
            }
            catch (Exception e) {
              throw new RuntimeException(e);
            }
          }))
          .toList();
      triangleBuilder.addTriangle(vertices.get(0), vertices.get(1), vertices.get(2));
    });
    try {
      if (!triangleBuilder.isEmpty()) {
        try {
          triangleBuilder.build(writer);
        }
        catch (Exception e) {
          throw new IOException(e);
        }
      }
      LOGGER.info("Building and writing terrain glTF file");
      writer.writeGltf(sceneFile);
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }

  void exportLines(Collection<Segment3D> lines, File dir) throws IOException {
    var writer = writerSupplier.get();
    writer.setBasePath(dir);
    HashMap<Vector3D, MeshVertex> vertexMap = new HashMap<>();
    var sceneFile = new File(dir, STREAMS_NAME + "." + GltfFormat.glb.name());
    var lineBuilder = new TreeBuilder(STREAMS_NAME);
    final Function<Vector3D, MeshVertex> vertexMapper = v -> {
      try {
        return lineBuilder.newVertex(new Point3f((float) v.getX(), (float) v.getZ(), (float) v.getY()));
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    };

    LOGGER.info("Exporting lines to {}", dir);
    lines.forEach(segment3D -> {
      var start = vertexMap.computeIfAbsent(segment3D.getStartPoint(), vertexMapper);
      var end = vertexMap.computeIfAbsent(segment3D.getEndPoint(), vertexMapper);
      lineBuilder.addLine(start, end);
    });
    try {
      if (!lineBuilder.isEmpty()) {
        try {
          lineBuilder.build(writer);
        }
        catch (Exception e) {
          throw new IOException(e);
        }
      }
      LOGGER.info("Building and writing stream glTF file");
      writer.writeGltf(sceneFile);
    }
    catch (Exception e) {
      throw new IOException(e);
    }
  }
}
