package peakgen.generate.settings;

import peakgen.generate.topology.LstCoordinate;
import peakgen.generate.util.OpenSimplex2;

import java.util.Random;

/**
 * Generates terrain data using OpenSimplex noise. This is fairly simple and naive, but it will
 * generate some mildly interesting terrain without too much effort.
 * <br>
 * A maximum of three values are generated for each point, these are:<br>
 * - Sea: Constrained to -1.0 to 1.0 <br>
 * - Uplift: Constrained to 0.0 to 1.0 <br>
 * - Slope: Constrained to 0.0 to 1.0 <br>
 * <br>
 *
 * It's possible to broaden flatter or steeper terrain by setting higher or lower values than the
 * constraints, but the actual return values will be constrained to the above ranges.
 */
public final class SimplexNoiseLstSettings implements LstSettings {
  public final static double DEFAULT_SEA_PERSISTENCE = 0.7d;
  public final static double DEFAULT_SEA_LOW = -0.6d;
  public final static double DEFAULT_SEA_HIGH = 1.0d;
  public final static boolean DEFAULT_UPLIFT_FROM_SEA = false;
  public final static double DEFAULT_UPLIFT_PERSISTENCE = 0.7d;
  public final static double DEFAULT_UPLIFT_LOW = -0.8d;
  public final static double DEFAULT_UPLIFT_HIGH = 1.0d;
  public final static boolean DEFAULT_SLOPE_FROM_SEA = false;
  public final static boolean DEFAULT_SLOPE_FROM_UPLIFT = true;
  public final static double DEFAULT_SLOPE_PERSISTENCE = 0.1d;
  public final static double DEFAULT_SLOPE_LOW = 0.0d;
  public final static double DEFAULT_SLOPE_HIGH = 1.0d;

  final int size;
  final int lod;
  final int landMaxRadius;
  final Random random;
  final long seed;
  final int octaves;
  final int seaShiftX;
  final int seaShiftY;
  final int upliftShiftX;
  final int upliftShiftY;
  final int slopeShiftX;
  final int slopeShiftY;
  final double seaPersistence;
  final double seaScale;
  final double seaLow;
  final double seaHigh;
  final boolean upliftFromSea;
  final double upliftPersistence;
  final double upliftScale;
  final double upliftLow;
  final double upliftHigh;
  final boolean slopeFromSea;
  final boolean slopeFromUplift;
  final double slopePersistence;
  final double slopeScale;
  final double slopeLow;
  final double slopeHigh;

  /**
   * Create a new SimplexNoiseLstSettings object with the default settings.
   *
   * @param size The size of the terrain map.
   * @param lod The level of detail for the terrain map.
   * @param landMaxRadius The maximum radius of the land.
   * @param random The random number generator to use.
   * @param seed The seed for the noise generator.
   */
  public SimplexNoiseLstSettings(int size, int lod, int landMaxRadius, Random random, long seed) {
    this.size = size;
    this.octaves = (int) Math.round(Math.log(size) / Math.log(2));
    this.lod = lod;
    this.landMaxRadius = landMaxRadius;
    this.random = random;
    this.seaShiftX = random.nextInt(-size, size);
    this.seaShiftY = random.nextInt(-size, size);
    this.upliftShiftX = random.nextInt(-size, size);
    this.upliftShiftY = random.nextInt(-size, size);
    this.slopeShiftX = random.nextInt(-size, size);
    this.slopeShiftY = random.nextInt(-size, size);
    this.seed = seed;
    this.seaPersistence = DEFAULT_SEA_PERSISTENCE;
    this.seaScale = 1.0d / (double) size;
    this.seaLow = DEFAULT_SEA_LOW;
    this.seaHigh = DEFAULT_SEA_HIGH;
    this.upliftFromSea = DEFAULT_UPLIFT_FROM_SEA;
    this.upliftPersistence = DEFAULT_UPLIFT_PERSISTENCE;
    this.upliftScale = 2.0d / (double) size;
    this.upliftLow = DEFAULT_UPLIFT_LOW;
    this.upliftHigh = DEFAULT_UPLIFT_HIGH;
    this.slopeFromSea = DEFAULT_SLOPE_FROM_SEA;
    this.slopeFromUplift = DEFAULT_SLOPE_FROM_UPLIFT;
    this.slopePersistence = DEFAULT_SLOPE_PERSISTENCE;
    this.slopeScale = 3.0d / (double) size;
    this.slopeLow = DEFAULT_SLOPE_LOW;
    this.slopeHigh = DEFAULT_SLOPE_HIGH;
  }

