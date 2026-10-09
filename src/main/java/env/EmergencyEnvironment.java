package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import jason.runtime.RuntimeServices;
import jason.runtime.RuntimeServicesFactory;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import model.ModelGenerator;
import model.TestEmergency;
import model.TestModel;

public class EmergencyEnvironment extends Environment {
  private static final int SIMULATION_SPEED = 300;
  private static final int SPAWN_CHANCE_PERCENTAGE = 90;
  private static final int MAX_EMERGENCIES = 20;
  private static final int N_RESCUER = 4;

  // Agent roles
  private static final String RESCUER = "rescuer";
  private static final String RESCUER_ASL_PATH = "src/main/agents/rescuer.asl";

  private static final Logger logger = Logger.getLogger(EmergencyEnvironment.class.getName());

  private final Random random = new Random();
  private final ModelGenerator modelGenerator = new ModelGenerator();
  private final AtomicInteger rescuerIdCounter = new AtomicInteger(1);

  private Thread generatorThread;
  private TestModel testModel;

  @Override
  public void init(String[] args) {
    super.init(args);

    testModel = modelGenerator.generateSmallScenario();

    // TODO: Init real model
    // TODO: Init view

    // Spawning dynamic rescuers asynchronously
    new Thread(this::spawnInitialRescuers, "agent-spawner").start();

    generatorThread = new Thread(this::generateEmergencies, "emergency-generator");
    generatorThread.setDaemon(true);
    generatorThread.start();

    logger.info("EmergencyEnvironment initialized.");
  }

  /** Spawns the initial configured batch of dynamic rescuers. */
  private void spawnInitialRescuers() {
    try {
      // Wait for the MAS environment to complete initialization
      Thread.sleep(150);

      for (int i = 0; i < N_RESCUER; i++) {
        spawnRescuer();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warning("Rescuer spawning thread was interrupted.");
    }
  }

  /**
   * Dynamically creates and registers a new rescuer agent with a unique name. Can be called
   * whenever an emergency requires extra units.
   *
   * @return The unique name of the spawned rescuer, or null on failure.
   */
  public synchronized String spawnRescuer() {
    String agName = String.format("%s_%d", RESCUER, rescuerIdCounter.getAndIncrement());
    try {
      RuntimeServices services = RuntimeServicesFactory.get();

      String createdAgName =
          services.createAgent(agName, RESCUER_ASL_PATH, null, null, null, null, null);
      services.startAgent(createdAgName);

      return createdAgName;
    } catch (Exception e) {
      logger.severe("Failed to spawn dynamic agent " + agName + ": " + e.getMessage());
      return null;
    }
  }

  @Override
  public void stop() {
    if (generatorThread != null) {
      generatorThread.interrupt();
    }
    super.stop();
  }

  /** Simulation loop: at every step a new emergency may be added to the model. */
  private void generateEmergencies() {
    try {
      while (!Thread.currentThread().isInterrupted()) {
        Thread.sleep(SIMULATION_SPEED);
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
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public boolean executeAction(String agName, Structure action) {
    logger.info(agName + " executing action: " + action);

    try {
      String actionName = action.getFunctor();

      if (actionName.equals("join")) {
        return addAgent(agName, action.getTerm(0).toString());
      }

      if (actionName.equals("move_towards")) {
        Thread.sleep(SIMULATION_SPEED);
        return moveRescuer(
            agName,
            new Point(
                Integer.parseInt(action.getTerm(0).toString()),
                Integer.parseInt(action.getTerm(1).toString())));
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }

    logger.warning("Unknown or unhandled action: " + action);
    return false;
  }

  /** Returns the current percepts for the specified agent. */
  @Override
  public Collection<Literal> getPercepts(String agName) {
    List<Literal> percepts = new ArrayList<>();
    // TODO: Populate percepts based on the agent role and thecurrent state of the environment.
    if (agName.equals(RESCUER)) {}
    return percepts;
  }

  // Internal actions

  private boolean addAgent(String agName, String role) {
    // TODO: Implement agent addition logic to the model
    logger.info("Added agent: " + agName + " with role: " + role);
    return true;
  }

  private boolean moveRescuer(String agName, Point target) {
    // TODO: Implement agent moving logic to the model
    logger.info("Moved rescuer: " + agName + " towards target: " + target);
    return true;
  }
}
