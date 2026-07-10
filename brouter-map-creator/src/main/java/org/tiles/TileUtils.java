package org.tiles;

import static java.lang.Math.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TileUtils {
  public static TileName coordinateToTile(double lon, double lat, int z) {
    // 1 << z is the bitwise equivalent of 2^z
    int mapSize = 1 << z;

    // 1. Calculate Tile X
    long x = (long) floor((lon + 180.0) / 360.0 * mapSize);

    // 2. Calculate Tile Y
    double latRad = toRadians(lat);
    long y = (long) floor(
      (1.0 - log(
        tan(latRad) + 1.0 / cos(latRad)
      ) / PI) / 2.0 * mapSize
    );

    return new TileName(z, x, y);
  }

  /**
   * Overloaded method to match Kotlin's default parameters.
   */
  public static File getOrCreateTileFile(String rootDir, TileName tile) throws IOException {
    return getOrCreateTileFile(rootDir, tile, false, "png");
  }

  /**
   * Generates a File object pointing to the correct Z/X/Y path and
   * ensures the parent directory structure exists.
   *
   * @param rootDir   The base directory string (e.g., "output/tiles")
   * @param tile      The TileIndex object containing x, y, z coordinates
   * @param extension The file extension, defaults to "png"
   * @return A File object representing the tile location
   */
  public static File getOrCreateTileFile(String rootDir, TileName tile, boolean create, String extension) throws IOException {
    // 1. Construct the path: root/z/x/
    // Path.of is the preferred modern entry point in Java 11+
    Path directoryPath = Path.of(rootDir, String.valueOf(tile.z()), String.valueOf(tile.x()));

    // 2. Create directories if requested
    if (create) {
      Files.createDirectories(directoryPath);
    }

    // 3. Define the full file path: root/z/x/y.extension
    String fileName = tile.y() + "." + extension;
    Path fullPath = directoryPath.resolve(fileName);

    return fullPath.toFile();
  }
}