  /**
   * Create a new SimplexNoiseLstSettings object with the specified settings.
   * @param size The size of the terrain map.
   * @param lod The level of detail for the terrain map.
   * @param landMaxRadius The maximum radius of the land.
   * @param random A random number generator associated with the settings. This is not used within this fully configured implementation.
   * @param seed The seed for the noise generator.
   * @param octaves The number of octaves to use for the noise.
   * @param seaShiftX The x shift for the sea noise.
   * @param seaShiftY The y shift for the sea noise.
   * @param upliftShiftX The x shift for the uplift noise.
   * @param upliftShiftY The y shift for the uplift noise.
   * @param slopeShiftX The x shift for the slope noise.
   * @param slopeShiftY The y shift for the slope noise.
   * @param seaPersistence The persistence of the sea noise.
   * @param seaScale The scale of the sea noise.
   * @param seaLow The lowest allowed value of the sea noise.
   * @param seaHigh The highest allowed value of the sea noise.
   * @param upliftFromSea Whether the uplift should be based on the sea noise (i.e. Added to it).
   * @param upliftPersistence The persistence of the uplift noise.
   * @param upliftScale The scale of the uplift noise.
   * @param upliftLow The lowest allowed value of the uplift noise.
   * @param upliftHigh The highest allowed value of the uplift noise.
   * @param slopeFromSea Whether the slope should be based on the sea noise (i.e. Added to it).
   * @param slopeFromUplift Whether the slope should be based on the uplift noise (i.e. Added to it).
   * @param slopePersistence The persistence of the slope noise.
   * @param slopeScale The scale of the slope noise.
   * @param slopeLow The lowest allowed value of the slope noise.
   * @param slopeHigh The highest allowed value of the slope noise.
   */
  public SimplexNoiseLstSettings(
      int size,
      int lod,
      int landMaxRadius,
      Random random,
      long seed,
      int octaves,
      int seaShiftX,
      int seaShiftY,
      int upliftShiftX,
      int upliftShiftY,
      int slopeShiftX,
      int slopeShiftY,
      double seaPersistence,
      double seaScale,
      double seaLow,
      double seaHigh,
      boolean upliftFromSea,
      double upliftPersistence,
      double upliftScale,
      double upliftLow,
      double upliftHigh,
      boolean slopeFromSea,
      boolean slopeFromUplift,
      double slopePersistence,
      double slopeScale,
      double slopeLow,
      double slopeHigh) {
    this.size = size;
    this.lod = lod;
    this.landMaxRadius = landMaxRadius;
    this.random = random;
    this.seed = seed;
    this.octaves = octaves;
    this.seaShiftX = seaShiftX;
    this.seaShiftY = seaShiftY;
    this.upliftShiftX = upliftShiftX;
    this.upliftShiftY = upliftShiftY;
    this.slopeShiftX = slopeShiftX;
    this.slopeShiftY = slopeShiftY;
    this.seaPersistence = seaPersistence;
    this.seaScale = seaScale;
    this.seaLow = seaLow;
    this.seaHigh = seaHigh;
    this.upliftFromSea = upliftFromSea;
    this.upliftPersistence = upliftPersistence;
    this.upliftScale = upliftScale;
    this.upliftLow = upliftLow;
    this.upliftHigh = upliftHigh;
    this.slopeFromSea = slopeFromSea;
    this.slopeFromUplift = slopeFromUplift;
    this.slopePersistence = slopePersistence;
    this.slopeScale = slopeScale;
    this.slopeLow = slopeLow;
    this.slopeHigh = slopeHigh;
  }

