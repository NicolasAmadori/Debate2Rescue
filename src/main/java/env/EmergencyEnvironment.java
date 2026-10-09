package env;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.Environment;
import jason.runtime.RuntimeServices;
import jason.runtime.RuntimeServicesFactory;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import model.Responder;
import model.ResponderRole;
import model.SimModel;
import view.GridSizeDialog;
import view.View;
import view.ViewImpl;

public class EmergencyEnvironment extends Environment {
  private static final Logger LOGGER = Logger.getLogger(EmergencyEnvironment.class.getName());

  // Rescuer agent
  private static final int N_RESCUER = 4;
  private static final String RESCUER = "rescuer";

  // Pilot agent
  private static final int N_PILOT = 4;
  private static final String PILOT = "pilot";

  // Emergency generation
  private static final int ACTION_THREADS = 20;
  private static final int SPAWN_CHANCE_PERCENTAGE = 90;
  private static final int MAX_EMERGENCIES = 20;

  private SimModel model;
  private View view;
  private final Random random = new Random();
  private Thread generatorThread;
  private int simulationSpeed = 300; // Managed by the view

  public EmergencyEnvironment() {
    // Increased the number of action threads for better concurrency handling
    super(ACTION_THREADS);
  }

  @Override
  public void init(String[] args) {
    super.init(args);
    Dimension gridSize = getGridSize(args);

    model = new SimModel(gridSize.width, gridSize.height, 7);
    view = new ViewImpl(model);
    view.setOnSpeedChange(speed -> simulationSpeed = speed);

    // Spawning dynamic rescuers asynchronously
    new Thread(this::spawnInitialAgents, "agent-spawner").start();

    // Starting the emergency generator thread
    generatorThread = new Thread(this::generateEmergencies, "emergency-generator");
    generatorThread.setDaemon(true);
    generatorThread.start();

    log("EmergencyEnvironment initialized.");
  }

  private Dimension getGridSize(String[] args) {
    Dimension size;
    if (args.length == 2) {
      size = new Dimension(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
    } else {
      Optional<Dimension> chosen = GridSizeDialog.ask();
      if (chosen.isEmpty()) {
        System.exit(0);
      }
      size = chosen.get();
    }
    return size;
  }

  /** Spawns the initial configured batch of dynamic rescuers and pilots. */
  private void spawnInitialAgents() {
    try {
      // Wait for the MAS environment to complete initialization
      Thread.sleep(150);
      spawnAgents(PILOT, N_PILOT);
      spawnAgents(RESCUER, N_RESCUER);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warning("Agent spawning thread was interrupted.");
    }
  }

  /**
   * Dynamically creates and registers a batch of new agents with unique names.
   *
   * @param role The role identifier
   * @param count Number of agents to spawn
   * @return The list of created agent names
   */
  public synchronized List<String> spawnAgents(String role, int count) {
    role = role.toLowerCase();
    List<String> spawned = new ArrayList<>();
    RuntimeServices services = RuntimeServicesFactory.get();
    String aslPath = "src/main/agents/" + role + ".asl";
    for (int i = 0; i < count; i++) {
      String agName = String.format("%s_%d", role, i + 1);
      try {
        String createdAgName = services.createAgent(agName, aslPath, null, null, null, null, null);
        services.startAgent(createdAgName);
        spawned.add(createdAgName);
      } catch (Exception e) {
        LOGGER.severe(
            "Failed to spawn dynamic agent " + agName + " (" + role + "): " + e.getMessage());
      }
    }

    return spawned;
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
        Thread.sleep(simulationSpeed);
        if (random.nextInt(100) < SPAWN_CHANCE_PERCENTAGE
            && model.getEmergencies().size() < MAX_EMERGENCIES) {
          model
              .spawnEmergency()
              .ifPresent(
                  e -> {
                    log("Generated: " + e);
                    informAgsEnvironmentChanged();
                  });
          ;
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public boolean executeAction(String agName, Structure action) {
    log(agName + " executing action: " + action);

    try {
      String actionName = action.getFunctor();

      if (actionName.equals("join")) {
        return addAgent(agName, action.getTerm(0).toString());
      }

      if (actionName.equals("move_towards")) {
        Thread.sleep(simulationSpeed);
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

    LOGGER.warning("Unknown or unhandled action: " + action);
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

  private void log(String message) {
    LOGGER.info(message);
    view.log(message);
  }

  // Internal actions

  /**
   * AgentSpeak Internal action: join Adds an agent to the environment with the specified role.
   *
   * @param agName the name of the agent to be added
   * @param role the role of the agent to be added
   * @return true if the agent was successfully added, false otherwise
   */
  private boolean addAgent(String agName, String role) {
    Responder responder = model.addResponder(agName, ResponderRole.valueOf(role.toUpperCase()));
    log(
        responder.name()
            + " joined at station ("
            + responder.station().x
            + ","
            + responder.station().y
            + ")");
    return true;
  }

  /**
   * AgentSpeak Internal action: move_towards Moves a rescuer agent towards the specified target
   * point.
   *
   * @param agName the name of the agent to be moved
   * @param target the target point to move towards
   * @return true if the agent was successfully moved, false otherwise
   */
  private boolean moveRescuer(String agName, Point target) {
    // TODO: Implement agent moving logic to the model
    log("Moved rescuer: " + agName + " towards target: " + target);
    return true;
  }
}
