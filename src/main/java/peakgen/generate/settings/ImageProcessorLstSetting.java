package peakgen.generate.settings;

import peakgen.generate.topology.LstCoordinate;
import peakgen.generate.util.GenerationUtils;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.util.Random;

/**
 * Settings for the LST generator that uses an image as the data source. The RGB values of the image
 * are used to determine the terrain values, mostly independently.<br>
 * Pixels in the image are sampled relative to the size of the terrain map as specified by the size
 * parameter. If the image is not square, it will be stretched or squashed as needed via the
 * sampling process.<br>
 * The basic rules are:<br>
 * - If the blue value is greater than the maximum of the red and green values, the point is
 * considered sea, otherwise the point is considered land.<br>
 * - If the point is land, the green value is used to determine the uplift factor (0.0 - 255) ->
 * (0.0 to 1.0).<br>
 * - If the point is land, the red value is used to determine the slope factor (0.0 - 255) -> (0.0
 * to 1.0).<br>
 * <br>
 * Examples of things to try:<br>
 * - A bright -> dark green gradient will produce fairly uniformly sloped terrain.<br>
 * - Applying red channel noise over the top of an image should increase the complexity of the
 * terrain.<br>
 * - Combining high red and green values should produce very high mountainous terrain.<br>
 * - A ring of high red and green surrounding a low red area should produce a plateau.<br>
 * - Broad areas of high green with varying red should produce thick mountain ranges.<br>
 * - Even just running the generator on a standard image can produce some interesting results.<br>
 *
 * @param size {@link LstSettings#size()}
 * @param halfSize Half the size of the terrain map, used for scaling.
 * @param lod {@link LstSettings#lod()}
 * @param seed This is not used in this implementation, but is required by the interface.
 * @param random This is not used in this implementation, but is required by the interface.
 * @param image The image to use as the data source.
 * @param raster The raster data for the image.
 * @param sampleModel The sample model for the image.
 * @param colorModel The color model for the image.
 */
public record ImageProcessorLstSetting(
    int size,
    int halfSize,
    int lod,
    long seed,
    Random random,
    BufferedImage image,
    Raster raster,
    SampleModel sampleModel,
    ColorModel colorModel)
    implements LstSettings {

  /**
   * Create a new image processor setting with minimal parameters. The raster, sample model, and
   * color model are extracted from the image.
   *
   * @param size {@link LstSettings#size()}
   * @param lod {@link LstSettings#lod()}
   * @param seed This is not used in this implementation, but is required by the interface.
   * @param img The image to use as the data source.
   */
  public ImageProcessorLstSetting(
      int size, int lod, long seed, BufferedImage img) {
    this(
        size,
        size / 2,
        lod,
        seed,
        new Random(seed),
        img,
        img.getData(),
        img.getData().getSampleModel(),
        img.getColorModel());
  }

  @Override
  public LstCoordinate getData(double x, double y, int maxSize) {
    var pixel =
        sampleModel.getDataElements(
            shiftAndScaleX((int) Math.round(x)),
            shiftAndScaleY((int) Math.round(y)),
            null,
            raster.getDataBuffer());

    // Components will be in the range of 0..255:
    int blue = colorModel.getBlue(pixel);
    int green = colorModel.getGreen(pixel);
    int red = colorModel.getRed(pixel);
    var redAndGreen = Math.max(red, green);

    if (blue >= redAndGreen) {
      return new LstCoordinate(
          x,
          y,
          maxSize,
          GenerationUtils.lerp(-1.0d, 1.0d, (255.0d - (double) blue) / 255.0d),
          0.0d,
          0.0d);
    } else {
      return new LstCoordinate(
          x,
          y,
          maxSize,
          GenerationUtils.lerp(-1.0d, 1.0d, (255.0d - (double) blue) / 255.0d),
          GenerationUtils.lerp(0.0d, 1.0d, (double) green / 255.0d),
          GenerationUtils.lerp(0.0d, 1.0d, (double) red / 255.0d));
    }
  }

  /**
   * Find the corresponding x and y in the image, since size and image size are different.
   *
   * @param x The x coordinate (assumed to be in the range of -halfSize...halfSize)
   * @return The corresponding x coordinate in the image
   */
  int shiftAndScaleX(double x) {
    var mapX = Math.clamp((x + halfSize) / size, 0.0d, 1.0d);
    return Math.clamp(
        (int) Math.round(GenerationUtils.lerp(0.0d, image.getWidth() - 1, mapX)),
        0,
        image.getWidth() - 1);
  }

  /**
   * Find the corresponding x and y in the image, since size and image size are different.
   *
   * @param y The y coordinate (assumed to be in the range of -halfSize...halfSize)
   * @return The corresponding y coordinate in the image
   */
  int shiftAndScaleY(double y) {
    var mapY = Math.clamp((y + halfSize) / size, 0.0d, 1.0d);
    return Math.clamp(
        (int) Math.round(GenerationUtils.lerp(image.getHeight() - 1, 0.0d, mapY)),
        0,
        image.getHeight() - 1);
  }
}