  @Override
  public LstCoordinate getData(double x, double y, int maxSize) {
    var firstVal = seaData(x, y);
    var secondVal = 0.0d;
    var thirdVal = 0.0d;
    if (firstVal > 0.0d) {
      secondVal = upliftData(x, y, firstVal);
      thirdVal = slopeData(x, y, firstVal, secondVal);
    }
    return new LstCoordinate(x, y, maxSize, firstVal, secondVal, thirdVal);
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public int lod() {
    return lod;
  }

  public int landMaxRadius() {
    return landMaxRadius;
  }

  @Override
  public Random random() {
    return random;
  }

  @Override
  public long seed() {
    return seed;
  }

  /**
   * Generate the sea data for a point, this data is more iterative so that it can be used directly
   * for the underlying terrain.
   *
   * @param x The x coordinate of the point.
   * @param y The y coordinate of the point.
   * @return The sea data for the point.
   */
  public double seaData(double x, double y) {
    return Math.clamp(
        sumOctave(x + seaShiftX, y + seaShiftY, seaPersistence, seaScale, seaLow, seaHigh)
            - continentalGradient(x, y),
        -1.0d,
        1.0d);
  }

  /**
   * Generate the uplift data for a point, based on the sea (height) data and some more noise.
   *
   * @param seaVal The sea data value for the point.
   * @return The uplift data for the point.
   */
  public double upliftData(double x, double y, double seaVal) {
    var start = (upliftFromSea) ? seaVal : 0.0d;
    return Math.clamp(
        start + sumOctave(x + upliftShiftX, y + upliftShiftY, upliftPersistence, upliftScale, upliftLow, upliftHigh),
        0.0005d,
        1.0d);
  }

  /**
   * Generate the slope data for a point, it should generate fairly dramatic mountain ranges without
   * them being fat.
   *
   * @return The slope data for the point.
   */
  public double slopeData(double x, double y, double seaVal, double upliftVal) {
    var start = (slopeFromSea) ? seaVal : (slopeFromUplift) ? upliftVal : 0.0d;
    return Math.clamp(
        start + sumOctave(x + slopeShiftX, y + slopeShiftY, slopePersistence, slopeScale, slopeHigh, slopeLow),
        0.0d,
        1.0d
    );
  }

  /**
   * Generate a sum of octaves of noise. This is a fairly standard way to generate noise, and it is
   * used here to generate the terrain data.
   *
   * @param x The x coordinate of the point.
   * @param y The y coordinate of the point.
   * @param persistence The persistence of the noise.
   * @param scale The scale of the noise.
   * @param low The low value of the noise.
   * @param high The high value of the noise.
   * @return The sum of the octaves of noise.
   */
  public double sumOctave(
      double x, double y, double persistence, double scale, double low, double high) {
    double maxAmp = 0;
    double amp = 1;
    double freq = scale;
    double noise = 0;

    // add successively smaller, higher-frequency terms
    for (int i = 0; i < octaves; ++i) {
      noise += OpenSimplex2.noise2(seed, x * freq, y * freq) * amp;
      maxAmp += amp;
      amp *= persistence;
      freq *= 2;
    }

    // take the average value of the iterations
    noise /= maxAmp;

    // normalize the result
    noise = noise * ((high - low) / 2.0) + ((high + low) / 2.0);

    return noise;
  }

  public double continentalGradient(double x, double y) {
    var radial = Math.sqrt(x * x + y * y);
    return Math.clamp(
        ((Math.max(0.0d, radial - ((double) landMaxRadius / 4.0d)) / (double) landMaxRadius) * 2.0d)
            - 1.0d,
        0.0d,
        1.0d);
  }

  /**
   * Get the value used to shift any x coordinate before sampling the sea noise.
   * @return The x shift for the sea noise.
   */
  public int seaShiftX() {
    return seaShiftX;
  }

  /**
   * Get the value used to shift any y coordinate before sampling the sea noise.
   * @return The y shift for the sea noise.
   */
  public int seaShiftY() {
    return seaShiftY;
  }

  /**
   * Get the value used to shift any x coordinate before sampling the uplift noise.
   * @return The x shift for the uplift noise.
   */
  public int upliftShiftX() {
    return upliftShiftX;
  }

  /**
   * Get the value used to shift any y coordinate before sampling the uplift noise.
   * @return The y shift for the uplift noise.
   */
  public int upliftShiftY() {
    return upliftShiftY;
  }

  /**
   * Get the value used to shift any x coordinate before sampling the slope noise.
   * @return The x shift for the slope noise.
   */
  public int slopeShiftX() {
    return slopeShiftX;
  }

  /**
   * Get the value used to shift any y coordinate before sampling the slope noise.
   * @return The y shift for the slope noise.
   */
  public int slopeShiftY() {
    return slopeShiftY;
  }

  /**
   * Get the persistence of the sea noise.
   * @return The persistence of the sea noise.
   */
  public double seaPersistence() {
    return seaPersistence;
  }

  /**
   * Get the scale of the sea noise.
   * @return The scale of the sea noise.
   */
  public double seaScale() {
    return seaScale;
  }

  /**
   * Get the lowest allowed value of the sea noise.
   * @return The lowest allowed value of the sea noise.
   */
  public double seaLow() {
    return seaLow;
  }

  /**
   * Get the highest allowed value of the sea noise.
   * @return The highest allowed value of the sea noise.
   */
  public double seaHigh() {
    return seaHigh;
  }

  /**
   * If the uplift should be based on the sea noise (i.e. Added to it).
   * @return The persistence of the uplift noise.
   */
  public boolean upliftFromSea() {
    return upliftFromSea;
  }

  /**
   * Get the persistence of the uplift noise.
   * @return The persistence of the uplift noise.
   */
  public double upliftPersistence() {
    return upliftPersistence;
  }

  /**
   * Get the scale of the uplift noise.
   * @return The scale of the uplift noise.
   */
  public double upliftScale() {
    return upliftScale;
  }

  /**
   * Get the lowest allowed value of the uplift noise.
   * @return The lowest allowed value of the uplift noise.
   */
  public double upliftLow() {
    return upliftLow;
  }

  /**
   * Get the highest allowed value of the uplift noise.
   * @return The highest allowed value of the uplift noise.
   */
  public double upliftHigh() {
    return upliftHigh;
  }

  /**
   * If the slope should be based on the sea noise (i.e. Added to it).
   * @return The persistence of the slope noise.
   */
  public boolean slopeFromSea() {
    return slopeFromSea;
  }

  /**
   * If the slope should be based on the uplift noise (i.e. Added to it).
   * @return The persistence of the slope noise.
   */
  public boolean slopeFromUplift() {
    return slopeFromUplift;
  }

  /**
   * Get the persistence of the slope noise.
   * @return The persistence of the slope noise.
   */
  public double slopePersistence() {
    return slopePersistence;
  }

  /**
   * Get the scale of the slope noise.
   * @return The scale of the slope noise.
   */
  public double slopeScale() {
    return slopeScale;
  }

  /**
   * Get the lowest allowed value of the slope noise.
   * @return The lowest allowed value of the slope noise.
   */
  public double slopeLow() {
    return slopeLow;
  }

  /**
   * Get the highest allowed value of the slope noise.
   * @return The highest allowed value of the slope noise.
   */
  public double slopeHigh() {
    return slopeHigh;
  }
}
