package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import model.ModelGenerator;
import model.TestEmergency;
import model.TestModel;
import view.View;
import view.ViewImpl;

public class EmergencyEnvironment extends Environment {
  private static final int SIMULATION_SPEED = 300;
  private static final int SPAWN_CHANCE_PERCENTAGE = 90;
  private static final int MAX_EMERGENCIES = 20;

  // Agent roles
  private static final String RESCUER = "rescuer";

  private static final Logger logger = Logger.getLogger(EmergencyEnvironment.class.getName());

  private final Random random = new Random();
  private final ModelGenerator modelGenerator = new ModelGenerator();
  private Thread generatorThread;
  private TestModel testModel;
  private View view;

  @Override
  public void init(String[] args) {
    super.init(args);

    testModel = modelGenerator.generateSmallScenario();

    // TODO: Init real model
    // TODO: Init view
    view = new ViewImpl(testModel);

    // TODO: remove, just to see something on the grid
    testModel.add(TestModel.STATION, 1, 1);
    testModel.setAgPos(0, 3, 3);
    testModel.setAgPos(1, 6, 2);

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
            new Point2D.Double(
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

  private boolean moveRescuer(String agName, Point2D target) {
    // TODO: Implement agent moving logic to the model
    logger.info("Moved rescuer: " + agName + " towards target: " + target);
    return true;
  }
}
