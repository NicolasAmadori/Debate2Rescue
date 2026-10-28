package view;

/** Categorizes simulation log events to determine their visual hierarchy in the view. */
public enum LogCategory {
  /** Events that are already visually represented on the 2D grid canvas (e.g. spawns, joins). */
  GUI_VISIBLE,

  /**
   * Internal simulation events not depicted on the grid (e.g. assignments, resolutions, agent
   * messages).
   */
  NON_GUI
}
