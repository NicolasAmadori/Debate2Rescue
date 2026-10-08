package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import model.ModelGenerator;
import model.TestEmergency;
import model.TestModel;

public class EmergencyEnvironment extends Environment {
  private static final int SPEED = 300;
  private static final int SPAWN_CHANCE_PERCENTAGE = 90;
  private static final int MAX_EMERGENCIES = 20;

  private static final Logger logger = Logger.getLogger(EmergencyEnvironment.class.getName());

  private final Random random = new Random();
  private final ModelGenerator modelGenerator = new ModelGenerator();
  private Thread generatorThread;
  private TestModel testModel;

  @Override
  public void init(String[] args) {
    super.init(args);
    Dimension size = new Dimension(10, 10);

    testModel = modelGenerator.generateScenario(size);

    // TODO: Init real model
    // TODO: Init view

    generatorThread = new Thread(this::generateEmergencies, "emergency-generator");
    generatorThread.setDaemon(true);
    generatorThread.start();
    logger.info("EmergencyEnvironment initialized.");
  }

  @Override
  public void stop() {
    generatorThread.interrupt();
    super.stop();
  }

  /** Simulation loop: at every step a new emergency may be added to the model. */
  private void generateEmergencies() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        Thread.sleep(SPEED);
        if (random.nextInt(100) < SPAWN_CHANCE_PERCENTAGE
            && testModel.getEmergencies().size() < MAX_EMERGENCIES) {
          Optional<TestEmergency> emergency = modelGenerator.generateEmergency(testModel);
          if (emergency.isPresent()) {
            logger.info("Generated emergency: " + emergency);
            testModel.addEmergency("foo"); // TODO: Replace with actual emergency object
            informAgsEnvironmentChanged();
          }
        }
      }
    } catch (InterruptedException e) {
    }
  }

  @Override
  public boolean executeAction(String agName, Structure action) {
    logger.info(agName + " executing action: " + action);

    String actionName = action.getFunctor();

    if (actionName.equals("join")) {
      return addAgent(agName, action.getTerm(0).toString());
    }

    logger.warning("Unknown or unhandled action: " + action);
    return false;
  }

  /** Returns the current percepts for the specified agent. */
  @Override
  public Collection<Literal> getPercepts(String agName) {
    List<Literal> percepts = new ArrayList<>();
    // TODO: Populate percepts based on the agent role and thecurrent state of the environment.
    return percepts;
  }

  private boolean addAgent(String agentName, String role) {
    // TODO: Implement agent addition logic to the model
    return true;
  }
}
