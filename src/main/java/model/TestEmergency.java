package model;

public class TestEmergency {
  private final int id;
  private final int severity;
  private final int x;
  private final int y;
  private final long timestamp;

  public TestEmergency(int id, int severity, int x, int y) {
    this.id = id;
    this.severity = severity;
    this.x = x;
    this.y = y;
    this.timestamp = System.currentTimeMillis();
  }

  public int getId() {
    return id;
  }

  public int getSeverity() {
    return severity;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return String.format("emergency(%d, %d, %d, %d)", id, severity, x, y);
  }
}
