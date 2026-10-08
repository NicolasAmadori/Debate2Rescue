package model;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Represents the test model for the emergency simulation. */
public class TestModel {
  private final Dimension size;
  private final int nStations;
  private final int nRescuers;
  private final int nPilots;
  private final List<String> emergencies = new ArrayList<>();

  public TestModel(Dimension size, int nStations, int nRescuers, int nPilots) {
    this.size = size;
    this.nStations = nStations;
    this.nRescuers = nRescuers;
    this.nPilots = nPilots;
  }

  public Dimension getSize() {
    return size;
  }

  public int getNStations() {
    return nStations;
  }

  public int getNRescuers() {
    return nRescuers;
  }

  public int getNPilots() {
    return nPilots;
  }

  public List<String> getEmergencies() {
    return emergencies;
  }

  public void addEmergency(String emergency) {
    emergencies.add(emergency);
  }
}
