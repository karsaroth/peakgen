package peakgen.generate.settings;

import peakgen.generate.topology.LstCoordinate;

import java.util.Random;

/**
 * Interface for settings used by the LST generator. It provides data for each point based on some
 * scalable data source (the points requested will include some random variation).
 */
public interface LstSettings {
  /**
   * Get the related data for a point, if it is sea or land, geological uplift, etc.
   *
   * @param x       The x-coordinate of the point.
   * @param y       The y-coordinate of the point.
   * @param maxSize If greater than 0, will clamp the absolute value of x and y to this value.
   * @return The data for the point.
   */
  LstCoordinate getData(double x, double y, int maxSize);

  /**
   * The size of the terrain map to be generated. The map will be a square, centered at the origin
   * (0, 0) with a side length of size. Note depending on where the data is used, terrain may be
   * further clipped to a circle.
   *
   * @return The size of the terrain map in meters. This value squared is the total area of the map.
   */
  int size();

  /**
   * The level of detail for the terrain map. This is the number of points per side of the map. The
   * points chosen will be semi-randomly distributed across the map. Since terrain generation
   * ignores sea points, the actual number of points generated will be less than this value in most
   * cases.
   *
   * @return The level of detail for the terrain map.
   */
  int lod();

  /**
   * The random number generator used for generating terrain. This is used to generate the random
   * variation in the selected points.
   *
   * @return The random number generator.
   */
  Random random();

  /**
   * The seed used for the random number generator. This should be used to initialize the random
   * number generator, so that the same terrain can be generated multiple times.
   */
  long seed();
}
