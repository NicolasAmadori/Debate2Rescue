package view;

import jason.environment.grid.Location;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.JToolTip;
import javax.swing.JWindow;
import model.Emergency;
import model.Responder;
import model.SimModel;

class CellTooltip extends MouseAdapter {

  private static final int OFFSET_PX = 16;

  private final SimModel model;
  private final Function<Point, Optional<Location>> cellAt;
  private final JWindow window;
  private final JToolTip tip = new JToolTip();

  /**
   * @param owner the window of the grid
   * @param area the component where the grid is drawn
   * @param cellAt finds the cell under a point of the area, empty if outside the grid
   * @param model the model to describe
   */
  CellTooltip(
      Window owner, Component area, Function<Point, Optional<Location>> cellAt, SimModel model) {
    this.model = model;
    this.cellAt = cellAt;
    window = new JWindow(owner);
    window.setFocusableWindowState(false);
    window.add(tip);
    area.addMouseListener(this);
    area.addMouseMotionListener(this);
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    Optional<String> text = cellAt.apply(e.getPoint()).flatMap(this::describe);
    if (text.isEmpty()) {
      window.setVisible(false);
      return;
    }
    tip.setTipText(text.get());
    window.pack();
    Point mouse = e.getLocationOnScreen();
    window.setLocation(mouse.x + OFFSET_PX, mouse.y + OFFSET_PX);
    window.setVisible(true);
  }

  @Override
  public void mouseExited(MouseEvent e) {
    window.setVisible(false);
  }

  /** The content of a cell as html, empty if there is nothing in it */
  private Optional<String> describe(Location cell) {
    List<String> lines = new ArrayList<>();
    model
        .getEmergencyAt(cell)
        .ifPresent(
            e -> {
              lines.add("<b>Emergency #" + e.id() + "</b> " + describe(e));
              lines.add(e.victims() + (e.victims() == 1 ? " victim" : " victims"));
            });
    if (model.hasObject(SimModel.STATION, cell)) {
      lines.add("<b>Station</b>");
    }
    List<String> names = model.getRespondersAt(cell).stream().map(Responder::name).toList();
    if (!names.isEmpty()) {
      lines.add("<b>Responders</b>: " + String.join(", ", names));
    }
    if (lines.isEmpty()) {
      return Optional.empty();
    }
    lines.add("<font color=gray>(" + cell.x + ", " + cell.y + ")</font>");
    return Optional.of("<html>" + String.join("<br>", lines) + "</html>");
  }

  private static String describe(Emergency e) {
    return Legend.readable(e.type().name())
        + ", "
        + Legend.readable(e.severity().name())
        + " (ESI "
        + e.severity().esi
        + "), "
        + Legend.readable(e.status().name());
  }
}
