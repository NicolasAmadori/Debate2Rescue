package model;

public enum Severity {
  HIGHEST_PRIORITY(1),
  EMERGENT(2),
  URGENT(3),
  LESS_URGENT(4),
  NON_URGENT(5);

  public final int esi;

  private Severity(int esi) {
    this.esi = esi;
  }
}
