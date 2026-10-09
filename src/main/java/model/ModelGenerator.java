package model;

import java.awt.Dimension;
import java.util.Optional;
import java.util.Random;

public class ModelGenerator {

  private final Random random = new Random();
  private int emergencyCounter = 0;

  public TestModel generateSmallScenario() {
    return this.generateScenario(new Dimension(15, 15), 2, 4, 1);
  }

  public TestModel generateMediumScenario() {
    return this.generateScenario(new Dimension(25, 25), 4, 8, 2);
  }

  public TestModel generateLargeScenario() {
    return this.generateScenario(new Dimension(50, 50), 10, 15, 5);
  }

  /**
   * Initializes and configures the scenario model for the simulation.
   *
   * @param size the dimensions of the grid
   * @param nRescuers the number of rescuer agents to include in the model
   * @param nPilots the number of pilot agents to include in the model
   * @return a newly configured instance of TestModel
   */
  public TestModel generateScenario(Dimension size, int nStations, int nRescuers, int nPilots) {
    this.emergencyCounter = 0;
    return new TestModel(size, nStations, nRescuers, nPilots);
  }

  /**
   * Generates a new random emergency at a non-occupied grid position.
   *
   * @param model the current state of the model used to check for cell collisions
   * @return an Optional containing the new Emergency if an empty cell was found, or
   *     Optional.empty() if MAX_ATTEMPTS was exceeded
   */
  public Optional<TestEmergency> generateEmergency(TestModel model) {
    // TODO: add check for non-occupied grid position
    Dimension size = model.getSize();
    int x = random.nextInt(size.width);
    int y = random.nextInt(size.height);
    int id = ++emergencyCounter;
    int severity = 1 + random.nextInt(5);
    return Optional.of(new TestEmergency(id, severity, x, y));
  }
}
