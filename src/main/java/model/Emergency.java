package model;

import jason.environment.grid.Location;

/**
 * An immutable emergency on the grid.
 *
 * @param id the unique id of the emergency
 * @param position the cell of the emergency
 * @param type what happened
 * @param severity how serious it is
 * @param victims how many people need help
 * @param status how the rescue is going
 */
public record Emergency(
    int id,
    Location position,
    EmergencyType type,
    Severity severity,
    int victims,
    EmergencyStatus status) {

  /***
   *
   * @param newStatus the new status of the emergency
   * @return a new Emergency with the same properties but a different status
   */
  public Emergency newStatus(EmergencyStatus newStatus) {
    return new Emergency(id, position, type, severity, victims, newStatus);
  }
}
