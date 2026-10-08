package view;

import java.awt.BorderLayout;
import java.util.function.IntConsumer;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Swing implementation of the view. The controller can call its methods from any thread: every
 * change is passed to the Swing thread.
 */
public class ViewImpl implements View {

  private final JFrame frame = new JFrame("Debate2Rescue");
  private final GridPanel grid;
  private final SidePanel side = new SidePanel();

  public ViewImpl(int columns, int rows) {
    grid = new GridPanel(columns, rows);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(grid, BorderLayout.CENTER);
    frame.getContentPane().add(side, BorderLayout.EAST);
    frame.pack();
  }

  @Override
  public void show() {
    SwingUtilities.invokeLater(() -> frame.setVisible(true));
  }

  @Override
  public void addStation(int x, int y) {
    SwingUtilities.invokeLater(() -> grid.addStation(x, y));
  }

  @Override
  public void addAgent(String name, AgentType type, int x, int y) {
    SwingUtilities.invokeLater(() -> grid.addAgent(name, type, x, y));
  }

  @Override
  public void moveAgent(String name, int x, int y) {
    SwingUtilities.invokeLater(() -> grid.moveAgent(name, x, y));
  }

  @Override
  public void addDisaster(int id, int x, int y) {
    SwingUtilities.invokeLater(() -> grid.addDisaster(id, x, y));
  }

  @Override
  public void removeDisaster(int id) {
    SwingUtilities.invokeLater(() -> grid.removeDisaster(id));
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
