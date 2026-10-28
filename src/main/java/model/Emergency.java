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
 * @param trapped whether victims are trapped
 * @param escalating whether the emergency is escalating
 * @param size the size of the emergency
 * @param collateralRisk whether there is risk to surrounding critical infrastructure
 * @param status how the rescue is going
 */
public record Emergency(
    int id,
    Location position,
    EmergencyType type,
    Severity severity,
    int victims,
    boolean trapped,
    boolean escalating,
    Size size,
    boolean collateralRisk,
    EmergencyStatus status) {

  /***
   *
   * @param newStatus the new status of the emergency
   * @return a new Emergency with the same properties but a different status
   */
  public Emergency newStatus(EmergencyStatus newStatus) {
    return new Emergency(
        id,
        position,
        type,
        severity,
        victims,
        trapped,
        escalating,
        size,
        collateralRisk,
        newStatus);
  }

  @Override
  public String toString() {
    return "Emergency["
        + this.id
        + "] at ("
        + this.position.x
        + ","
        + this.position.y
        + "): "
        + this.type.toString()
        + " with "
        + this.severity.toString()
        + "["
        + this.severity.esi
        + "], n."
        + this.victims
        + " victims, size "
        + this.size.toString()
        + (this.trapped ? " [TRAPPED]" : "")
        + (this.escalating ? " [ESCALATING]" : "")
        + (this.collateralRisk ? " [COLLATERAL_RISK]" : "");
  }
}
