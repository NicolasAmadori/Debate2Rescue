package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import model.EmergencyType;

/** Explains what the shapes and colors on the grid mean */
class Legend extends JPanel {

  private static final int SWATCH_PX = 14;

  Legend() {
    super(new GridLayout(0, 2, 8, 4));
    setBorder(BorderFactory.createTitledBorder("Legend"));

    for (AgentType agent : AgentType.values()) {
      add(entry(agent.name(), new Swatch(Shape.CIRCLE, agent.getColor())));
    }
    add(entry("station", new Swatch(Shape.STRIPES, null)));
    // obstacles are drawn by GridWorldView.drawObstacle, dark gray with a black outline
    add(entry("obstacle", new Swatch(Shape.SQUARE, Color.DARK_GRAY)));
    for (EmergencyType type : EmergencyType.values()) {
      add(entry(type.name(), new Swatch(Shape.SQUARE, ViewImpl.toColor(type.colorCode))));
    }
  }

  private static JLabel entry(String name, Icon icon) {
    return new JLabel(readable(name), icon, JLabel.LEFT);
  }

  /** FIRE becomes "Fire", ARMED_PERSON becomes "Armed person" */
  static String readable(String name) {
    String text = name.toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  private enum Shape {
    CIRCLE,
    SQUARE,
    STRIPES
  }

  /** A small icon drawn like the matching element of the grid */
  private record Swatch(Shape shape, Color color) implements Icon {

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      switch (shape) {
        case CIRCLE -> {
          g.setColor(color);
          g.fillOval(x, y, SWATCH_PX, SWATCH_PX);
        }
        case SQUARE -> {
          g.setColor(color);
          g.fillRect(x, y, SWATCH_PX, SWATCH_PX);
          // the outline keeps white and yellow visible, as on the grid
          g.setColor(Color.BLACK);
          g.drawRect(x, y, SWATCH_PX - 1, SWATCH_PX - 1);
        }
        case STRIPES -> ViewImpl.drawStripes(g, x, y, SWATCH_PX, SWATCH_PX);
      }
    }

    @Override
    public int getIconWidth() {
      return SWATCH_PX;
    }

    @Override
    public int getIconHeight() {
      return SWATCH_PX;
    }
  }
}
