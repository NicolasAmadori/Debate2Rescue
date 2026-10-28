package view;

import arg.DebateEventListener;
import arg.DebateLogger;
import config.Config;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultStyledDocument;
import model.SimModel;

class SidePanel extends JPanel {

  private static final int MIN_DELAY_MS = 50;
  private static final int MAX_DELAY_MS = 1000;
  private static final int START_DELAY_MS = 300;
  private static final int START_SPAWN_CHANCE = 30;
  private static final double WIDTH_RATIO = 0.5;
  private static final int MIN_WIDTH = 720;
  private static final int CONTROLS_WIDTH = 260;

  private final JSlider speedSlider = new JSlider(MIN_DELAY_MS, MAX_DELAY_MS, START_DELAY_MS);
  private final JSlider spawnSlider = new JSlider(0, 100, START_SPAWN_CHANCE);
  private final JSlider maxEmergenciesSlider;

  // Simulation Events (Top 50%)
  private final JTextPane simTextPane = new JTextPane();
  private final DefaultStyledDocument simDoc = new DefaultStyledDocument();
  private final JScrollPane simScroll = new JScrollPane(simTextPane);

  // Debate Arena (Bottom 50%)
  private final JTextPane debateTextPane = new JTextPane();
  private final JScrollPane debateScroll = new JScrollPane(debateTextPane);
  private final DefaultComboBoxModel<DebateSessionItem> debateComboModel =
      new DefaultComboBoxModel<>();
  private final JComboBox<DebateSessionItem> debateSelector = new JComboBox<>(debateComboModel);

  // Debate tracking: debateId -> Document & Metadata
  private final Map<Integer, DefaultStyledDocument> debateDocs = new ConcurrentHashMap<>();
  private final Map<Integer, DebateSessionItem> debateItems = new ConcurrentHashMap<>();
  private final Map<Integer, Map<Integer, String>> debateContenders = new ConcurrentHashMap<>();
  private final Map<Integer, Map<Integer, Integer>> debateContenderIndices =
      new ConcurrentHashMap<>();

  private final JSplitPane splitPane;
  private final DebateEventListener debateListener = new ViewDebateListener();

  SidePanel(int gridWidth, int gridHeight) {
    super(new BorderLayout(8, 0));
    setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
    setPreferredSize(new Dimension(MIN_WIDTH, 550));

    // Sliders
    speedSlider.setInverted(true);
    speedSlider.setBorder(BorderFactory.createTitledBorder("Speed"));
    spawnSlider.setBorder(BorderFactory.createTitledBorder(spawnTitle()));
    maxEmergenciesSlider =
        new JSlider(
            1,
            SimModel.getMaxEmergencyLimit(),
            Math.min(
                Config.getMaxNumEmergencies(gridWidth, gridHeight),
                SimModel.getMaxEmergencyLimit()));
    maxEmergenciesSlider.setBorder(BorderFactory.createTitledBorder(maxEmergenciesTitle()));

    JPanel sliders = new JPanel(new BorderLayout(0, 8));
    sliders.add(speedSlider, BorderLayout.NORTH);
    sliders.add(spawnSlider, BorderLayout.CENTER);
    sliders.add(maxEmergenciesSlider, BorderLayout.SOUTH);

    JPanel top = new JPanel(new BorderLayout(0, 8));
    top.add(sliders, BorderLayout.NORTH);
    top.add(new Legend(), BorderLayout.CENTER);

    JPanel controls = new JPanel(new BorderLayout());
    controls.setPreferredSize(new Dimension(CONTROLS_WIDTH, 0));
    controls.add(top, BorderLayout.NORTH);
    add(controls, BorderLayout.WEST);

    // Setup Simulation Events (Top Pane)
    simTextPane.setEditable(false);
    simTextPane.setStyledDocument(simDoc);
    simScroll.setBorder(BorderFactory.createTitledBorder("Simulation Events"));

    // Setup Debate Arena (Bottom Pane)
    debateTextPane.setEditable(false);
    DefaultStyledDocument initialDebateDoc = new DefaultStyledDocument();
    LogFormatter.append(
        initialDebateDoc,
        "No debates started yet. Debates will appear here when resource contention occurs.\n",
        LogFormatter.createStyle(11, false, true, Color.GRAY));
    debateTextPane.setStyledDocument(initialDebateDoc);
    debateScroll.setBorder(BorderFactory.createTitledBorder("Debate Transcript"));

    JPanel debateHeader = new JPanel(new BorderLayout(6, 0));
    debateHeader.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
    JLabel selectorLabel = new JLabel("Debate Arena: ");
    selectorLabel.setFont(selectorLabel.getFont().deriveFont(java.awt.Font.BOLD));
    debateHeader.add(selectorLabel, BorderLayout.WEST);
    debateHeader.add(debateSelector, BorderLayout.CENTER);

    debateSelector.addActionListener(
        e -> {
          DebateSessionItem selected = (DebateSessionItem) debateSelector.getSelectedItem();
          if (selected != null) {
            DefaultStyledDocument doc = debateDocs.get(selected.id);
            if (doc != null) {
              debateTextPane.setStyledDocument(doc);
              debateTextPane.setCaretPosition(doc.getLength());
            }
          }
        });

    JPanel debatePanel = new JPanel(new BorderLayout(0, 4));
    debatePanel.add(debateHeader, BorderLayout.NORTH);
    debatePanel.add(debateScroll, BorderLayout.CENTER);

    // 50-50 Vertical Split Pane
    splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, simScroll, debatePanel);
    splitPane.setResizeWeight(0.5);
    splitPane.setContinuousLayout(true);
    splitPane.setBorder(null);

