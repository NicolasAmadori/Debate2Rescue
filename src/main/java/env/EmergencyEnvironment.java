package env;

import config.Config;
import config.ConfigValues;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.StringTerm;
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
import model.EmergencyStatus;
import model.Pilot;
import model.Rescuer;
import model.Responder;
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

  // Percepts
  private static final String PERCEPT_RESCUER = "rescuer";
  private static final String PERCEPT_PILOT = "pilot";
  private static final String PERCEPT_EMERGENCY = "emergency";
  private static final String PERCEPT_AT = "at";
  private static final String PERCEPT_STATION = "station";
  private static final String PERCEPT_EMERGENCY_INFO = "emergency_info";

  // Emergency generation
  private static final int ACTION_THREADS = 20;
  private static final int SPAWN_CHANCE_PERCENTAGE = 30;

  private SimModel model;
  private View view;
  private ConfigValues config;
  private int maxEmergencies;
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
    maxEmergencies = Config.getMaxNumEmergencies(config.gridWidth(), config.gridHeight());

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
      Thread.sleep(200);
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
            && model.getEmergencies().size() < maxEmergencies) {
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
    try {
      return switch (action.getFunctor()) {
        case "join_as_rescuer" -> joinAsRescuer(agName);
        case "join_as_pilot" -> joinAsPilot(agName);
        case "move_towards" ->
            moveTowards(agName, new Location(intArg(action, 0), intArg(action, 1)));
        case "assign" -> assign(action.getTerm(0).toString(), intArg(action, 1));
        case "start_rescue" -> startRescue(agName, intArg(action, 0));
        case "wait_steps" -> waitSteps(intArg(action, 0));
        case "resolve_emergency" -> resolveEmergency(agName, intArg(action, 0));
        case "log" -> logAgentMessage(agName, action.getTerms());
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
    for (Rescuer r : model.getRescuers()) {
      percepts.add(literal(PERCEPT_RESCUER, r.name(), r.position().x, r.position().y));
    }
    for (Pilot p : model.getPilots()) {
      percepts.add(
          literal(
              PERCEPT_PILOT,
              p.name(),
              p.resource().name().toLowerCase(),
              p.position().x,
              p.position().y));
    }
    for (Emergency e : model.getEmergencies()) {
      percepts.add(literal(PERCEPT_EMERGENCY, e.id(), e.position().x, e.position().y));
    }
    return percepts;
  }

  private List<Literal> buildResponderPercepts(String agName) {
    List<Literal> percepts = new ArrayList<>();
    model
        .getResponder(agName)
        .ifPresent(
            r -> {
              percepts.add(literal(PERCEPT_AT, r.position().x, r.position().y));
              percepts.add(literal(PERCEPT_STATION, r.station().x, r.station().y));
              model
                  .getEmergencyAt(r.position())
                  .ifPresent(
                      e ->
                          percepts.add(
                              literal(
                                  PERCEPT_EMERGENCY_INFO,
                                  e.id(),
                                  e.type().name().toLowerCase(),
                                  e.severity().esi,
                                  e.victims())));
            });
    return percepts;
  }

  // Internal actions

  private boolean join(Responder r) {
    log(r.name() + " joined at station (" + r.station().x + "," + r.station().y + ")");
    return true;
  }

  /**
   * AgentSpeak Internal action: join_as_rescuer. Makes the agent join as a rescuer.
   *
   * @param agName the name of the agent joining as a rescuer
   * @return true if the agent successfully joined as a rescuer, false otherwise
   */
  private boolean joinAsRescuer(String agName) {
    return join(model.addRescuer(agName));
  }

  /**
   * AgentSpeak Internal action: join_as_pilot. Makes the agent join as a pilot.
   *
   * @param agName the name of the agent joining as a pilot
   * @return true if the agent successfully joined as a pilot, false otherwise
   */
  private boolean joinAsPilot(String agName) {
    return join(model.addPilot(agName));
  }

  /**
   * AgentSpeak Internal action: move_towards. Moves a rescuer agent towards the specified target
   *
   * @param agName the name of the agent to be moved
   * @param target the target location to move towards
   * @return true if the agent was successfully moved, false otherwise
   * @throws InterruptedException if the thread is interrupted while moving the agent
   */
  private boolean moveTowards(String agName, Location target) throws InterruptedException {
    Thread.sleep(simulationSpeed);
    model.moveTowards(agName, target);
    return true;
  }

  /**
   * AgentSpeak Internal action: assign. Assigns a rescuer to the specified emergency.
   *
   * @param rescuerId the ID of the rescuer being assigned to the emergency
   * @param emergencyId the ID of the emergency to be assigned
   * @return true if the assignment was successful, false otherwise
   */
  private boolean assign(String rescuerId, int emergencyId) {
    Optional<Emergency> emergency = model.setEmergencyStatus(emergencyId, EmergencyStatus.ASSIGNED);
    emergency.ifPresent(e -> log("Emergency " + emergencyId + " assigned to rescuer " + rescuerId));
    return emergency.isPresent();
  }

  /**
   * AgentSpeak Internal action: start_rescue. Starts the rescue operation for the specified
   * emergency by the given agent.
   *
   * @param agName the name of the agent performing the rescue
   * @param emergencyId the ID of the emergency to be rescued
   * @return true if the rescue operation was successfully started, false otherwise
   */
  private boolean startRescue(String agName, int emergencyId) {
    Optional<Emergency> emergency =
        model.setEmergencyStatus(emergencyId, EmergencyStatus.IN_PROGRESS);
    emergency.ifPresent(e -> log(agName + " reached emergency " + emergencyId + ": " + e));
    return emergency.isPresent();
  }

  /**
   * AgentSpeak Internal action: wait_steps. Makes the agent wait for the specified number of
   * simulation steps.
   *
   * @param steps the number of simulation steps to wait
   * @return true after waiting
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  private boolean waitSteps(int steps) throws InterruptedException {
    for (int i = 0; i < steps; i++) {
      Thread.sleep(simulationSpeed);
    }
    return true;
  }

  /**
   * AgentSpeak Internal action: log. Logs a message from the specified agent to both the console
   * and the view, concatenating the provided terms.
   *
   * @param agName the name of the agent logging the message
   * @param terms the terms to be concatenated and logged as the message
   * @return true after logging the message
   */
  private boolean logAgentMessage(String agName, List<Term> terms) {
    log(agName + ": " + concatTerms(terms));
    return true;
  }

  /**
   * AgentSpeak Internal action: resolve_emergency. Resolves the specified emergency by the given
   * agent.
   *
   * @param agName the name of the agent resolving the emergency
   * @param id the ID of the emergency to be resolved
   * @return true if the emergency was successfully resolved, false otherwise
   */
  private boolean resolveEmergency(String agName, int id) {
    Optional<Emergency> emergency = model.resolveEmergency(id);
    emergency.ifPresent(
        e -> {
          log("Emergency " + id + " resolved by: " + agName);
          informAgsEnvironmentChanged();
        });
    return emergency.isPresent();
  }

  // Utility methods

  private void log(String message) {
    LOGGER.info(message);
    view.log(message);
  }

  private static int intArg(Structure action, int index) throws Exception {
    return (int) ((NumberTerm) action.getTerm(index)).solve();
  }

  private static String concatTerms(List<Term> terms) {
    StringBuilder text = new StringBuilder();
    for (Term term : terms) {
      text.append(term instanceof StringTerm s ? s.getString() : term.toString());
    }
    return text.toString();
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
