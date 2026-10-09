package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import config.Config;
import jason.environment.grid.Location;
import org.junit.jupiter.api.Test;

class SimModelTest {

  private SimModelImpl model = new SimModelImpl(20, 20);

  @Test
  void testInitialSetting() {
    assertEquals(Config.getNumStations(20, 20), model.getStations().size());
  }

  @Test
  void testAddResponders() {
    Responder rescuer1 = model.addResponder("rescuer1", ResponderRole.RESCUER);
    Responder rescuer2 = model.addResponder("rescuer2", ResponderRole.RESCUER);
    Responder pilot1 = model.addResponder("pilot1", ResponderRole.PILOT);
    Responder pilot2 = model.addResponder("pilot2", ResponderRole.PILOT);
    assertEquals(model.getStations().get(0), rescuer1.station());
    assertEquals(
        model.getStations().get(1),
        rescuer2.station(),
        "Responders with the same role should be spread across the grid");
    assertEquals(model.getStations().get(0), pilot1.station());
    assertEquals(
        model.getStations().get(1),
        pilot2.station(),
        "Responders with the same role should be spread across the grid");
  }

  @Test
  void testEmergencySpawnAndResolution() {
    Emergency emergency = model.spawnEmergency().orElseThrow();
    assertFalse(model.getStations().contains(emergency.position()));
    assertEquals(EmergencyStatus.WAITING, emergency.status());
    assertEquals(1, model.getEmergencies().size());

    model.setEmergencyStatus(emergency.id(), EmergencyStatus.IN_PROGRESS);
    assertEquals(
        EmergencyStatus.IN_PROGRESS,
        model.getEmergencyAt(emergency.position()).orElseThrow().status());

    assertTrue(model.resolveEmergency(emergency.id()).isPresent());
    assertTrue(model.getEmergencies().isEmpty());
  }

  @Test
  void testMovingResponders() {
    model.addResponder("rescuer1", ResponderRole.RESCUER);
    Location rescuerStation = model.getResponder("rescuer1").orElseThrow().station();
    model.addResponder("pilot1", ResponderRole.PILOT);
    Location pilotStation = model.getResponder("pilot1").orElseThrow().station();
    int distRescuer, distPilot;
    Location target;
    do {
      target = model.getFreePosition().orElseThrow();
      distRescuer = Config.getManhattanDistance(rescuerStation, target);
      distPilot = Config.getManhattanDistance(pilotStation, target);
    } while (distRescuer < 1 && distPilot < 2);
    model.moveTowards("rescuer1", target);
    model.moveTowards("pilot1", target);
    assertEquals(
        distRescuer - 1,
        Config.getManhattanDistance(
            model.getResponder("rescuer1").orElseThrow().position(), target),
        "Rescuer should move one cell");
    assertEquals(
        distPilot - 2,
        Config.getManhattanDistance(model.getResponder("pilot1").orElseThrow().position(), target),
        "Pilot should move two cells");
  }

  @Test
  void testCellUpdating() {
    model.addResponder("rescuer1", ResponderRole.RESCUER);
    model.addResponder("pilot1", ResponderRole.PILOT);
    Location station = model.getStations().get(0);
    Location target = model.getFreePosition().orElseThrow();

    assertTrue(model.hasObject(SimModelImpl.STATION, station));
    assertTrue(
        model.hasObject(SimModelImpl.AGENT, station),
        "Station cell should have agent bit there are agents");

    model.moveTowards("rescuer1", target);
    assertTrue(
        model.hasObject(SimModelImpl.AGENT, station),
        "Station cell should keep agent bit since not all agents are gone");

    model.moveTowards("pilot1", target);
    assertFalse(
        model.hasObject(SimModelImpl.AGENT, station),
        "Station cell should not have agent bit since all agents are gone");
  }
}
