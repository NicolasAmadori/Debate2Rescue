package model;

public enum Size {
  SMALL(1),
  MEDIUM(2),
  LARGE(3);

  public final int value;

  private Size(int value) {
    this.value = value;
  }
}
