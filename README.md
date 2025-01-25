# Peakgen
This is a Java library that provides the capability to generate terrain based
on geological uplift values, variable resistance to thermal shock, and water erosion.

The input is provided as a series of constrained values which defines:
* Landmasses and their coastlines (aka seaFactor)
* Uplift values (aka upliftFactor)
* Thermal shock resistance, defined by maximum allowed slope (aka slopeFactor)

From this input, semi-random points are selected, their catchment areas and
potential stream flows are calculated. The input can either be seeded simplex noise
or an image file. An interface is provided to allow other input methods as well.

Calculations of uplift and erosion are done iteratively, over large timescales,
and can continue for as long as desired. The terrain tends to stabilize after
a few hundred iterations.

The output is both a 3d mesh representing the terrain, and the final shape of
the terrain's water flow as a series of streamlines. Both can be exported to
glTF 2.0 files.

## Basic Usage
[LSTG]: <src/main/java/peakgen/generate/LargeScaleTerrainGenerator.java>
[GEO_SETTINGS]: <src/main/java/peakgen/generate/GeologySettings.java>
[LST_SETTINGS]: <src/main/java/peakgen/generate/settings/LstSettings.java>
[IMAGE_SETTINGS]: <src/main/java/peakgen/generate/settings/ImageProcessorLstSetting.java>
[NOISE_SETTINGS]: <src/main/java/peakgen/generate/settings/SimplexNoiseLstSettings.java>
[GLTF_EX]: <src/main/java/peakgen/generate/export/gltf/GlTFExport.java>

Creating an instance of the [LargeScaleTerrainGenerator][LSTG] class initiates
the creation of all initial data structures. At a minimum you must provide
an instance of [LstSettings][LST_SETTINGS], which can be either 
[SimplexNoiseLstSettings][NOISE_SETTINGS] or 
[ImageProcessorLstSettings][IMAGE_SETTINGS]. Adjusting how geology is simulated
is done by providing an instance of [GeologySettings][GEO_SETTINGS].

```java
    var seed = 54321L;
    var settings = new SimplexNoiseLstSettings(
        50000, // 50km^2 area
        140000, // 140k sample points across the area
        20000, // Reduce land noise so no land generates outside of a 20km radius
        new Random(seed),
        seed
    );

    var generator = new LargeScaleTerrainGenerator(settings);
```

Determine when the terrain generation cycle will be stopped by providing a 
predicate to the `generate` method. The following example will stop the
generation cycle after 300 iterations (terrain has usually stabilized by then).

```java
    generator.generate(lstGen -> lstGen.getNumberOfSteps() > 300);
```

The terrain mesh and streamlines can be exported to glTF files by using the [GltfExporter][GLTF_EX] class.
Any nodes that are sea nodes will be mapped between 0 and -1500 meters in the
resulting terrain file, depending on the seaFactor value (0 -> -1)

```java
    var exporter = new GlTFExport();

    try {
      exporter.export(generator, "/sample/dir");
    } catch (IOException e) {
      e.printStackTrace();
    }
```

This is the end result after blender import (orthographic projection):
![example1_blender_streams.png](images%2Fexample1_blender.png)

And the same result's streams after blender import (zoomed 
slightly for greater clarity):
![example1_blender_streams.png](images%2Fexample1_blender_streams.png)

## Using an Image as Input
If an image is provided as input, the [ImageProcessorLstSettings][IMAGE_SETTINGS]
uses the RGB channels to determine the sea, uplift, and thermal shock values.
Any pixel with a larger blue value than the red or green values is considered to
be sea, otherwise it is considered land. Red defines the uplift value, and green
defines the thermal shock resistance. These values are mapped to the
geology settings' constraints. Any sea pixels will have their blue channel values
mapped to a seaFactor value (higher blue value = deeper sea).

```java
import peakgen.generate.LargeScaleTerrainGenerator;
import peakgen.generate.export.gltf.GlTFExport;
import peakgen.generate.settings.ImageProcessorLstSetting;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

class ImageProcessorSample {

  public static void main(String[] args) throws IOException {
    var seed = 18860610L;
    var imageFile = new File("/sample/dir/example2_input.png");
    BufferedImage img = javax.imageio.ImageIO.read(imageFile);
    var settings =
        new ImageProcessorLstSetting(
            50000, // 50km^2 area
            140000, // 140k sample points across the area
            seed,
            img);

    var generator = new LargeScaleTerrainGenerator(settings);

    generator.generate(lstGen -> lstGen.getNumberOfSteps() > 300);

    var exporter = new GlTFExport();

    try {
      exporter.export(generator, "/sample/dir");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
```

### Example:

An input file like this example, with mostly deep sea, and some semi-random
uplift (and noise based slope) data used in the above example code:
![example2_input.png](images%2Fexample2_input.png)

Will result in the following output when imported into blender (orthographic projection):
![example2_blender_terrain.png](images%2Fexample2_blender_terrain.png)

The resulting stream look like this (zoomed slightly for greater clarity):
![example2_blender_streams.png](images%2Fexample2_blender_streams.png)

# Notes on Detail/Resolution:
Without a lot of memory, processing power, and time the resolution of the
simulation is unfortunately limited using this library. The resulting terrain
may have a relatively low polygon count for the area that is being generated.
Some potential solutions for this include:
- **Adding detail to the terrain using noise**: This should allow
  an unbounded improvement in detail, but may add some unrealistic elements and
  require recalculating streams (if they are in use).
- **Simulating smaller discrete areas**: Simulate smaller areas and stitch them
  together. Depending on what inputs are used the edges may not meet correctly,
  and the mountain height may be limited by the smaller simulation area.

# Acknowledgements
* ["Large Scale Terrain Generation from Tectonic Uplift and Fluvial Erosion"
  by Guillaume Cordonnier, Jean Braun, Marie-Paule Cani, Bedrich Benes,
  Eric Galin, et al..](https://doi.org/10.1111/cgf.12820)
* KdotJPG's [SimplexNoise](https://github.com/KdotJPG/OpenSimplex2) is used to
  generate initial noise, and is embedded.
* Chad Juliano's [jgltf-mesh](https://github.com/chadj2/jgltf-mesh) library is
embedded, with some modifications. This library made glTF exporting a lot easier.
