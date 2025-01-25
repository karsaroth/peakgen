package peakgen.generate;

/**
 * Settings that define global geological constants for the simulation.
 *
 * @param deltaT The time step for the simulation in years.
 * @param maxU The maximum uplift rate (m/y).
 * @param minU The minimum uplift rate (m/y).
 * @param k The erosion coefficient.
 * @param m One of the two positive constants in a stream power law. The other (n) is assumed to be
 *     1 to ease computation.
 * @param maxSlopeRadians The maximum allowed slope in radians. This will be the largest possible
 *     slope that can be generated when point settings contain the maximum slope factor.
 * @param minSlopeRadians The minimum slope in radians. This will be the smallest possible slope
 *     that can be generated when point settings contain the minimum slope factor.
 */
public record GeologySettings(
    double deltaT,
    double maxU,
    double minU,
    double k,
    double m,
    double maxSlopeRadians,
    double minSlopeRadians) {
  public static final double DELTA_T = 250000.0; // 250,000 years per step for fast convergence.
  public static final double MAX_U =
      5.01e-4; // 5.01 * 10^-4 per year = average uplift for earth mountains
  public static final double MIN_U = 0.0;
  public static final double K =
      5.61e-7; // 5.6110 * 10^-7 erosion rate = max height of mountains around 2km.
  public static final double M = 0.5; // M = 0.5 is common in geomorphology.
  public static final double MAX_SLOPE_RADIANS = Math.toRadians(58);
  public static final double MIN_SLOPE_RADIANS = Math.toRadians(6);
  public static final GeologySettings DEFAULT = new GeologySettings();

  /**
   * A set of default geological settings based on the original paper. Slope values in this case are
   * assumed to be based on noise, and if so should produce mountains that are not unrealistically
   * jagged.
   */
  public GeologySettings() {
    this(DELTA_T, MAX_U, MIN_U, K, M, MAX_SLOPE_RADIANS, MIN_SLOPE_RADIANS);
  }

  /**
   * Get the estimated maximum height of a mountain given the maximum uplift rate. This is based on
   * the equation: H = 2.244 * (U / K) where H is the height of the mountain, U is the uplift rate,
   * and K is the erosion coefficient.
   *
   * @return The estimated maximum height of a mountain.
   */
  public double estimatedMaxHeight() {
    return 2.244d * (maxU / k);
  }
}
