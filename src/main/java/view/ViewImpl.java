package view;

import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.SwingUtilities;
import model.Responder;
import model.ResponderRole;
import model.SimModel;

public class ViewImpl extends GridWorldView implements View {

  private static final Color STATION_COLOR = Color.DARK_GRAY;
  private static final Color EMERGENCY_COLOR = Color.RED;
  private static final int GRID_SIZE_PX = 600;
  private static final int STRIPE_GAP_PX = 5;

  private SidePanel side;

  public ViewImpl(SimModel model) {
    super(model, "Debate2Rescue", GRID_SIZE_PX);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setVisible(true);
  }

  @Override
  public void initComponents(int width) {
    super.initComponents(width);

    getCanvas().setBackground(Color.WHITE);
    getCanvas().setPreferredSize(new Dimension(width, width));
    this.side = new SidePanel();
    getContentPane().add(side, BorderLayout.EAST);
    pack();
  }

  @Override
  public void update() {
    repaint();
  }

  @Override
  public void draw(Graphics g, int x, int y, int object) {
    switch (object) {
      case SimModel.STATION -> drawStation(g, x, y);
      case SimModel.EMERGENCY -> {
        Color color =
            model()
                .getEmergencyAt(new Location(x, y))
                .map(e -> toColor(e.type().colorCode))
                .orElse(EMERGENCY_COLOR);
        g.setColor(color);
        g.fillRect(x * cellSizeW + 1, y * cellSizeH + 1, cellSizeW - 1, cellSizeH - 1);
        g.setColor(Color.BLACK);
        g.drawRect(x * cellSizeW + 1, y * cellSizeH + 1, cellSizeW - 2, cellSizeH - 2);
      }
      default -> {}
    }
  }

  @Override
  public void drawAgent(Graphics g, int x, int y, Color c, int id) {
    if (g == null) {
      return;
    }
    int top = y * cellSizeH;
    int height = cellSizeH;
    if (model.hasObject(SimModel.EMERGENCY, x, y)) {
      top += cellSizeH / 2;
      height = cellSizeH / 2;
    }
    List<Responder> here = model().getRespondersAt(new Location(x, y));
    int slotsPerSide = (int) Math.ceil(Math.sqrt(here.size()));
    int slotW = cellSizeW / Math.max(1, slotsPerSide);
    int slotH = height / Math.max(1, slotsPerSide);
    int gap = Math.max(1, Math.min(slotW, slotH) / 6);
    for (int i = 0; i < here.size(); i++) {
      int slotX = x * cellSizeW + (i % slotsPerSide) * slotW;
      int slotY = top + (i / slotsPerSide) * slotH;
      g.setColor(colorOf(here.get(i).role()));
      // the circle is centered on the shorter side of the container
      int diameter = Math.min(slotW, slotH) - 2 * gap;
      g.fillOval(
          slotX + (slotW - diameter) / 2 + 1,
          slotY + (slotH - diameter) / 2 + 1,
          diameter,
          diameter);
    }
  }

  @Override
  public void log(String message) {
    SwingUtilities.invokeLater(() -> side.log(message));
  }

  @Override
  public void setOnSpeedChange(IntConsumer onSpeedChange) {
    SwingUtilities.invokeLater(() -> side.setOnSpeedChange(onSpeedChange));
  }

  /** Draws a station as a striped cell */
  private void drawStation(Graphics g, int x, int y) {
    int width = cellSizeW - 1;
    int height = cellSizeH - 1;
    // avoiding painting outside requires a nel canva for the square
    Graphics cell = g.create(x * cellSizeW + 1, y * cellSizeH + 1, width, height);
    cell.setColor(STATION_COLOR);
    for (int i = -height; i < width; i += STRIPE_GAP_PX) {
      cell.drawLine(i, height, i + height, 0);
    }
    cell.drawRect(0, 0, width - 1, height - 1);
    cell.dispose();
  }

  private static Color colorOf(ResponderRole role) {
    return switch (role) {
      case RESCUER -> AgentType.RESCUER.getColor();
      case PILOT -> AgentType.PILOT.getColor();
    };
  }

  private SimModel model() {
    return (SimModel) this.model;
  }

  private static Color toColor(String colorCode) {
    return switch (colorCode) {
      case "red" -> Color.RED;
      case "blue" -> Color.BLUE;
      case "pink" -> Color.PINK;
      case "brown" -> new Color(139, 69, 19);
      case "black" -> Color.BLACK;
      case "white" -> Color.WHITE;
      case "green" -> Color.GREEN;
      case "gray" -> Color.GRAY;
      case "orange" -> Color.ORANGE;
      case "yellow" -> Color.YELLOW;
      default -> EMERGENCY_COLOR;
    };
  }
}
