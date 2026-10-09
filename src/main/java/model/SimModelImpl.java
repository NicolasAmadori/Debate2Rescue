package model;

import config.Config;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.Location;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SimModelImpl extends GridWorldModel implements SimModel {

  public static final int STATION = 8;
  public static final int EMERGENCY = 16;
  public static final int MAX_VICTIMS = 15;

  private final List<Location> stations;
  private final Map<Integer, Emergency> emergencies = new LinkedHashMap<>();
  private final Map<String, Responder> responders = new LinkedHashMap<>();
  private final EmergencyType[] emergencyTypes = EmergencyType.values();
  private final Severity[] severities = Severity.values();

  private int nextEmergencyId = 1;

  protected SimModelImpl(int w, int h) {
    super(w, h, 0);
    stations = new ArrayList<>();
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

  @Override
  public synchronized List<Emergency> getEmergencies() {
    return List.copyOf(emergencies.values());
  }

  @Override
  public synchronized Optional<Emergency> spawnEmergency() {
    Location pos = getFreePos(STATION | EMERGENCY);
    if (pos == null) {
      return Optional.empty();
    }
    Emergency emergency =
        new Emergency(
            nextEmergencyId++,
            pos,
            emergencyTypes[random.nextInt(emergencyTypes.length)],
            severities[random.nextInt(severities.length)],
            random.nextInt(MAX_VICTIMS) + 1,
            EmergencyStatus.WAITING);
    emergencies.put(emergency.id(), emergency);
    add(EMERGENCY, pos);
    return Optional.of(emergency);
  }

  @Override
  public synchronized Optional<Emergency> setEmergencyStatus(
      int emergencyId, EmergencyStatus newStatus) {
    Emergency emergency = emergencies.get(emergencyId);
    if (emergency == null) {
      return Optional.empty();
    }
    Emergency updated = emergency.newStatus(newStatus);
    emergencies.put(emergencyId, updated);
    return Optional.of(updated);
  }

  @Override
  public synchronized Optional<Emergency> getEmergencyAt(Location position) {
    return emergencies.values().stream().filter(e -> e.position().equals(position)).findFirst();
  }

  @Override
  public synchronized Optional<Emergency> resolveEmergency(int emergencyId) {
    Emergency emergency = emergencies.remove(emergencyId);
    if (emergency == null) {
      return Optional.empty();
    }
    remove(EMERGENCY, emergency.position());
    return Optional.of(emergency);
  }

  @Override
  public synchronized Responder addResponder(String name, ResponderRole role) {
    long sameRole = responders.values().stream().filter(r -> r.role() == role).count();
    Location station = stations.get((int) (sameRole % stations.size()));
    Responder responder = new Responder(name, role, station, station);
    responders.put(name, responder);
    return responder;
  }
}
