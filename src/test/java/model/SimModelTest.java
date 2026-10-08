package model;

import static config.Config.STATION_FREQ;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SimModelTest {

  private SimModel model = new SimModelImpl(10, 10);

  @Test
  void testInitialSetting() {
    assertEquals(Math.round(10 * STATION_FREQ), model.getStations().count());
  }
}
