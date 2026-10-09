package view;

import java.awt.Color;

/** The kinds of agents shown on the grid with its own color */
public enum AgentType {
  RESCUER(new Color(230, 126, 34)),
  PILOT(new Color(41, 128, 185));

  private final Color color;

  AgentType(Color color) {
    this.color = color;
  }

  public Color getColor() {
    return color;
  }
}
