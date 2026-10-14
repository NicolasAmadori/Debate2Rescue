package model;

import jason.environment.grid.Location;

/**
 * An immutable rescuer on the grid.
 *
 * @param name the name of the rescuer
 * @param station the station where the rescuer starts and goes back to
 * @param position the current cell of the rescuer
 */
public record Rescuer(String name, Location station, Location position) implements Responder {

  @Override
  public Rescuer moveTo(Location newPosition) {
    return new Rescuer(name, station, newPosition);
  }

  @Override
  public String toString() {
    return "Rescuer["
        + this.name
        + "]"
        + " at ("
        + this.position.x
        + ","
        + this.position.y
        + "): from station ("
        + this.station.x
        + ","
        + this.station.y
        + ")";
  }
}
