package model;

import jason.environment.grid.GridWorldModel;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/** Represents the test model for the emergency simulation. */
public class TestModel extends GridWorldModel {
  public static final int STATION = 8;
  public static final int EMERGENCY = 16;
  private final Dimension size;
  private final int nStations;
  private final int nRescuers;
  private final int nPilots;
  private final List<String> emergencies = new ArrayList<>();

  public TestModel(Dimension size, int nStations, int nRescuers, int nPilots) {
    super(size.width, size.height, nRescuers + nPilots);
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
