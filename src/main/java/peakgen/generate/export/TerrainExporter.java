package peakgen.generate.export;

import peakgen.generate.LargeScaleTerrainGenerator;
import peakgen.generate.util.GenerationUtils;

import java.io.File;
import java.io.IOException;

/**
 * Interface for exporting terrain data to some other format to enable using the generated terrain
 * in other applications.
 */
public interface TerrainExporter {

  /**
   * Exports the terrain data to a file format that can be used in other applications. The path
   * should be a directory where the terrain data can be exported, and the output may consist of
   * one or more files. Validation of the path is the responsibility of the implementing class,
   * but the {@link GenerationUtils#validateDir(String)} method can be used to help with this.
   *
   * @param generator the generator that is maintaining the terrain data
   * @param path the directory path to export the terrain data to
   * @throws IOException if an I/O error occurs during the export process
   */
  void export(LargeScaleTerrainGenerator generator, String path) throws IOException;

  /**
   * Takes a string path and returns a {@link File} object representing the target directory for the
   * export operation. The default implementation should suffice for most cases.
   *
   * @param path the path to the target directory for the export operation
   * @return the target directory for the export operation as a {@link File} object
   *
   * @throws IllegalArgumentException if the path could not be validated
   */
  default File getTargetDir(String path) {
    return GenerationUtils.validateDir(path);
  }
}
