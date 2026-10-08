package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ModelGeneratorTest {

  private ModelGenerator generator;
  private final Dimension defaultGridSize = new Dimension(25, 25);
  private final int defaultStations = 4;
  private final int defaultRescuers = 8;
  private final int defaultPilots = 2;

  @BeforeEach
  void setUp() {
    generator = new ModelGenerator();
  }

  @Test
  @DisplayName("generateScenario should initialize TestModel with the specified parameters")
  void testGenerateGenericScenario() {
    TestModel model =
        generator.generateScenario(
            defaultGridSize, defaultStations, defaultRescuers, defaultPilots);

    assertNotNull(model, "Model should not be null");
    assertEquals(defaultGridSize, model.getSize(), "Grid size must match the configured value");
    assertEquals(defaultStations, model.getNStations(), "Number of stations must match");
    assertEquals(defaultRescuers, model.getNRescuers(), "Number of rescuers must match");
    assertEquals(defaultPilots, model.getNPilots(), "Number of pilots must match");
  }

  @Test
  @DisplayName("generateSmallScenario should configure the expected preset values")
  void testGenerateSmallScenario() {
    TestModel model = generator.generateSmallScenario();

    assertNotNull(model);
    assertEquals(new Dimension(15, 15), model.getSize());
    assertEquals(2, model.getNStations());
    assertEquals(4, model.getNRescuers());
    assertEquals(1, model.getNPilots());
  }

  @Test
  @DisplayName("generateMediumScenario should configure the expected preset values")
  void testGenerateMediumScenario() {
    TestModel model = generator.generateMediumScenario();

    assertNotNull(model);
    assertEquals(new Dimension(25, 25), model.getSize());
    assertEquals(4, model.getNStations());
    assertEquals(8, model.getNRescuers());
    assertEquals(2, model.getNPilots());
  }

  @Test
  @DisplayName("generateLargeScenario should configure the expected preset values")
  void testGenerateLargeScenario() {
    TestModel model = generator.generateLargeScenario();

    assertNotNull(model);
    assertEquals(new Dimension(50, 50), model.getSize());
    assertEquals(10, model.getNStations());
    assertEquals(15, model.getNRescuers());
    assertEquals(5, model.getNPilots());
  }

  @Test
  @DisplayName("generateScenario should reset emergencyCounter to 0")
  void testGenerateScenarioResetsCounter() {
    TestModel model =
        generator.generateScenario(
            defaultGridSize, defaultStations, defaultRescuers, defaultPilots);

    generator.generateEmergency(model);
    generator.generateEmergency(model);

    TestModel newModel =
        generator.generateScenario(
            defaultGridSize, defaultStations, defaultRescuers, defaultPilots);
    Optional<TestEmergency> nextEmergency = generator.generateEmergency(newModel);

    assertTrue(nextEmergency.isPresent(), "Emergency should be present");
    assertEquals(
        1,
        nextEmergency.get().getId(),
        "Emergency ID should restart from 1 after generateScenario");
  }

  @Test
  @DisplayName("generateEmergency should increment ID monotonically")
  void testGenerateEmergencyIdIncrement() {
    TestModel model =
        generator.generateScenario(
            defaultGridSize, defaultStations, defaultRescuers, defaultPilots);

    Optional<TestEmergency> first = generator.generateEmergency(model);
    Optional<TestEmergency> second = generator.generateEmergency(model);
    Optional<TestEmergency> third = generator.generateEmergency(model);

    assertTrue(first.isPresent());
    assertTrue(second.isPresent());
    assertTrue(third.isPresent());

    assertEquals(1, first.get().getId());
    assertEquals(2, second.get().getId());
    assertEquals(3, third.get().getId());
  }

  @RepeatedTest(
      value = 50,
      name = "Run {currentRepetition}/{totalRepetitions} - checking randomized bounds")
  @DisplayName(
      "generateEmergency should produce coordinates within bounds and severity between 1 and 5")
  void testGenerateEmergencyBoundsAndSeverity() {
    TestModel model =
        generator.generateScenario(
            defaultGridSize, defaultStations, defaultRescuers, defaultPilots);

    Optional<TestEmergency> emergencyOpt = generator.generateEmergency(model);

    assertTrue(emergencyOpt.isPresent(), "Generated emergency should be present");
    TestEmergency emergency = emergencyOpt.get();

    assertTrue(
        emergency.getX() >= 0 && emergency.getX() < defaultGridSize.width,
        "X coordinate must be within [0, gridSize - 1]");
    assertTrue(
        emergency.getY() >= 0 && emergency.getY() < defaultGridSize.height,
        "Y coordinate must be within [0, gridSize - 1]");

    assertTrue(
        emergency.getSeverity() >= 1 && emergency.getSeverity() <= 5,
        "Severity must be in the range [1, 5]");
  }
}
