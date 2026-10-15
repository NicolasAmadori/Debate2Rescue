package config;

import jason.environment.grid.Location;

public final class Config {

  private Config() {}

  // To multiply with the mean between width and height of the grid
  public static final double STATION_FREQ = 0.1;
  public static final double MAX_STATION_FREQ = 0.5;
  public static final double MAX_EMERGENCY_FREQ = 0.5;

  // To multiply with the number of stations
  public static final int RESCUER_FREQ = 1;
  public static final int MAX_RESCUER_FREQ = 4;
  public static final int PILOT_FREQ = 2;
  public static final int MAX_PILOT_FREQ = 4;

  private static int mean(int width, int height) {
    return (width + height) / 2;
  }

  public static int getNumStations(int width, int height) {
    return (int) Math.round(mean(width, height) * STATION_FREQ);
  }

  public static int getMaxNumStations(int width, int height) {
    return (int) Math.round(mean(width, height) * MAX_STATION_FREQ);
  }

  public static int getMaxNumEmergencies(int width, int height) {
    return (int) Math.round(mean(width, height) * MAX_EMERGENCY_FREQ);
  }

  public static int getManhattanDistance(Location from, Location to) {
    return Math.abs(from.x - to.x) + Math.abs(from.y - to.y);
  }
}
