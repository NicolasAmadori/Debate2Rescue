package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import config.Config;
import javax.management.relation.Role;
import org.junit.jupiter.api.Test;

class SimModelTest {

  private SimModel model = new SimModelImpl(10, 10);

  @Test
  void testInitialSetting() {
    assertEquals(Math.round(10 * Config.STATION_FREQ), model.getStations().size());
  }

  @Test
  void testAddResponders() {
    Responder rescuer1 = model.addResponder("rescuer1", Role.RESCUER);
    Responder rescuer2 = model.addResponder("rescuer2", Role.RESCUER);
    Responder pilot1 = model.addResponder("pilot1", Role.PILOT);
    Responder pilot2 = model.addResponder("pilot2", Role.PILOT);
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
}
