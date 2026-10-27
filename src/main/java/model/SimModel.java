package model;

import config.Config;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.Location;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SimModel extends GridWorldModel {

  public static final int STATION = 8;
  public static final int EMERGENCY = 16;
  public static final int MAX_VICTIMS = 15;

  private final List<Location> stations;
  private final List<Location> obstacles;
  private final Map<Integer, Emergency> emergencies = new LinkedHashMap<>();
  private final Map<String, Responder> responders = new LinkedHashMap<>();
  private final PathFinder pathFinder;
  private final EmergencyType[] emergencyTypes = EmergencyType.values();
  private final Severity[] severities = Severity.values();
  private final Size[] sizes = Size.values();
  private final Resource[] resources = Resource.values();

  private int nextEmergencyId = 1;

  public SimModel(int width, int height, int numStations, int numObstacles) {
    super(width, height, 0);
    this.pathFinder = new PathFinder(this);
    stations = new ArrayList<>();
    for (int i = 0; i < numStations; i++) {
      Location pos = getFreePos(STATION);
      stations.add(pos);
      add(STATION, pos);
    }
    obstacles = new ArrayList<>();
    for (int i = 0; i < numObstacles; i++) {
      Location pos = getFreePos(OBSTACLE | STATION);
      obstacles.add(pos);
      add(OBSTACLE, pos);
    }
  }

  /***
   * Getter method
   * @return the list of locations of the stations on the grid.
   */
  public List<Location> getStations() {
    return List.copyOf(stations);
  }

  /***
   * Getter method
   * @return the list of locations of the obstacles on the grid.
   */
  public List<Location> getObstacles() {
    return List.copyOf(obstacles);
  }

  /***
   * Getter method
   * @return the list of emergencies on the grid.
   */
  public synchronized List<Emergency> getEmergencies() {
    return List.copyOf(emergencies.values());
  }

  /***
   * Getter method
   * @return a free grid position if present, or empty if there are no free cells left.
   */
  public synchronized Optional<Location> getFreePosition() {
    Location pos = getFreePos(OBSTACLE | STATION | EMERGENCY | AGENT);
    return Optional.ofNullable(pos);
  }

  /***
   * Creates an emergency with random properties in a cell without stations and emergencies.
   *
   * @return the new emergency, or empty if there are no free cells left.
   */
  public synchronized Optional<Emergency> spawnEmergency() {
    Location pos = getFreePos(OBSTACLE | STATION | EMERGENCY);
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
            random.nextDouble() < Config.TRAPPED_PROB,
            random.nextDouble() < Config.ESCALATING_PROB,
            sizes[random.nextInt(sizes.length)],
            random.nextDouble() < Config.COLLATERAL_RISK_PROB,
            EmergencyStatus.WAITING);
    emergencies.put(emergency.id(), emergency);
    add(EMERGENCY, pos);
    return Optional.of(emergency);
  }

  /***
   * Sets the status of an emergency.
   *
   * @param emergencyId the emergency identifier
   * @param newStatus the new status
   * @return the updated emergency, or empty if not found.
   */
  public synchronized Optional<Emergency> setEmergencyStatus(
      int emergencyId, EmergencyStatus newStatus) {
    Emergency emergency = emergencies.get(emergencyId);
    if (emergency == null) {
      return Optional.empty();
    }
    Emergency updated = emergency.newStatus(newStatus);
    emergencies.put(emergencyId, updated);
    updateCell(updated.position());
    return Optional.of(updated);
  }

  /***
   * Gets the emergency at a given position.
   *
   * @param position the location to check
   * @return the emergency at the position, or empty if not found.
   */
  public synchronized Optional<Emergency> getEmergencyAt(Location position) {
    return emergencies.values().stream().filter(e -> e.position().equals(position)).findFirst();
  }

  /***
   * Declares an emergency over and removes it from the grid.
   * @param emergencyId the emergency identifier
   * @return the removed emergency, or empty if it does not exist.
   */
  public synchronized Optional<Emergency> resolveEmergency(int emergencyId) {
    Emergency emergency = emergencies.remove(emergencyId);
    if (emergency == null) {
      return Optional.empty();
    }
    remove(EMERGENCY, emergency.position());
    return Optional.of(emergency);
  }

  /***
   * Places a new rescuer on a station. Rescuers are spread over the stations.
   *
   * @param name the name of the rescuer
   * @return the new rescuer
   */
  public synchronized Rescuer addRescuer(String name) {
    Location station = stations.get((int) (getRescuers().size() % stations.size()));
    Rescuer rescuer = new Rescuer(name, station, station);
    responders.put(name, rescuer);
    updateCell(station);
    return rescuer;
  }

  /***
   * Places a new pilot on a station. Pilots are spread over the stations.
   *
   * @param name the name of the pilot
   * @return the new pilot
   */
  public synchronized Pilot addPilot(String name) {
    int pilotsSize = getPilots().size();
    Location station = stations.get((int) (pilotsSize % stations.size()));
    Resource resource = resources[(int) (pilotsSize % resources.length)];
    Pilot pilot = new Pilot(name, station, station, resource);
    responders.put(name, pilot);
    updateCell(station);
    return pilot;
  }

  /***
   * Moves a responder towards the target following a Bresenham line and avoiding obstacles.
   * Rescuers move by one cell, pilots by two.
   * @param name the name of the responder
   * @param target the target location
   * @return false if the responder is not found, true otherwise
   */
  public synchronized boolean moveTowards(String name, Location target) {
    Responder responder = responders.get(name);
    if (responder == null) {
      return false;
    }
    Location from = responder.position();
    Location to = pathFinder.nextPosition(responder, target);
    if (!to.equals(from)) {
      responders.put(name, responder.moveTo(to));
      updateCell(from);
      updateCell(to);
    }
    return true;
  }

  /***
   * Getter method.
   * @param name the name of the responder
   * @return the responder with the given name, or empty if not found.
   */
  public synchronized Optional<Responder> getResponder(String name) {
    return Optional.ofNullable(responders.get(name));
  }

  /***
   * Returns a list of all responders currently in the simulation.
   * @return a list of all responders
   */
  public synchronized List<Responder> getResponders() {
    return List.copyOf(responders.values());
  }

  /***
   * Returns a list of all rescuers currently in the simulation.
   * @return a list of all rescuers
   */
  public synchronized List<Rescuer> getRescuers() {
    return responders.values().stream()
        .filter(Rescuer.class::isInstance)
        .map(Rescuer.class::cast)
        .toList();
  }

  /***
   * Returns a list of all pilots currently in the simulation.
   * @return a list of all pilots
   */
  public synchronized List<Pilot> getPilots() {
    return responders.values().stream()
        .filter(Pilot.class::isInstance)
        .map(Pilot.class::cast)
        .toList();
  }

  /***
   * Gets the responder at a given position.
   *
   * @param position
   * @return the responder at the position, or empty if not found.
   */
  public synchronized List<Responder> getRespondersAt(Location position) {
    return responders.values().stream().filter(r -> r.position().equals(position)).toList();
  }

  /***
   * Gets the rescuers at a given position.
   *
   * @param position
   * @return the rescuers at the position, or empty if not found.
   */
  public synchronized List<Rescuer> getRescuersAt(Location position) {
    return getRescuers().stream().filter(r -> r.position().equals(position)).toList();
  }

  /***
   * Gets the pilots at a given position.
   *
   * @param position
   * @return the pilots at the position, or empty if not found.
   */
  public synchronized List<Pilot> getPilotsAt(Location position) {
    return getPilots().stream().filter(r -> r.position().equals(position)).toList();
  }

  /***
   * Private method to safely update cell state in case of multiple agents.
   * @param cell to update
   */
  private void updateCell(Location cell) {
    if (getRespondersAt(cell).isEmpty()) {
      data[cell.x][cell.y] &= ~AGENT; // remove agent bit
    } else {
      data[cell.x][cell.y] |= AGENT; // add agent bit, if not already present
    }
    if (view != null) {
      view.update(cell.x, cell.y);
    }
  }
}
