package peakgen.generate.topology;

import javax.vecmath.Point2d;

import static peakgen.generate.util.GenerationUtils.PRECISION_MODEL;

/**
 * A coordinate with additional data for LST generation.
 */
public final class LstCoordinate implements Comparable<LstCoordinate> {
  public final double x;
  public final double y;
  final Point2d point;
  public final double seaFactor;
  public final double upliftFactor;
  public final double slopeFactor;

  /**
   * Create a new LstCoordinate based on an existing coordinate, constraining it to a maximum size.
   * The data is generated based on the original coordinate location, and includes factors rather
   * than actual values, which are translated into geological values based on settings as part
   * of the LST generation.
   *
   * @param x            The x-coordinate of the point
   * @param y            The y-coordinate of the point
   * @param seaFactor    Less than 0 if the point is in the sea. A sea point will act as a river sink and
   *                     will not be uplifted.
   * @param upliftFactor The factor for uplift, larger values means faster uplift (e.g. due to
   *                     tectonic activity).
   * @param slopeFactor  The factor for slope, how dramatically the terrain can slope. This should
   *                     ideally be based on the thermal shock rating of the terrain.
   */
  public LstCoordinate(double x, double y, double seaFactor, double upliftFactor, double slopeFactor) {
    this.x = x;
    this.y = y;
    this.point = new Point2d(x, y);
    this.seaFactor = seaFactor;
    this.upliftFactor = upliftFactor;
    this.slopeFactor = slopeFactor;
  }

  /**
   * Create a new LstCoordinate based on an existing coordinate, constraining it to a maximum size.
   * The data is generated based on the original coordinate location and includes factors rather
   * than actual values, which are translated into geological values based on settings as part
   * of the LST generation.
   *
   * @param x            The x-coordinate of the point
   * @param y            The y-coordinate of the point
   * @param maxSize      The maximum size of the terrain map, used as a convenient way to ensure the x and y
   *                     values are not outside this boundary square by clamping the absolute value of x and
   *                     y to the maximum size. Will be ignored if less than or equal to 0.
   * @param seaFactor    Less than 0 if the point is in the sea. A sea point will act as a river sink and
   *                     will not be uplifted.
   * @param upliftFactor The factor for uplift, larger values means faster uplift (e.g. due to
   *                     tectonic activity).
   * @param slopeFactor  The factor for slope, how dramatically the terrain can slope. This should
   *                     ideally be based on the thermal shock rating of the terrain.
   */
  public LstCoordinate(double x, double y, int maxSize, double seaFactor, double upliftFactor, double slopeFactor) {
    if (maxSize > 0) {
      if (x > maxSize) {
        x = maxSize;
      }
      if (y > maxSize) {
        y = maxSize;
      }
      if (x < -maxSize) {
        x = -maxSize;
      }
      if (y < -maxSize) {
        y = -maxSize;
      }
    }
    this.x = x;
    this.y = y;
    this.point = new Point2d(x, y);
    this.seaFactor = seaFactor;
    this.upliftFactor = upliftFactor;
    this.slopeFactor = slopeFactor;
  }

  /**
   * A pass-through method to get an indication of whether the point's sea factor is below 0 (true) or not (false).
   *
   * @return True if the point is in the sea, false otherwise.
   */
  public boolean isSea() {
    return seaFactor <= 0.0d;
  }

  /**
   * A pass-through method to get the uplift factor (between 0 and 1) for the point. This is used
   * together with the geology settings to determine how much the point is uplifted per year.
   *
   * @return The uplift factor for the point (between 0 and 1)
   */
  public double upliftFactor() {
    return upliftFactor;
  }

  /**
   * A pass-through method to get the slope factor (between 0 and 1) for the point. This is used
   * together with the geology settings to determine the maximum slope allowed for this point compared
   * to its neighbors.
   *
   * @return The slope factor for the point between 0 and 1
   */
  public double slopeFactor() {
    return slopeFactor;
  }

  public double distanceFrom(LstCoordinate other) {
    return point.distance(other.point);
  }

  @Override
  public int compareTo(LstCoordinate other) {
    var xComp = PRECISION_MODEL.compare(x, other.x);
    return xComp == 0 ? PRECISION_MODEL.compare(y, other.y) : xComp;
  }

  @Override public boolean equals(Object other) {
    if (other instanceof LstCoordinate lstc) {
      return point.equals(lstc.point);
    }
    return false;
  }

  @Override public int hashCode() {
    return point.hashCode();
  }

  @Override public LstCoordinate clone() {
    return new LstCoordinate(x, y, seaFactor, upliftFactor, slopeFactor);
  }

  @Override public String toString() {
    return String.format("%s/%f/%f/%f", super.toString(), seaFactor, upliftFactor, slopeFactor);
  }
}
