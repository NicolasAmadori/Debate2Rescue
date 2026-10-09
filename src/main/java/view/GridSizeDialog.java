package view;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Asks the grid size before the simulation starts */
public final class GridSizeDialog {

  private static final int MIN_SIZE = 5;
  private static final int MAX_SIZE = 50;
  private static final int DEFAULT_SIZE = 20;

  private GridSizeDialog() {}

  /**
   * Shows the dialog and waits for the user
   *
   * @return the chosen columns (width) and rows (height), or empty if the user cancelled
   */
  public static Optional<Dimension> ask() {
    JSpinner columns = new JSpinner(new SpinnerNumberModel(DEFAULT_SIZE, MIN_SIZE, MAX_SIZE, 1));
    JSpinner rows = new JSpinner(new SpinnerNumberModel(DEFAULT_SIZE, MIN_SIZE, MAX_SIZE, 1));

    JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
    panel.add(new JLabel("Columns"));
    panel.add(columns);
    panel.add(new JLabel("Rows"));
    panel.add(rows);

    int choice =
        JOptionPane.showConfirmDialog(
            null, panel, "Grid size", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (choice != JOptionPane.OK_OPTION) {
      return Optional.empty();
    }
    return Optional.of(new Dimension((int) columns.getValue(), (int) rows.getValue()));
  }
}
