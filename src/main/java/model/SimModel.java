package model;

import jason.environment.grid.Location;
import java.util.List;
import java.util.Optional;

public interface SimModel {

  /***
   * Getter method
   * @return the list of locations of the stations on the grid.
   */
  List<Location> getStations();

  /***
   * Getter method
   * @return the list of emergencies on the grid.
   */
  List<Emergency> getEmergencies();

  /***
   * Getter method
   * @return a free grid position if present, or empty if there are no free cells left.
   */
  Optional<Location> getFreePosition();

  /***
   * Creates an emergency with random properties in a cell without stations and emergencies.
   *
   * @return the new emergency, or empty if there are no free cells left.
   */
  Optional<Emergency> spawnEmergency();

  /***
   * Sets the status of an emergency.
   *
   * @param emergencyId
   * @param newStatus
   * @return the updated emergency, or empty if not found.
   */
  Optional<Emergency> setEmergencyStatus(int emergencyId, EmergencyStatus newStatus);

  /***
   * Gets the emergency at a given position.
   *
   * @param position
   * @return the emergency at the position, or empty if not found.
   */
  Optional<Emergency> getEmergencyAt(Location position);

  /***
   * Declares an emergency over and removes it from the grid.
   * @param emergencyId
   * @return the removed emergency, or empty if it does not exist.
   */
  Optional<Emergency> resolveEmergency(int emergencyId);

  /***
   * Places a new responder on a station. Responders with the same role are spread over the stations.
   *
   * @param name the name of the responder
   * @param role rescuer or pilot
   * @return the new responder
   */
  Responder addResponder(String name, ResponderRole role);

  /***
   * Moves a responder towards the target, first along x and then along y (manhattan path).
   * Rescuers move by one cell, pilots fly by two.
   *
   * @return false if the responder is not found, true otherwise
   */
  boolean moveTowards(String name, Location target);

  /***
   * Getter method.
   * @param name the name of the responder
   * @return the responder with the given name, or empty if not found.
   */
  Optional<Responder> getResponder(String name);
}
