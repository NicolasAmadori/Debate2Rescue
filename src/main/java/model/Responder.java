package model;

import jason.environment.grid.Location;

/**
 * An immutable responder (rescuer/pilot) on the grid.
 *
 * @param name the name of the responder
 * @param role rescuer or pilot
 * @param station the station where the responder starts and goes back to
 * @param position the current cell of the responder
 */
public record Responder(String name, ResponderRole role, Location station, Location position) {

  public Responder moveTo(Location newPosition) {
    return new Responder(name, role, station, newPosition);
  }
}
