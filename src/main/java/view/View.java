package view;

import arg.DebateEventListener;
import java.util.function.IntConsumer;

public interface View {

  /** Repaints the view with the updated model */
  void update();

  /**
   * Appends a message to the event log
   *
   * @param message the message to show
   */
  void log(String message);

  /**
   * Appends a categorized message to the event log
   *
   * @param category the category determining formatting hierarchy
   * @param message the message to show
   */
  default void log(LogCategory category, String message) {
    log(message);
  }

  /**
   * Returns the debate event listener implemented by the view, if any.
   *
   * @return the debate listener or null
   */
  default DebateEventListener getDebateListener() {
    return null;
  }

  /**
   * Registers the action to run when the user moves the speed slider
   *
   * @param onSpeedChange receives the delay in ms between simulation steps, in milliseconds
   */
  void setOnSpeedChange(IntConsumer onSpeedChange);

  /**
   * Registers the action to run when the user moves the spawn rate slider
   *
   * @param onSpawnChanceChange receives the chance, in percentage, of spawning an emergency at
   *     every simulation step
   */
  void setOnSpawnChanceChange(IntConsumer onSpawnChanceChange);

  /**
   * Registers the action to run when the user moves the max emergencies slider
   *
   * @param onMaxEmergenciesChange receives the max number of emergencies on the grid at the same
   *     time
   */
  void setOnMaxEmergenciesChange(IntConsumer onMaxEmergenciesChange);
}
