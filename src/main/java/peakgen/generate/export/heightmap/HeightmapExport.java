package peakgen.generate.export.heightmap;

import org.apache.commons.geometry.euclidean.threed.Triangle3D;
import org.apache.commons.geometry.euclidean.threed.Vector3D;
import org.apache.commons.geometry.euclidean.threed.line.Lines3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import peakgen.generate.LargeScaleTerrainGenerator;
import peakgen.generate.export.TerrainExporter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static peakgen.generate.util.GenerationUtils.*;

public final class HeightmapExport implements TerrainExporter {
  static final Logger LOGGER = LoggerFactory.getLogger(HeightmapExport.class);
  final double minHeight;
  final int size;
  final String fileName;
  double maxHeight;
  double diff;
  double sizeDiff;
  int halfSize;

  /**
   * Simple constructor, with minimal configuration
   * @param minHeight The minimum height, this will match to full black in the heightmap.
   * @param size The size that the heightmap will be generated (a square)
   * @param fileName The filename of the heightmap
   */
  public HeightmapExport(
      double minHeight, int size, String fileName
  ) {
    this.minHeight = minHeight;
    this.size = size;
    this.fileName = fileName;
  }

  @Override public void export(LargeScaleTerrainGenerator generator, String path) throws IOException {
    File pathFile = this.getTargetDir(path);
    File toCreate = verifyFileForWriting(pathFile, fileName);
    String formatExt = getFormatExtension(toCreate).toLowerCase();
    if (Arrays
        .stream(ImageIO.getWriterFileSuffixes())
        .noneMatch(f -> f.equals(formatExt))) {
      throw new RuntimeException("Unable to write to %s file, no writers available for this format".formatted(formatExt));
    }
    int size = generator
        .getTerrainSettings()
        .size();
    this.halfSize = size / 2;
    this.sizeDiff = convertSize(size, this.size);
    BufferedImage image = new BufferedImage(this.size, this.size, BufferedImage.TYPE_INT_ARGB);
    this.maxHeight = generator.getMaxHeight();
    this.diff = -minHeight;
    var mesh = generator.generateTriangularMesh();
    var down = Vector3D.of(0, 0, -1);
    LOGGER.info("Rasterizing {} triangles...", mesh.getFaceCount());
    for (var face: mesh.faces()) {
      var triangle = face.getPolygon();
      var bounds = triangle.getBounds();
      var max = bounds.getMax();
      var min = bounds.getMin();
      var maxX = (int)Math.ceil(max.getX());
      var maxY = (int)Math.ceil(max.getY());
      var minX = (int)Math.floor(min.getX());
      var minY = (int)Math.floor(min.getY());
      int lastPixelX = -1;
      int lastPixelY = -1;
      for (int x = minX; x <= maxX; x++) {
        int thisPixelX = (int)Math.floor((double)(x + halfSize) / sizeDiff);
        if (thisPixelX == lastPixelX || thisPixelX >= this.size) continue;
        for (int y = minY; y <= maxY; y++) {
          int thisPixelY = (int)Math.floor((double)(y + halfSize) / sizeDiff);
          if (thisPixelY == lastPixelY || thisPixelY >= this.size) continue;
          var point = Vector3D.of(x, y, this.maxHeight);
          if (inside(triangle, point)) {
            var intersection = triangle.intersection(Lines3D.fromPointAndDirection(
                point, down, PRECISION_MODEL
            ));
            if (intersection != null) {
              var pixel = convertHeight(intersection.getZ());
              image.setRGB(thisPixelX, thisPixelY, pixel);
              lastPixelX = thisPixelX;
              lastPixelY = thisPixelY;
            }
          }
        }
        lastPixelY = -1;
      }
    }
    LOGGER.info("Writing image to file {} using {} writer", toCreate, formatExt);
    ImageIO.write(image, formatExt, toCreate);
  }

  String getFormatExtension(File toCreate) {
    var name = toCreate.getName();
    var periodIndex = toCreate
        .getName()
        .lastIndexOf(".");
    if (periodIndex != -1) {
      return name.substring(periodIndex + 1);
    } else {
      return "png";
    }
  }

  double convertSize(int genSize, int imageSize) {
    return (double) genSize / (double) imageSize;
  }

  int convertHeight(double height) {
    var clampedHeight = Math.min(Math.max(height, minHeight), maxHeight);
    var rgbFactor = (clampedHeight - minHeight) / (maxHeight - minHeight);
    var rgb = (int)Math.round(lerp(0, 255, rgbFactor));
    // set the pixel value
    return (255 << 24) | (rgb << 16) | (rgb << 8) | rgb;
  }

  boolean inside(Triangle3D t, Vector3D p) {
    var abp = edgeFunction(t.getPoint1(), t.getPoint2(), p);
    var bcp = edgeFunction(t.getPoint2(), t.getPoint3(), p);
    var cap = edgeFunction(t.getPoint3(), t.getPoint1(), p);
    return abp <= 0 && bcp <= 0 && cap <= 0;
  }

  double edgeFunction(Vector3D a, Vector3D b, Vector3D c) {
    return (b.getX() - a.getX()) * (c.getY() - a.getY()) - (b.getY() - a.getY()) * (c.getX() - a.getX());
  }
}
