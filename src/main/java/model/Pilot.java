package model;

import jason.environment.grid.Location;

/**
 * An immutable pilot on the grid.
 *
 * @param name the name of the pilot
 * @param station the station where the pilot starts and goes back to
 * @param position the current cell of the pilot
 * @param resource the resource transported by the pilot
 */
public record Pilot(String name, Location station, Location position, Resource resource)
    implements Responder {

  @Override
  public Pilot moveTo(Location newPosition) {
    return new Pilot(name, station, newPosition, resource);
  }

  @Override
  public String toString() {
    return "Pilot["
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
        + ") transporting "
        + resource.toString();
  }
}
