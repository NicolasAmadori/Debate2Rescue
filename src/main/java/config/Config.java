package config;

import jason.environment.grid.Location;

public final class Config {

  private Config() {}

  // To multiply with the mean between width and height of the grid
  public static final double STATION_FREQ = 0.1;

  public static int getNumStations(int width, int height) {
    return (int) Math.round(((width + height) / 2) * STATION_FREQ);
  }

  public static int getManhattanDistance(Location from, Location to) {
    return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
  }
}