    // Keep split at 50% upon initial display and resizing
    addComponentListener(
        new ComponentAdapter() {
          @Override
          public void componentResized(ComponentEvent e) {
            splitPane.setDividerLocation(0.5);
          }
        });

    add(splitPane, BorderLayout.CENTER);
  }

  @Override
  public Dimension getPreferredSize() {
    Container parent = getParent();
    if (parent == null || parent.getWidth() == 0) {
      return super.getPreferredSize();
    }
    int width = Math.max(MIN_WIDTH, (int) (parent.getWidth() * WIDTH_RATIO));
    return new Dimension(width, super.getPreferredSize().height);
  }

  void log(String message) {
    log(LogCategory.NON_GUI, message);
  }

  void log(LogCategory category, String message) {
    LogFormatter.appendSimulationEvent(simDoc, category, message);
    simTextPane.setCaretPosition(simDoc.getLength());
  }

  DebateEventListener getDebateListener() {
    return debateListener;
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

  /** Represents an entry in the debate dropdown selector. */
  static class DebateSessionItem {
    final int id;
    final String resourceType;
    boolean finished = false;
    int winner = -1;
    String winningRescuer = null;

    DebateSessionItem(int id, String resourceType) {
      this.id = id;
      this.resourceType = resourceType;
    }

    @Override
    public String toString() {
      if (!finished) {
        return String.format("Debate #%d [%s] - IN PROGRESS", id, resourceType.toUpperCase());
      } else {
        String winnerDesc =
            winningRescuer != null ? (winningRescuer + " for Em #" + winner) : ("Em #" + winner);
        return String.format(
            "Debate #%d [%s] - WON by %s", id, resourceType.toUpperCase(), winnerDesc);
      }
    }
  }

  /** Handles incoming debate events and updates both simulation and debate views. */
  private class ViewDebateListener implements DebateEventListener {

    @Override
    public void onDebateStart(int debateId, String resourceType, List<String> contenders) {
      onDebateStart(debateId, resourceType, contenders, Collections.emptyList());
    }

    @Override
    public void onDebateStart(
        int debateId, String resourceType, List<String> contenders, List<String> emergencyInfos) {
      SwingUtilities.invokeLater(
          () -> {
            // Map contenders to their rescuer names and assign index for coloring
            Map<Integer, String> emToRescuer = new LinkedHashMap<>();
            Map<Integer, Integer> emToIndex = new HashMap<>();
            for (int i = 0; i < contenders.size(); i++) {
              String c = contenders.get(i);
              Integer emId = DebateLogger.extractEmergencyId(c);
              String rescuer = DebateLogger.extractRescuerName(c);
              if (emId != null) {
                emToRescuer.put(emId, rescuer);
                emToIndex.put(emId, i);
              }
            }
            debateContenders.put(debateId, emToRescuer);
            debateContenderIndices.put(debateId, emToIndex);

            // Create dedicated StyledDocument for this debate session
            DefaultStyledDocument doc = new DefaultStyledDocument();
            debateDocs.put(debateId, doc);

            // 1. Log rich Debate Start banner with emergency info recap in the debate's own
            // transcript
            LogFormatter.appendDebateStartBanner(
                doc, debateId, resourceType, contenders, emergencyInfos);

            // 2. Log single-line Debate Start event in the top Simulation Events pane
            LogFormatter.appendDebateStartOneLine(simDoc, debateId, resourceType, contenders);
            simTextPane.setCaretPosition(simDoc.getLength());

            // Create and insert into dropdown selector in DESCENDING order (highest debateId at
            // top)
            DebateSessionItem item = new DebateSessionItem(debateId, resourceType);
            debateItems.put(debateId, item);

            // Insert at index 0 because higher ID is newest
            debateComboModel.insertElementAt(item, 0);
            debateSelector.setSelectedItem(item);
            debateTextPane.setStyledDocument(doc);
            debateTextPane.setCaretPosition(doc.getLength());
          });
    }

    @Override
    public void onDebateMove(
        int debateId, int moveId, String rescuer, int emergency, String type, String content) {
      SwingUtilities.invokeLater(
          () -> {
            DefaultStyledDocument doc = debateDocs.get(debateId);
            if (doc == null) {
              return;
            }
            Map<Integer, Integer> indices = debateContenderIndices.get(debateId);
            int contenderIndex = indices != null ? indices.getOrDefault(emergency, 0) : 0;

            // Log move in the debate's own transcript ONLY (not in top simulation pane)
            LogFormatter.appendDebateMove(
                doc, moveId, rescuer, emergency, type, content, contenderIndex);

            // Auto-scroll if this debate is currently active in the viewer
            DebateSessionItem selected = (DebateSessionItem) debateSelector.getSelectedItem();
            if (selected != null && selected.id == debateId) {
              debateTextPane.setCaretPosition(doc.getLength());
            }
          });
    }

    @Override
    public void onDebateStatus(int debateId, int currentWinner, String reason) {
      SwingUtilities.invokeLater(
          () -> {
            DefaultStyledDocument doc = debateDocs.get(debateId);
            if (doc == null) {
              return;
            }
            // Log status evaluation in the debate's own transcript ONLY
            LogFormatter.appendDebateStatus(doc, currentWinner, reason);

            DebateSessionItem selected = (DebateSessionItem) debateSelector.getSelectedItem();
            if (selected != null && selected.id == debateId) {
              debateTextPane.setCaretPosition(doc.getLength());
            }
          });
    }

    @Override
    public void onDebateEnd(
        int debateId, String resourceType, int winner, List<String> losers, String justification) {
      SwingUtilities.invokeLater(
          () -> {
            DefaultStyledDocument doc = debateDocs.get(debateId);
            Map<Integer, String> emToRescuer = debateContenders.get(debateId);
            String winningRescuer = emToRescuer != null ? emToRescuer.get(winner) : null;

            // Update dropdown item status
            DebateSessionItem item = debateItems.get(debateId);
            if (item != null) {
              item.finished = true;
              item.winner = winner;
              item.winningRescuer = winningRescuer;
              debateSelector.repaint();
            }

            // 1. Log rich Debate Concluded banner in the debate's own transcript
            if (doc != null) {
              LogFormatter.appendDebateEndBanner(
                  doc, debateId, resourceType, winner, winningRescuer, losers, justification);
              DebateSessionItem selected = (DebateSessionItem) debateSelector.getSelectedItem();
              if (selected != null && selected.id == debateId) {
                debateTextPane.setCaretPosition(doc.getLength());
              }
            }

            // 2. Log single-line Debate Concluded event in the top Simulation Events pane
            LogFormatter.appendDebateEndOneLine(
                simDoc, debateId, resourceType, winner, winningRescuer, justification);
            simTextPane.setCaretPosition(simDoc.getLength());
          });
    }
  }
}
