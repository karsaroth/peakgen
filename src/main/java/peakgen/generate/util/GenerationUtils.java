package peakgen.generate.util;

import org.apache.commons.numbers.core.Precision;
import org.apache.commons.numbers.core.Precision.DoubleEquivalence;
import peakgen.generate.topology.LstCoordinate;

import java.io.File;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class GenerationUtils {
  public static final DoubleEquivalence PRECISION_MODEL = Precision.doubleEquivalenceOfEpsilon(1.0e-6d);

  /**
   * Validates a directory path.
   *
   * @param path the path to validate
   * @return the directory
   */
  public static File validateDir(String path) {
    File dir = new File(path);
    Tuples.T2<Boolean, String> result = Tuples.T2.of(true, "");
    result = attempt(result, "%s is not a directory".formatted(path), dir::isDirectory);
    result = attempt(result, "Could not create directory %s".formatted(path),
        () -> dir.exists() || dir.mkdirs());
    result = attempt(result, "Could not write to directory %s".formatted(path), dir::canWrite);
    if (!result.first()) {
      throw new IllegalArgumentException(result.second());
    }
    return dir;
  }

  public static Tuples.T2<Boolean, String> attempt(
      Tuples.T2<Boolean, String> previous, String what, Supplier<Boolean> s) {
    if (previous.first()) {
      var result = s.get();
      return Tuples.T2.of(result, result ? previous.second() : previous.second() + "\n" + what);
    } else {
      return previous;
    }
  }

  /**
   * Find any points in between two coordinates that are in the sea.
   * @param a The first coordinate
   * @param b The second coordinate
   * @param numSamples The number of samples to take between the two coordinates
   * @param dataFunction The function to get the sea data from
   * @return True if any of the points are in the sea, false otherwise
   */
  public static boolean sampleForSea(
      LstCoordinate a, LstCoordinate b, int numSamples, BiFunction<Double, Double, LstCoordinate> dataFunction) {
    if (numSamples < 3) {
      return false;
    }
    double step = 1.0 / (numSamples - 1); // Calculate the step size
    boolean isSea = false;

    // Skip the first and last points, we know their values.
    for (int i = 1; i < numSamples; i++) {
      double t = i * step; // Calculate the interpolation point
      double x = lerp(a.x, b.x, t); // Interpolate the x-coordinate
      double y = lerp(a.y, b.y, t); // Interpolate the y-coordinate
      var data = dataFunction.apply(x, y);
      if (data.isSea()) {
        isSea = true;
        break;
      }
    }
    return isSea;
  }

  /**
   * Linearly interpolate between two values.
   *
   * @param start The start value
   * @param end The end value
   * @param interpolationPoint The interpolation point
   * @return The interpolated value
   */
  public static double lerp(double start, double end, double interpolationPoint) {
    return start + interpolationPoint * (end - start);
  }

  /**
   * Calculate the hash of a point to help link two points together in a map.
   *
   * @param x The x-coordinate of the point
   * @param y The y-coordinate of the point
   * @return The hash of the point
   */
  public static int pointHash(double x, double y) {
    long bits = 1L;
    bits = 31L * bits + ((x == 0.0) ? 0L : Double.doubleToLongBits(x));
    bits = 31L * bits + ((y == 0.0) ? 0L : Double.doubleToLongBits(y));
    return (int)(bits ^ bits >> 32);
  }
}
