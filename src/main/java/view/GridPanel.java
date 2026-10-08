package view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

class GridPanel extends JPanel {

  private static final Color GRID_COLOR = Color.lightGray;
  private static final Color STATION_COLOR = Color.darkGray;
  private static final Color DISASTER_COLOR = new Color(192, 57, 43);

  private final int columns;
  private final int rows;
  private final List<Point> stations = new ArrayList<>();
  private final Map<Integer, Point> disasters = new HashMap<>();
  private final Map<String, Agent> agents = new LinkedHashMap<>();

  private record Agent(AgentType type, Point position) {}

  GridPanel(int columns, int rows) {
    this.columns = columns;
    this.rows = rows;
    int cellSize = Math.max(10, 600 / Math.max(columns, rows));
    setPreferredSize(new Dimension(columns * cellSize, rows * cellSize));
    setBackground(Color.white);
  }

  void addStation(int x, int y) {
    stations.add(new Point(x, y));
    repaint();
  }

  void addAgent(String name, AgentType type, int x, int y) {
    agents.put(name, new Agent(type, new Point(x, y)));
    repaint();
  }

  void moveAgent(String name, int x, int y) {
    Agent agent = agents.get(name);
    if (agent != null) {
      agents.put(name, new Agent(agent.type(), new Point(x, y)));
      repaint();
    }
  }

  void addDisaster(int id, int x, int y) {
    disasters.put(id, new Point(x, y));
    repaint();
  }

  void removeDisaster(int id) {
    disasters.remove(id);
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    int cellSize = Math.min(getWidth() / columns, getHeight() / rows);

    g.setColor(GRID_COLOR);
    for (int x = 0; x <= columns; x++) {
      g.drawLine(x * cellSize, 0, x * cellSize, rows * cellSize);
    }
    for (int y = 0; y <= rows; y++) {
      g.drawLine(0, y * cellSize, columns * cellSize, y * cellSize);
    }

    g.setColor(STATION_COLOR);
    for (Point station : stations) {
      g.fillRect(station.x * cellSize + 1, station.y * cellSize + 1, cellSize - 1, cellSize - 1);
    }

    for (Map.Entry<Integer, Point> disaster : disasters.entrySet()) {
      Point p = disaster.getValue();
      g.setColor(DISASTER_COLOR);
      g.fillRect(p.x * cellSize + 1, p.y * cellSize + 1, cellSize - 1, cellSize - 1);
      g.setColor(Color.white);
      g.drawString(String.valueOf(disaster.getKey()), p.x * cellSize + 3, p.y * cellSize + 12);
    }

    drawAgents(g, cellSize);

    // Linux fix, the movements are not always painted causing skips
    Toolkit.getDefaultToolkit().sync();
  }

  /** Several agents can share a cell, so each cell is split into slots. */
  private void drawAgents(Graphics g, int cellSize) {
    Map<Point, List<Agent>> agentsByCell = new HashMap<>();
    for (Agent agent : agents.values()) {
      agentsByCell.computeIfAbsent(agent.position(), p -> new ArrayList<>()).add(agent);
    }

    for (Map.Entry<Point, List<Agent>> cell : agentsByCell.entrySet()) {
      Point p = cell.getKey();
      List<Agent> inCell = cell.getValue();
      int slotsPerSide = (int) Math.ceil(Math.sqrt(inCell.size()));
      int slotSize = cellSize / slotsPerSide;
      int gap = Math.max(1, slotSize / 8);
      for (int i = 0; i < inCell.size(); i++) {
        int slotX = p.x * cellSize + (i % slotsPerSide) * slotSize;
        int slotY = p.y * cellSize + (i / slotsPerSide) * slotSize;
        g.setColor(inCell.get(i).type().getColor());
        g.fillRect(slotX + gap + 1, slotY + gap + 1, slotSize - 2 * gap, slotSize - 2 * gap);
      }
    }
  }
}
