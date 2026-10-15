package view;

import config.Config;
import config.ConfigValues;
import java.awt.GridLayout;
import java.util.Optional;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Asks the grid size before the simulation starts */
public final class SimConfigDialog {

  // Grid values
  private static final int MIN_SIZE = 5;
  private static final int MAX_SIZE = 50;
  private static final int DEFAULT_SIZE = 20;
  // Stations values
  private static final int STATIONS_MIN = 1;
  private static final int STATIONS_MAX = Config.getMaxNumStations(MAX_SIZE, MAX_SIZE);
  private static final int DEFAULT_STATIONS = Config.getNumStations(DEFAULT_SIZE, DEFAULT_SIZE);
  // Rescuers values
  private static final int RESCUERS_MIN = 1;
  private static final int RESCUERS_MAX = STATIONS_MAX * Config.MAX_RESCUER_FREQ;
  private static final int DEFAULT_RESCUERS = DEFAULT_STATIONS * Config.RESCUER_FREQ;
  // Pilots values
  private static final int PILOTS_MIN = 1;
  private static final int PILOTS_MAX = STATIONS_MAX * Config.MAX_PILOT_FREQ;
  private static final int DEFAULT_PILOTS = DEFAULT_STATIONS * Config.PILOT_FREQ;

  private SimConfigDialog() {}

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
            null,
            panel,
            "Simulation configuration",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
    if (choice != JOptionPane.OK_OPTION) {
      return Optional.empty();
    }
    int columnsValue = (int) columns.getValue();
    int rowsValue = (int) rows.getValue();
    int stationsValue =
        Math.min((int) stations.getValue(), Config.getMaxNumStations(columnsValue, rowsValue));
    int rescuersValue =
        Math.min((int) rescuers.getValue(), stationsValue * Config.MAX_RESCUER_FREQ);
    rescuersValue = Math.max(rescuersValue, stationsValue); // At least one rescuer per station
    int pilotsValue = Math.min((int) pilots.getValue(), stationsValue * Config.MAX_PILOT_FREQ);
    pilotsValue = Math.max(pilotsValue, stationsValue); // At least one pilot per station

    ConfigValues config =
        new ConfigValues(columnsValue, rowsValue, stationsValue, rescuersValue, pilotsValue);
    return Optional.of(config);
  }
}
