package view;

import jason.environment.grid.GridWorldView;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.function.IntConsumer;
import javax.swing.SwingUtilities;
import model.TestModel;

public class ViewImpl extends GridWorldView implements View {

  private static final Color STATION_COLOR = Color.DARK_GRAY;
  private static final Color EMERGENCY_COLOR = Color.RED;
  private static final int GRID_SIZE_PX = 600;

  private SidePanel side;

  // TODO: change the parameter type to actual model
  public ViewImpl(TestModel model) {
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
      case TestModel.STATION -> {
        g.setColor(STATION_COLOR);
        g.fillRect(x * cellSizeW + 1, y * cellSizeH + 1, cellSizeW - 1, cellSizeH - 1);
      }
      case TestModel.EMERGENCY -> {
        g.setColor(EMERGENCY_COLOR);
        g.fillRect(x * cellSizeW + 1, y + cellSizeH + 1, cellSizeW - 1, cellSizeH - 1);
      }
      default -> {}
    }
  }

  @Override
  public void drawAgent(Graphics g, int x, int y, Color c, int id) {
    // TODO: pick the color based on the agent type once the actual model is ready
    super.drawAgent(g, x, y, AgentType.RESCUER.getColor(), id);
  }

  @Override
  public void log(String message) {
    SwingUtilities.invokeLater(() -> side.log(message));
  }

  @Override
  public void setOnSpeedChange(IntConsumer onSpeedChange) {
    SwingUtilities.invokeLater(() -> side.setOnSpeedChange(onSpeedChange));
  }
}
