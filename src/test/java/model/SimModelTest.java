package model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import config.Config;
import org.junit.jupiter.api.Test;

class SimModelTest {

  private SimModel model = new SimModelImpl(10, 10);

  @Test
  void testInitialSetting() {
    assertEquals(Math.round(10 * Config.STATION_FREQ), model.getStations().size());
  }
}
