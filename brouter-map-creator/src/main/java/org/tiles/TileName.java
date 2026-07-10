package org.tiles;

import java.util.Objects;

public final class TileName {
  public final int z;
  public final long x;
  public final long y;

  public TileName(int z, long x, long y) {
    this.z = z;
    this.x = x;
    this.y = y;
  }

  public int z() {
    return z;
  }

  public long x() {
    return x;
  }

  public long y() {
    return y;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (TileName) obj;
    return this.z == that.z &&
      this.x == that.x &&
      this.y == that.y;
  }

  @Override
  public int hashCode() {
    return Objects.hash(z, x, y);
  }

  @Override
  public String toString() {
    return "TileName[" +
      "z=" + z + ", " +
      "x=" + x + ", " +
      "y=" + y + ']';
  }
}
