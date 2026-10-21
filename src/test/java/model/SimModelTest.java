package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import config.Config;
import jason.environment.grid.Location;
import org.junit.jupiter.api.Test;

class SimModelTest {

  private SimModel model = new SimModel(20, 20, 2, 5);

  @Test
  void testInitialSetting() {
    assertEquals(2, model.getStations().size());
    assertEquals(5, model.getObstacles().size());
  }

  @Test
  void testAddResponders() {
    Responder rescuer1 = model.addRescuer("rescuer1");
    System.out.println(rescuer1);
    Responder rescuer2 = model.addRescuer("rescuer2");
    System.out.println(rescuer2);
    Responder pilot1 = model.addPilot("pilot1");
    System.out.println(pilot1);
    Responder pilot2 = model.addPilot("pilot2");
    System.out.println(pilot2);
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
    System.out.println(emergency);
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
    model.addRescuer("rescuer1");
    Location rescuerStation = model.getResponder("rescuer1").orElseThrow().station();
    model.addPilot("pilot1");
    Location pilotStation = model.getResponder("pilot1").orElseThrow().station();
    int distRescuer, distPilot;
    Location target;
    do {
      target = model.getFreePosition().orElseThrow();
      distRescuer = Config.getManhattanDistance(rescuerStation, target);
      distPilot = Config.getManhattanDistance(pilotStation, target);
    } while (distRescuer < 1 || distPilot < 2);
    model.moveTowards("rescuer1", target);
    model.moveTowards("pilot1", target);
    Location newRescuerPos = model.getResponder("rescuer1").orElseThrow().position();
    Location newPilotPos = model.getResponder("pilot1").orElseThrow().position();
    assertTrue(
        Config.getManhattanDistance(newRescuerPos, target) < distRescuer,
        "Rescuer should move closer to target");
    assertTrue(
        Config.getManhattanDistance(newPilotPos, target) < distPilot,
        "Pilot should move closer to target");
  }

  @Test
  void testCellUpdating() {
    model.addRescuer("rescuer1");
    model.addPilot("pilot1");
    Location station = model.getStations().get(0);
    Location target = model.getFreePosition().orElseThrow();

    assertTrue(model.hasObject(SimModel.STATION, station));
    assertTrue(
        model.hasObject(SimModel.AGENT, station),
        "Station cell should have agent bit there are agents");

    model.moveTowards("rescuer1", target);
    assertTrue(
        model.hasObject(SimModel.AGENT, station),
        "Station cell should keep agent bit since not all agents are gone");

    model.moveTowards("pilot1", target);
    assertFalse(
        model.hasObject(SimModel.AGENT, station),
        "Station cell should not have agent bit since all agents are gone");
  }
}
