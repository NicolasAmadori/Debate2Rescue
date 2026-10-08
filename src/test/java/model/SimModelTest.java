package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import config.Config;
import org.junit.jupiter.api.Test;

class SimModelTest {

  private SimModel model = new SimModelImpl(10, 10);

  @Test
  void testInitialSetting() {
    assertEquals(Math.round(10 * Config.STATION_FREQ), model.getStations().size());
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
