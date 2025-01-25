package peakgen.generate;

import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.euclidean.threed.mesh.TriangleMesh.Face;
import org.tinfour.common.SimpleTriangle;
import org.tinfour.common.Vertex;
import peakgen.generate.topology.LstCoordinate;
import peakgen.generate.topology.LstNode;

import java.util.stream.Collectors;

import static peakgen.generate.GeologySettings.DEFAULT;
import static peakgen.generate.util.GenerationUtils.lerp;

public class TestUtil {

  public static LstNode lstn(double x, double y) {
    return lstn(x, y, false, DEFAULT);
  }

  public static LstNode lstn(double x, double y, boolean sea, GeologySettings gs) {
    var c = (sea) ? lstsea(x, y) : lstc(x, y);
    return lstn(c, gs);
  }

  public static LstCoordinate lstsea(double x, double y) {
    return new LstCoordinate(x, y, 0.0d, 0.0d, 0.0d);
  }

  public static LstCoordinate lstc(double x, double y) {
    return new LstCoordinate(x, y, 0.5d, 0.5d, 0.3d);
  }

  public static LstNode lstn(LstCoordinate c, GeologySettings gs) {
    if (c.isSea()) {
      return new LstNode(c, 0.0, 0.0, 0.0, -1, 0.0);
    }
    return new LstNode(c, lerp(gs.minU(), gs.maxU(), c.upliftFactor()), 0.0, 1, -1, c.slopeFactor());
  }

  public static LstNode lstn(double x, double y, boolean sea) {
    return lstn(x, y, sea, DEFAULT);
  }

  public static LstNode lstn(LstCoordinate c) {
    return lstn(c, DEFAULT);
  }

  public static Vertex c(double x, double y) {
    return new Vertex(x, y, 0.0d);
  }

  public static boolean faceIsTriangle(Face f, SimpleTriangle t) {
    return f
        .getVertices()
        .stream()
        .anyMatch(vector -> vectorIsVertex(vector, t.getVertexA()) ||
            vectorIsVertex(vector, t.getVertexB()) ||
            vectorIsVertex(vector, t.getVertexC()));

  }

  public static boolean vectorIsVertex(Vector3D vector, Vertex vertex) {
    return vector.getX() == vertex.getX() && vector.getY() == vertex.getY();
  }

  public static String verticesToString(Face f) {
    return f.getVertices().stream().map(Vector3D::toString).collect(Collectors.joining(", "));
  }

}
