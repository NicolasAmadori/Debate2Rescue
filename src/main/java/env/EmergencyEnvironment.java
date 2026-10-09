package env;

import config.ConfigValues;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.environment.Environment;
import jason.environment.grid.Location;
import jason.runtime.RuntimeServices;
import jason.runtime.RuntimeServicesFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import model.Emergency;
import model.Responder;
import model.ResponderRole;
import model.SimModel;
import view.SimConfigDialog;
import view.View;
import view.ViewImpl;

public class EmergencyEnvironment extends Environment {
  private static final Logger LOGGER = Logger.getLogger(EmergencyEnvironment.class.getName());

  // Agent roles
  private static final String ORCHESTRATOR = "orchestrator";
  private static final String RESCUER = "rescuer";
  private static final String PILOT = "pilot";

  // Emergency generation
  private static final int ACTION_THREADS = 20;
  private static final int SPAWN_CHANCE_PERCENTAGE = 90;
  private static final int MAX_EMERGENCIES = 20;

  private SimModel model;
  private View view;
  private ConfigValues config;
  private final Random random = new Random();
  private Thread generatorThread;
  private int simulationSpeed = 300; // Managed from the view

  public EmergencyEnvironment() {
    // Increased the number of action threads for better concurrency handling
    super(ACTION_THREADS);
  }

  @Override
  public void init(String[] args) {
    super.init(args);
    config = getConfigValues(args);

    model = new SimModel(config.gridWidth(), config.gridHeight(), config.stationsCount());
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

  @Override
  public void stop() {
    if (generatorThread != null) {
      generatorThread.interrupt();
    }
    super.stop();
  }

  private ConfigValues getConfigValues(String[] args) {
    ConfigValues configValues;
    if (args.length == 5) {
      configValues =
          new ConfigValues(
              Integer.parseInt(args[0]),
              Integer.parseInt(args[1]),
              Integer.parseInt(args[2]),
              Integer.parseInt(args[3]),
              Integer.parseInt(args[4]));
    } else {
      Optional<ConfigValues> chosen = SimConfigDialog.ask();
      if (chosen.isEmpty()) {
        System.exit(0);
      }
      configValues = chosen.get();
    }
    return configValues;
  }

  /** Spawns the initial configured batch of dynamic rescuers and pilots. */
  private void spawnInitialAgents() {
    try {
      // Wait for the MAS environment to complete initialization
      Thread.sleep(150);
      spawnAgents(PILOT, config.pilotsCount());
      spawnAgents(RESCUER, config.rescuersCount());
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
      return switch (action.getFunctor()) {
        case "join" -> addAgent(agName, action.getTerm(0).toString());
        case "move_towards" -> {
          yield moveRescuer(agName, new Location(intArg(action, 0), intArg(action, 1)));
        }
        case "start_rescue" -> true; // TODO: Implement the logic for starting a rescue operation
        default -> {
          LOGGER.warning("Unknown or unhandled action: " + action);
          yield false;
        }
      };
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    } catch (Exception e) {
      LOGGER.warning(agName + " failed " + action + ": " + e);
      return false;
    }
  }

  /** Returns the current percepts for the specified agent. */
  @Override
  public Collection<Literal> getPercepts(String agName) {
    if (agName.equals(ORCHESTRATOR)) {
      return buildOrchestratorPercepts(agName);
    }

    return buildResponderPercepts(agName);
  }

  private List<Literal> buildOrchestratorPercepts(String agName) {
    List<Literal> percepts = new ArrayList<>();
    for (Emergency e : model.getEmergencies()) {
      // TODO: add emergency percepts
    }
    for (Responder r : model.getResponders()) {
      // TODO: add emergency percepts
    }
    return percepts;
  }

  private List<Literal> buildResponderPercepts(String agName) {
    List<Literal> percepts = new ArrayList<>();
    model
        .getResponder(agName)
        .ifPresent(
            r -> {
              percepts.add(literal("at", r.position().x, r.position().y));
              percepts.add(literal("station", r.station().x, r.station().y));
              model
                  .getEmergencyAt(r.position())
                  .ifPresent(
                      e ->
                          percepts.add(
                              literal(
                                  "emergency_info",
                                  e.id(),
                                  e.type().name().toLowerCase(),
                                  e.severity().name().toLowerCase(),
                                  e.victims())));
            });
    return percepts;
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
   *
   * @param agName the name of the agent to be moved
   * @param target the target location to move towards
   * @return true if the agent was successfully moved, false otherwise
   * @throws InterruptedException if the thread is interrupted while moving the agent
   */
  private boolean moveRescuer(String agName, Location target) throws InterruptedException {
    Thread.sleep(simulationSpeed);
    model.moveTowards(agName, target);
    return true;
  }

  // Utility methods

  private void log(String message) {
    LOGGER.info(message);
    view.log(message);
  }

  private static int intArg(Structure action, int index) throws Exception {
    return (int) ((NumberTerm) action.getTerm(index)).solve();
  }

  /**
   * Creates a literal from atoms, numbers, and terms. For example, literal("at", 3, 4) will create
   * at(3,4).
   *
   * @param functor the name of the literal
   * @param args the arguments of the literal, which can be integers, terms, or atoms
   * @return the created literal
   */
  private static Literal literal(String functor, Object... args) {
    Literal literal = ASSyntax.createLiteral(functor);
    for (Object arg : args) {
      if (arg instanceof Integer n) {
        literal.addTerm(ASSyntax.createNumber(n));
      } else if (arg instanceof Term t) {
        literal.addTerm(t);
      } else {
        literal.addTerm(ASSyntax.createAtom(arg.toString()));
      }
    }
    return literal;
  }
}
