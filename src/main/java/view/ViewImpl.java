package view;

import arg.DebateEventListener;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntConsumer;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import model.Pilot;
import model.Rescuer;
import model.Resource;
import model.Responder;
import model.SimModel;

public class ViewImpl extends GridWorldView implements View {

  private static final Map<Resource, BufferedImage> RESOURCE_ICONS = loadResourceIcons();
  private static final Color STATION_COLOR = Color.DARK_GRAY;
  private static final Color OBSTACLE_COLOR = Color.DARK_GRAY;
  private static final Color EMERGENCY_COLOR = Color.RED;
  private static final int GRID_SIZE_PX = 600;
  private static final int STRIPE_GAP_PX = 5;

  private SidePanel side;

  public ViewImpl(SimModel model) {
    super(model, "Debate2Rescue", GRID_SIZE_PX);
    new CellTooltip(this, getCanvas(), this::cellAt, model);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setVisible(true);
  }

  @Override
  public void initComponents(int width) {
    super.initComponents(width);

    getCanvas().setBackground(Color.WHITE);
    getCanvas().setPreferredSize(new Dimension(width, width));
    this.side = new SidePanel(model.getWidth(), model.getHeight());
    getContentPane().add(side, BorderLayout.EAST);
    pack();
  }

  @Override
  public void update(int x, int y) {
    super.update(x, y);
    // Linux fix: without it some movements are not painted
    Toolkit.getDefaultToolkit().sync();
  }

  @Override
  public void draw(Graphics g, int x, int y, int object) {
    if ((object & SimModel.OBSTACLE) != 0) {
      drawObstacle(g, x, y);
    }
    if ((object & SimModel.STATION) != 0) {
      drawStation(g, x, y);
    }
    if ((object & SimModel.EMERGENCY) != 0) {
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
  }

  @Override
  public void drawObstacle(Graphics g, int x, int y) {
    drawObstacleTexture(g, x * cellSizeW + 1, y * cellSizeH + 1, cellSizeW - 1, cellSizeH - 1);
  }

  /** Draws an obstacle as a cell with a dark gray 'X' texture, also used by the legend */
  static void drawObstacleTexture(Graphics g, int left, int top, int width, int height) {
    Graphics cell = g.create(left, top, width, height);
    cell.setColor(OBSTACLE_COLOR);
    cell.drawRect(0, 0, width - 1, height - 1);

    // Draw thick 'X' pattern
    cell.drawLine(0, 0, width - 1, height - 1);
    cell.drawLine(1, 0, width - 1, height - 2);
    cell.drawLine(0, 1, width - 2, height - 1);

    cell.drawLine(0, height - 1, width - 1, 0);
    cell.drawLine(1, height - 1, width - 1, 1);
    cell.drawLine(0, height - 2, width - 2, 0);
    cell.dispose();
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
    int gap = Math.max(1, Math.min(slotW, slotH) / 10);
    for (int i = 0; i < here.size(); i++) {
      int slotX = x * cellSizeW + (i % slotsPerSide) * slotW;
      int slotY = top + (i / slotsPerSide) * slotH;
      g.setColor(colorOf(here.get(i)));
      // the circle is centered on the shorter side of the container
      int diameter = Math.min(slotW, slotH) - 2 * gap;
      g.fillOval(
          slotX + (slotW - diameter) / 2 + 1,
          slotY + (slotH - diameter) / 2 + 1,
          diameter,
          diameter);
      if (here.get(i) instanceof Pilot pilot) {
        BufferedImage icon = RESOURCE_ICONS.get(pilot.resource());
        int iconSize = diameter;
        int iconX = slotX + (slotW - iconSize) / 2 + 1;
        int iconY = slotY + (slotH - iconSize) / 2 + 1;
        g.drawImage(icon, iconX, iconY, iconSize, iconSize, null);
      }
    }
  }

  @Override
  public void log(String message) {
    log(LogCategory.NON_GUI, message);
  }

  @Override
  public void log(LogCategory category, String message) {
    SwingUtilities.invokeLater(() -> side.log(category, message));
  }

  @Override
  public DebateEventListener getDebateListener() {
    return side.getDebateListener();
  }

  @Override
  public void setOnSpeedChange(IntConsumer onSpeedChange) {
    SwingUtilities.invokeLater(() -> side.setOnSpeedChange(onSpeedChange));
  }

  @Override
  public void setOnSpawnChanceChange(IntConsumer onSpawnChanceChange) {
    SwingUtilities.invokeLater(() -> side.setOnSpawnChanceChange(onSpawnChanceChange));
  }

  @Override
  public void setOnMaxEmergenciesChange(IntConsumer onMaxEmergenciesChange) {
    SwingUtilities.invokeLater(() -> side.setOnMaxEmergenciesChange(onMaxEmergenciesChange));
  }

  /** Draws a station as a striped cell */
  private void drawStation(Graphics g, int x, int y) {
    drawStripes(g, x * cellSizeW + 1, y * cellSizeH + 1, cellSizeW - 1, cellSizeH - 1);
  }

  /** Draws the striped square of a station, also used by the legend */
  static void drawStripes(Graphics g, int left, int top, int width, int height) {
    // avoiding painting outside requires a nel canva for the square
    Graphics cell = g.create(left, top, width, height);
    cell.setColor(STATION_COLOR);
    for (int i = -height; i < width; i += STRIPE_GAP_PX) {
      cell.drawLine(i, height, i + height, 0);
    }
    cell.drawRect(0, 0, width - 1, height - 1);
    cell.dispose();
  }

  private Optional<Location> cellAt(Point point) {
    Location cell = new Location(point.x / cellSizeW, point.y / cellSizeH);
    return model.inGrid(cell) ? Optional.of(cell) : Optional.empty();
  }

  private static Color colorOf(Responder responder) {
    return switch (responder) {
      case Rescuer r -> AgentType.RESCUER.getColor();
      case Pilot p -> AgentType.PILOT.getColor();
    };
  }

  private static Map<Resource, BufferedImage> loadResourceIcons() {
    Map<Resource, BufferedImage> icons = new EnumMap<>(Resource.class);
    for (Resource resource : Resource.values()) {
      try (InputStream stream =
          ViewImpl.class.getClassLoader().getResourceAsStream(resource.path)) {
        if (stream == null) {
          throw new IllegalStateException("Missing resource icon: " + resource.path);
        }
        BufferedImage image = ImageIO.read(stream);
        if (image == null) {
          throw new IllegalStateException("Unsupported resource icon: " + resource.path);
        }
        icons.put(resource, image);
      } catch (IOException exception) {
        throw new ExceptionInInitializerError(exception);
      }
    }
    return Collections.unmodifiableMap(icons);
  }

  private SimModel model() {
    return (SimModel) this.model;
  }

  static Color toColor(String colorCode) {
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
