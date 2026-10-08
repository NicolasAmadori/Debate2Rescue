package model;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Represents the test model for the emergency simulation. */
public class TestModel {
  private final Dimension size;
  private final List<String> emergencies = new ArrayList<>();

  public TestModel(Dimension size) {
    this.size = size;
  }

  public Dimension getSize() {
    return size;
  }

  public List<String> getEmergencies() {
    return emergencies;
  }

  public void addEmergency(String emergency) {
    emergencies.add(emergency);
  }
}
