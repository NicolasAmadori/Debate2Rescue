package view;

import java.util.function.IntConsumer;

public interface View {

  /** Opens the window */
  void show();

  /**
   * Shows a station on the grid
   *
   * @param x the column of the station
   * @param y the row of the station
   */
  void addStation(int x, int y);

  /**
   * Shows a new agent on the grid
   *
   * @param name the unique name of the agent
   * @param type the kind of agent
   * @param x the column of the agent
   * @param y the row of the agent
   */
  void addAgent(String name, AgentType type, int x, int y);

  /**
   * Moves an agent on the grid
   *
   * @param name the name of the agent
   * @param x the new column of the agent
   * @param y the new row of the agent
   */
  void moveAgent(String name, int x, int y);

  /**
   * Shows a new disaster on the grid
   *
   * @param id the unique id of the disaster
   * @param x the column of the disaster
   * @param y the row of the disaster
   */
  void addDisaster(int id, int x, int y);

  /**
   * Removes a disaster from the grid
   *
   * @param id the id of the disaster
   */
  void removeDisaster(int id);

  /**
   * Appends a message to the event log
   *
   * @param message the message to show
   */
  void log(String message);

  /**
   * Registers the action to run when the user moves the speed slider
   *
   * @param onSpeedChange receives the delay in ms between simulation steps
   */
  void setOnSpeedChange(IntConsumer onSpeedChange);
}
