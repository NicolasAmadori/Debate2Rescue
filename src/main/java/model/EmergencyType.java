package model;

public enum EmergencyType {
  FIRE("red"),
  MEDICAL("blue"),
  HOSTAGE("pink"),
  HAZMAT("brown"),
  BOMB("black"),
  AGGRESSION("white"),
  EVACUATION("green"),
  ARMED_PERSON("gray"),
  DISASTER("orange"),
  MISSING("yellow");

  public final String colorCode;

  private EmergencyType(String colorCode) {
    this.colorCode = colorCode;
  }
}
