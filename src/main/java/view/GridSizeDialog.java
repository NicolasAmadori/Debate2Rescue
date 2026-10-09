package view;

import config.ConfigValues;
import java.awt.GridLayout;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Asks the grid size before the simulation starts */
public final class GridSizeDialog {

  // Grid values
  private static final int MIN_SIZE = 5;
  private static final int MAX_SIZE = 50;
  private static final int DEFAULT_SIZE = 20;
  // Stations values
  private static final int STATIONS_MIN = 1;
  private static final int STATIONS_MAX = 10;
  private static final int DEFAULT_STATIONS = 2;
  // Rescuers values
  private static final int RESCUERS_MIN = 5;
  private static final int RESCUERS_MAX = 50;
  private static final int DEFAULT_RESCUERS = 20;
  // Pilots values
  private static final int PILOTS_MIN = 5;
  private static final int PILOTS_MAX = 50;
  private static final int DEFAULT_PILOTS = 20;

  private GridSizeDialog() {}

  /**
   * Shows the dialog and waits for the user
   *
   * @return the chosen columns (width) and rows (height), or empty if the user cancelled
   */
  public static Optional<ConfigValues> ask() {
    JSpinner columns = new JSpinner(new SpinnerNumberModel(DEFAULT_SIZE, MIN_SIZE, MAX_SIZE, 1));
    JSpinner rows = new JSpinner(new SpinnerNumberModel(DEFAULT_SIZE, MIN_SIZE, MAX_SIZE, 1));
    JSpinner stations =
        new JSpinner(new SpinnerNumberModel(DEFAULT_STATIONS, STATIONS_MIN, STATIONS_MAX, 1));
    JSpinner rescuers =
        new JSpinner(new SpinnerNumberModel(DEFAULT_RESCUERS, RESCUERS_MIN, RESCUERS_MAX, 1));
    JSpinner pilots =
        new JSpinner(new SpinnerNumberModel(DEFAULT_PILOTS, PILOTS_MIN, PILOTS_MAX, 1));

    JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
    panel.add(new JLabel("Columns"));
    panel.add(columns);
    panel.add(new JLabel("Rows"));
    panel.add(rows);
    panel.add(new JLabel("Stations count"));
    panel.add(stations);
    panel.add(new JLabel("Rescuers count"));
    panel.add(rescuers);
    panel.add(new JLabel("Pilots count"));
    panel.add(pilots);

    int choice =
        JOptionPane.showConfirmDialog(
            null, panel, "Grid size", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (choice != JOptionPane.OK_OPTION) {
      return Optional.empty();
    }
    ConfigValues config =
        new ConfigValues(
            (int) columns.getValue(),
            (int) rows.getValue(),
            (int) stations.getValue(),
            (int) rescuers.getValue(),
            (int) pilots.getValue());
    return Optional.of(config);
  }
}
