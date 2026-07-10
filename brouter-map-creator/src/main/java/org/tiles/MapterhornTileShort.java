package org.tiles;

import btools.statcoding.BitInputStream;
import btools.statcoding.BitOutputStream;
import btools.statcoding.huffman.HuffmanDecoder;
import btools.statcoding.huffman.HuffmanEncoder;

import java.io.*;
import java.util.Arrays;
import java.util.Objects;

//mostly copy of https://github.com/abrensch/statcoding/blob/main/examples/lidar/DEM1LidarTile.java
//with some removals (since coordinates of tiles are stored in the name)

public class MapterhornTileShort {
  private short[] data; // array holding the elevation data in meters
  private String dataType;
  private long x; // x-coordinate of the slippy map
  private long y; // y-coordinate of the slippy map
  private int z; // zoom level of slippy map (16 max)

  public long getX() {
    return x;
  }

  public long getY() {
    return y;
  }

  public int getZ() {
    return z;
  }

  public TileName getTile() {
    return new TileName(z, x, y);
  }

  static final int TILE_SIZE = 512;
  public static final int ZOOM_LEVEL = 16;
  static final String EXTENSION = "mapterhorn";

  public MapterhornTileShort(short[] data) {
    this.data = data;
    this.dataType = "MTT_S";
  }

  public static MapterhornTileShort getTile(String path, double lon, double lat) {
    var tile = TileUtils.coordinateToTile(lon, lat, ZOOM_LEVEL);
    return getTile(path, tile.x(), tile.y());

  }

  public static MapterhornTileShort getTile(String path, long x, long y) {
    var tile = new TileName(ZOOM_LEVEL, x, y);
    try {
      var file = TileUtils.getOrCreateTileFile(path, tile, false, EXTENSION);

      if (file.exists()) {
        return new MapterhornTileShort(file);
      } else {
        return null;
      }
    } catch (IOException e) {
      return null;
    }
  }

  public short getElevation(int ilon, int ilat) {
    double lat = ((double) ilat - 0.5) / 1000000.0 - 90.0;

    // Reverse the longitude conversion
    // Formula: lon = (ilon - 0.5) / 1,000,000 - 180
    double lon = ((double) ilon - 0.5) / 1000000.0 - 180.0;
    //System.out.println("POINT(" + lon + " " + lat + ")");
    return getElevation(lon, lat);
  }

  public short getElevation(double lon, double lat) {
    double xRelative = ((lon + 180.0) / 360.0 * (1 << z)) - x;

    double yRelative = ((1.0 - Math.log(Math.tan(Math.toRadians(lat)) + 1.0 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2.0 * (1 << z)) - y;

    int pxRaw = (int) (xRelative * TILE_SIZE);
    int pyRaw = (int) (yRelative * TILE_SIZE);

    // Ensure we stay within bounds
    int px = Math.max(0, Math.min(pxRaw, TILE_SIZE - 1));
    int py = Math.max(0, Math.min(pyRaw, TILE_SIZE - 1));

    return data[py * TILE_SIZE + px];
  }

  /**
   * Write this tile to a stream in a compact format.
   * <br><br>
   * It uses huffman encoding with fixed statistics on the elevation-diffs.
   *
   * @param outStream the stream to write to (expected to be buffered)
   * @see #readCompact(InputStream)
   */
  public void writeCompact(OutputStream outStream) throws IOException {

    BitOutputStream bos = new BitOutputStream(outStream);
    bos.writeUTF(dataType);

    HuffmanEncoder<Integer> encoder = new HuffmanEncoder<>() {
      @Override
      protected void encodeObjectToStream(Integer lv) throws IOException {
        bos.encodeSignedVarBits(lv, 8);
      }
    };
    for (int pass = 1; pass <= 2; pass++) {
      encoder.init(bos);
      int lastValue = 0;
      for (int i = 0; i < (TILE_SIZE * TILE_SIZE); i++) {
        int value = data[i];
        encoder.encodeObject(value - lastValue);
        lastValue = value;
      }
    }
    //System.out.println(encoder.getStats());
    bos.writeSyncBlock(0L);
  }

  /**
   * Fill this tile from a compact format input stream.
   *
   * @param file the stream to read from (expected to be buffered)
   * @see #writeCompact(OutputStream)
   */
  public MapterhornTileShort(File file) {

    var path = file.toPath();
    int nameCount = path.getNameCount();

    if (nameCount < 3) {
      throw new IllegalArgumentException("Path does not contain enough segments for Z/X/Y");
    }

    try {
      // 3. Extract the last three segments
      y = Long.parseLong(path.getName(nameCount - 1).toString().replace("." + EXTENSION, ""));
      x = Long.parseLong(path.getName(nameCount - 2).toString());
      z = Integer.parseInt(path.getName(nameCount - 3).toString());

      try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file))) {

        BitInputStream bis = new BitInputStream(is);
        dataType = bis.readUTF();

        HuffmanDecoder<Long> decoder = new HuffmanDecoder<>() {
          @Override
          protected Long decodeObjectFromStream() throws IOException {
            return bis.decodeSignedVarBits(8);
          }
        };
        decoder.init(bis, 12);
        data = new short[TILE_SIZE * TILE_SIZE];
        long value = 0L;
        for (int i = 0; i < (TILE_SIZE * TILE_SIZE); i++) {
          value += decoder.decodeObject();
          data[i] = (short) value;
        }
        if (bis.readSyncBlock() != 0L) {
          throw new IllegalArgumentException("0-sync not found!");
        }
      } catch (IOException e) {
        System.err.println("Error processing tile file : " + file.getAbsolutePath() + " " + e.getMessage());
      }

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("One of the path segments is not a valid integer.", e);
    }
  }

  public boolean equalsTile(int z, long y, long x) {
    return x == this.x && y == this.y && z == this.z;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    MapterhornTileShort that = (MapterhornTileShort) o;
    return x == that.x && y == that.y && z == that.z && Objects.deepEquals(data, that.data) && Objects.equals(dataType, that.dataType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(Arrays.hashCode(data), dataType, x, y, z);
  }

  @Override
  public String toString() {
    return "MapterhornTileShort{" +
      "dataType='" + dataType + '\'' +
      ", x=" + x +
      ", y=" + y +
      ", z=" + z +
      '}';
  }
}
