package env;

import jason.asSyntax.Structure;
import jason.environment.Environment;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import model.ModelGenerator;
import model.TestEmergency;
import model.TestModel;

public class EmergencyEnvironment extends Environment {
  private static final int MIN_DELAY_MS = 3000;
  private static final int MAX_DELAY_MS = 8000;
  private static final int GRID_SIZE = 10;

  private static final Logger logger = Logger.getLogger(EmergencyEnvironment.class.getName());

  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final Random random = new Random();
  private final ModelGenerator modelGenerator = new ModelGenerator();
  private TestModel testModel;

  @Override
  public void init(String[] args) {
    super.init(args);

    testModel = modelGenerator.generateScenario(GRID_SIZE);

    // TODO: Init model
    // TODO: Init view

    scheduleNextEmergencyEvent();
    logger.info("EmergencyEnvironment initialized.");
  }

  private void scheduleNextEmergencyEvent() {
    long delayMs = MIN_DELAY_MS + random.nextInt(MAX_DELAY_MS - MIN_DELAY_MS);

    scheduler.schedule(
        () -> {
          try {
            TestEmergency emergency = modelGenerator.generateEmergency(testModel).orElse(null);
            if (emergency != null) {
              logger.info("Generated emergency: " + emergency);
              // TODO: Notify orchestrator about the new emergency
            }
          } catch (Exception e) {
            logger.severe("Error during emergency event generation: " + e.getMessage());
          } finally {
            scheduleNextEmergencyEvent();
          }
        },
        delayMs,
        TimeUnit.MILLISECONDS);
  }

  @Override
  public boolean executeAction(String agName, Structure action) {
    logger.info(agName + " executing action: " + action);

    String actionName = action.getFunctor();

    if (actionName.equals("foo")) {
      logger.info("Action recognized and executed.");
      return true;
    }

    logger.warning("Unknown or unhandled action: " + action);
    return false;
  }

  @Override
  public void stop() {
    scheduler.shutdownNow();
    super.stop();
  }
}
