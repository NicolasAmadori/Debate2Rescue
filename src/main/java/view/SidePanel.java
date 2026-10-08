package view;

import java.awt.BorderLayout;
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
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final JSlider speedSlider = new JSlider(MIN_DELAY_MS, MAX_DELAY_MS, START_DELAY_MS);
  private final JTextArea logArea = new JTextArea();

  SidePanel() {
    super(new BorderLayout(0, 8));
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    setPreferredSize(new Dimension(320, 400));

    // moving the slider to the right speeds up the simulation
    speedSlider.setInverted(true);
    speedSlider.setBorder(BorderFactory.createTitledBorder("Speed"));
    add(speedSlider, BorderLayout.NORTH);

    logArea.setEditable(false);
    logArea.setLineWrap(true);
    logArea.setWrapStyleWord(true);
    logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    JScrollPane logScroll = new JScrollPane(logArea);
    logScroll.setBorder(BorderFactory.createTitledBorder("Events"));
    add(logScroll, BorderLayout.CENTER);
  }

  void log(String message) {
    logArea.append(LocalTime.now().format(TIME_FORMAT) + "  " + message + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  void setOnSpeedChange(IntConsumer onSpeedChange) {
    speedSlider.addChangeListener(e -> onSpeedChange.accept(speedSlider.getValue()));
    onSpeedChange.accept(speedSlider.getValue());
  }
}
