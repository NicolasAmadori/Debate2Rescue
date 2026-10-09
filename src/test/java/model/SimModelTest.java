package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import config.Config;
import jason.environment.grid.Location;
import org.junit.jupiter.api.Test;

class SimModelTest {

  private SimModel model = new SimModelImpl(20, 20);

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
    model.addResponder("pilot1", ResponderRole.PILOT);
    Location target = new Location(5, 5);
    model.moveTowards("rescuer1", target);
    model.moveTowards("pilot1", target);
    assertEquals(
        new Location(2, 1),
        model.getResponder("rescuer1").orElseThrow().position(),
        "Rescuer should move one cell");
    assertEquals(
        new Location(3, 1),
        model.getResponder("pilot1").orElseThrow().position(),
        "Pilot should move two cells");
  }
}
