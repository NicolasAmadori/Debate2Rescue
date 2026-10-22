package view;

import config.Config;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;

class SidePanel extends JPanel {

  private static final int MIN_DELAY_MS = 50;
  private static final int MAX_DELAY_MS = 1000;
  private static final int START_DELAY_MS = 300;
  private static final int START_SPAWN_CHANCE = 30;
  private static final double WIDTH_RATIO = 0.5;
  private static final int MIN_WIDTH = 650;
  private static final int CONTROLS_WIDTH = 260;
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final JSlider speedSlider = new JSlider(MIN_DELAY_MS, MAX_DELAY_MS, START_DELAY_MS);
  private final JSlider spawnSlider = new JSlider(0, 100, START_SPAWN_CHANCE);
  private final JSlider maxEmergenciesSlider;
  private final JTextArea logArea = new JTextArea();

  SidePanel(int gridWidth, int gridHeight) {
    super(new BorderLayout(8, 0));
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    setPreferredSize(new Dimension(MIN_WIDTH, 500));

    // moving the slider to the right speeds up the simulation
    speedSlider.setInverted(true);
    speedSlider.setBorder(BorderFactory.createTitledBorder("Speed"));
    spawnSlider.setBorder(BorderFactory.createTitledBorder(spawnTitle()));
    maxEmergenciesSlider =
        new JSlider(
            1,
            Config.getMaxEmergencyLimit(gridWidth, gridHeight),
            Config.getMaxNumEmergencies(gridWidth, gridHeight));
    maxEmergenciesSlider.setBorder(BorderFactory.createTitledBorder(maxEmergenciesTitle()));
    JPanel sliders = new JPanel(new BorderLayout(0, 8));
    sliders.add(speedSlider, BorderLayout.NORTH);
    sliders.add(spawnSlider, BorderLayout.CENTER);
    sliders.add(maxEmergenciesSlider, BorderLayout.SOUTH);
    JPanel top = new JPanel(new BorderLayout(0, 8));
    top.add(sliders, BorderLayout.NORTH);
    top.add(new Legend(), BorderLayout.CENTER);
    // legend needs its own panel to make controls keep
    // their height instead of stretching to the whole column
    JPanel controls = new JPanel(new BorderLayout());
    controls.setPreferredSize(new Dimension(CONTROLS_WIDTH, 0));
    controls.add(top, BorderLayout.NORTH);
    add(controls, BorderLayout.WEST);

    logArea.setEditable(false);
    logArea.setLineWrap(true);
    logArea.setWrapStyleWord(true);
    logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    JScrollPane logScroll = new JScrollPane(logArea);
    logScroll.setBorder(BorderFactory.createTitledBorder("Events"));
    add(logScroll, BorderLayout.CENTER);
  }

  @Override
  public Dimension getPreferredSize() {
    // before the window is shown there is no width yet, so the fixed size is used
    Container parent = getParent();
    if (parent == null || parent.getWidth() == 0) {
      return super.getPreferredSize();
    }
    int width = Math.max(MIN_WIDTH, (int) (parent.getWidth() * WIDTH_RATIO));
    return new Dimension(width, super.getPreferredSize().height);
  }

  void log(String message) {
    logArea.append(LocalTime.now().format(TIME_FORMAT) + "  " + message + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  void setOnSpeedChange(IntConsumer onSpeedChange) {
    speedSlider.addChangeListener(e -> onSpeedChange.accept(speedSlider.getValue()));
    onSpeedChange.accept(speedSlider.getValue());
  }

  void setOnSpawnChanceChange(IntConsumer onSpawnChanceChange) {
    spawnSlider.addChangeListener(
        e -> {
          spawnSlider.setBorder(BorderFactory.createTitledBorder(spawnTitle()));
          onSpawnChanceChange.accept(spawnSlider.getValue());
        });
    onSpawnChanceChange.accept(spawnSlider.getValue());
  }

  void setOnMaxEmergenciesChange(IntConsumer onMaxEmergenciesChange) {
    maxEmergenciesSlider.addChangeListener(
        e -> {
          maxEmergenciesSlider.setBorder(BorderFactory.createTitledBorder(maxEmergenciesTitle()));
          onMaxEmergenciesChange.accept(maxEmergenciesSlider.getValue());
        });
    onMaxEmergenciesChange.accept(maxEmergenciesSlider.getValue());
  }

  // with a chance of p% per step, an emergency spawns on average every 100 / p steps
  private String spawnTitle() {
    int chance = spawnSlider.getValue();
    if (chance == 0) {
      return "Spawn rate: 0% (no new emergencies)";
    }
    return "Spawn rate: " + chance + "% (≈ 1 every " + Math.round(100.0 / chance) + " steps)";
  }

  private String maxEmergenciesTitle() {
    return "Max emergencies: " + maxEmergenciesSlider.getValue();
  }
}
