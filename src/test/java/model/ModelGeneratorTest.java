package model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ModelGeneratorTest {

  private ModelGenerator generator;

  @BeforeEach
  void setUp() {
    generator = new ModelGenerator();
  }

  @Test
  @DisplayName("generateScenario should initialize TestModel with the specified grid size")
  void testGenerateScenarioGridSize() {
    int expectedGridSize = 25;
    TestModel model = generator.generateScenario(expectedGridSize);

    assertNotNull(model, "Model should not be null");
    assertEquals(
        expectedGridSize, model.getGridSize(), "Grid size must match the configured value");
  }

  @Test
  @DisplayName("generateScenario should reset emergencyCounter to 0")
  void testGenerateScenarioResetsCounter() {
    TestModel model = generator.generateScenario(10);

    generator.generateEmergency(model);
    generator.generateEmergency(model);

    TestModel newModel = generator.generateScenario(10);
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
    TestModel model = generator.generateScenario(10);

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
    int gridSize = 15;
    TestModel model = generator.generateScenario(gridSize);

    Optional<TestEmergency> emergencyOpt = generator.generateEmergency(model);

    assertTrue(emergencyOpt.isPresent(), "Generated emergency should be present");
    TestEmergency emergency = emergencyOpt.get();

    assertTrue(
        emergency.getX() >= 0 && emergency.getX() < gridSize,
        "X coordinate must be within [0, gridSize - 1]");
    assertTrue(
        emergency.getY() >= 0 && emergency.getY() < gridSize,
        "Y coordinate must be within [0, gridSize - 1]");

    assertTrue(
        emergency.getSeverity() >= 1 && emergency.getSeverity() <= 5,
        "Severity must be in the range [1, 5]");
  }
}
