package model;

import config.Config;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.Location;
import java.util.List;

public class SimModelImpl extends GridWorldModel implements SimModel {

  public static final int STATION = 8;

  private final List<Location> stations;

  protected SimModelImpl(int w, int h) {
    super(w, h, 0);
    stations = List.of();
    int numS = (int) Math.round(((w + h) / 2) * Config.STATION_FREQ);
    for (int i = 0; i < numS; i++) {
      Location pos = getFreePos(STATION);
      stations.add(pos);
      add(STATION, pos);
    }
  }

  @Override
  public List<Location> getStations() {
    return List.copyOf(stations);
  }
}
