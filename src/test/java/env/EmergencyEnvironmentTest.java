package env;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.environment.grid.Location;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.function.IntConsumer;
import model.Emergency;
import model.EmergencyStatus;
import model.SimModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import view.View;

class EmergencyEnvironmentTest {

  private static final int SIMULATION_SPEED = 0;
  private static final int GRID_WIDTH = 10;
  private static final int GRID_HEIGHT = 10;
  private static final int INITIAL_STATIONS = 2;

  private EmergencyEnvironment env;
  private SimModel model;

  @BeforeEach
  void setUp() throws Exception {
    env =
        new EmergencyEnvironment() {
          @Override
          public void init(String[] args) {}
        };
    model = new SimModel(GRID_WIDTH, GRID_HEIGHT, INITIAL_STATIONS);

    View dummyView =
        new View() {
          @Override
          public void log(String message) {
            // Empty implementation for testing purposes
          }

          @Override
          public void setOnSpeedChange(IntConsumer listener) {
            // Empty implementation for testing purposes
          }

          @Override
          public void update() {
            // Empty implementation for testing purposes
          }
        };

    // Injecting the model and view into the environment using reflection
    setPrivateField(env, "model", model);
    setPrivateField(env, "view", dummyView);
    setPrivateField(env, "simulationSpeed", SIMULATION_SPEED);
  }

  @AfterEach
  void tearDown() {
    if (env != null) {
      env.stop();
    }
  }

  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
    Field field = EmergencyEnvironment.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  @Test
  void testJoinAsRescuerAddsPercepts() {
    Structure joinAction = (Structure) Literal.parseLiteral("join_as_rescuer");
    assertTrue(
        env.executeAction("rescuer_1", joinAction), "Action 'join_as_rescuer' should return true");

    Location station = model.getResponder("rescuer_1").orElseThrow().station();

    // The rescuer should have 'at' and 'station' percepts after joining
    Collection<Literal> responderPercepts = env.getPercepts("rescuer_1");
    assertNotNull(responderPercepts, "getPercepts should not return null");

    Literal expectedAt = Literal.parseLiteral(String.format("at(%d,%d)", station.x, station.y));
    Literal expectedStation =
        Literal.parseLiteral(String.format("station(%d,%d)", station.x, station.y));

    assertTrue(
        responderPercepts.stream().anyMatch(p -> p.equals(expectedAt)),
        "getPercepts should include 'at' percept after joining");
    assertTrue(
        responderPercepts.stream().anyMatch(p -> p.equals(expectedStation)),
        "getPercepts should include 'station' percept after joining");

    // The orchestrator should have a percept for the registered rescuer
    Collection<Literal> orchestratorPercepts = env.getPercepts("orchestrator");
    Literal expectedRescuer =
        Literal.parseLiteral(String.format("rescuer(rescuer_1,%d,%d)", station.x, station.y));
    assertTrue(
        orchestratorPercepts.stream().anyMatch(p -> p.equals(expectedRescuer)),
        "Orchestrator percepts should contain the registered rescuer");
  }

  @Test
  void testJoinAsPilotAddsPercepts() {
    Structure joinAction = (Structure) Literal.parseLiteral("join_as_pilot");
    assertTrue(
        env.executeAction("pilot_1", joinAction), "Action 'join_as_pilot' should return true");

    Location station = model.getResponder("pilot_1").orElseThrow().station();

    // Verify the pilot's percepts
    Collection<Literal> responderPercepts = env.getPercepts("pilot_1");
    assertNotNull(responderPercepts, "getPercepts should not return null");

    Literal expectedAt = Literal.parseLiteral(String.format("at(%d,%d)", station.x, station.y));
    assertTrue(
        responderPercepts.stream().anyMatch(p -> p.equals(expectedAt)),
        "getPercepts should include 'at' percept after joining");

    // Verify the orchestrator's percepts
    Collection<Literal> orchestratorPercepts = env.getPercepts("orchestrator");
    Literal expectedPilot =
        Literal.parseLiteral(
            String.format("pilot(pilot_1,helicopter,%d,%d)", station.x, station.y));
    assertTrue(
        orchestratorPercepts.stream().anyMatch(p -> p.equals(expectedPilot)),
        "Orchestrator percepts should contain the registered pilot with resource");
  }

  @Test
  void testExecuteActionMoveTowards() {
    env.executeAction("rescuer_1", (Structure) Literal.parseLiteral("join_as_rescuer"));
    Location startPos = model.getResponder("rescuer_1").orElseThrow().position();

    int targetX = (startPos.x + 1 < 10) ? startPos.x + 1 : startPos.x - 1;
    int targetY = startPos.y;

    Structure moveAction =
        (Structure) Literal.parseLiteral(String.format("move_towards(%d,%d)", targetX, targetY));
    assertTrue(
        env.executeAction("rescuer_1", moveAction), "Action 'move_towards' should return true");

    Collection<Literal> percepts = env.getPercepts("rescuer_1");
    Literal expectedAt = Literal.parseLiteral(String.format("at(%d,%d)", targetX, targetY));
    assertTrue(
        percepts.stream().anyMatch(p -> p.equals(expectedAt)),
        "Agent position percept 'at' should be updated after moving");

    Structure invalidMove = (Structure) Literal.parseLiteral("move_towards(invalid, 1)");
    assertFalse(
        env.executeAction("rescuer_1", invalidMove),
        "Action 'move_towards' with invalid coords should return false");
  }

  @Test
  void testEmergencyPerceptsAndLifecycleActions() {
    env.executeAction("rescuer_1", (Structure) Literal.parseLiteral("join_as_rescuer"));
    Emergency emergency = model.spawnEmergency().orElseThrow();
    int id = emergency.id();
    Location ePos = emergency.position();

    // Verify that the orchestrator perceives the emergency
    Collection<Literal> orchestratorPercepts = env.getPercepts("orchestrator");
    Literal expectedEmergency =
        Literal.parseLiteral(String.format("emergency(%d,%d,%d)", id, ePos.x, ePos.y));
    assertTrue(
        orchestratorPercepts.stream().anyMatch(p -> p.equals(expectedEmergency)),
        "Orchestrator should perceive spawned emergency");

    // Move the rescuer to the emergency cell to verify 'emergency_info'
    Structure moveAction =
        (Structure) Literal.parseLiteral(String.format("move_towards(%d,%d)", ePos.x, ePos.y));
    while (!model.getResponder("rescuer_1").orElseThrow().position().equals(ePos)) {
      env.executeAction("rescuer_1", moveAction);
    }

    Collection<Literal> rescuerPercepts = env.getPercepts("rescuer_1");
    assertTrue(
        rescuerPercepts.stream().anyMatch(p -> p.getFunctor().equals("emergency_info")),
        "Rescuer at emergency location should perceive 'emergency_info'");

    // The percept must expose the emergency type so the rescuer can pick the resource
    Literal expectedInfo =
        Literal.parseLiteral(
            String.format(
                "emergency_info(%d,%s,%d,%d,%b,%b)",
                id,
                emergency.type().name().toLowerCase(),
                emergency.severity().esi,
                emergency.victims(),
                emergency.trapped(),
                emergency.escalating()));
    assertTrue(
        rescuerPercepts.stream().anyMatch(p -> p.equals(expectedInfo)),
        "Rescuer should perceive the emergency type name");

    // Test start_rescue
    Structure startRescueValid = (Structure) Literal.parseLiteral("start_rescue(" + id + ")");
    Structure startRescueInvalid = (Structure) Literal.parseLiteral("start_rescue(999)");

    assertFalse(
        env.executeAction("rescuer_1", startRescueInvalid),
        "start_rescue should fail for non-existent emergency ID");
    assertTrue(
        env.executeAction("rescuer_1", startRescueValid),
        "start_rescue should succeed for valid emergency ID");
    assertEquals(
        EmergencyStatus.IN_PROGRESS,
        model.getEmergencies().get(0).status(),
        "Emergency status should be IN_PROGRESS");

    // Test resolve_emergency
    Structure resolveValid = (Structure) Literal.parseLiteral("resolve_emergency(" + id + ")");
    Structure resolveInvalid = (Structure) Literal.parseLiteral("resolve_emergency(999)");

    assertFalse(
        env.executeAction("rescuer_1", resolveInvalid),
        "resolve_emergency should fail for non-existent emergency ID");
    assertTrue(
        env.executeAction("rescuer_1", resolveValid),
        "resolve_emergency should succeed for valid emergency ID");
    assertTrue(
        model.getEmergencies().isEmpty(), "Emergencies list should be empty after resolution");
  }

  @Test
  void testExecuteActionWaitSteps() {
    Structure waitAction = (Structure) Literal.parseLiteral("wait_steps(3)");
    assertTrue(
        env.executeAction("rescuer_1", waitAction), "Action 'wait_steps' should return true");

    Structure invalidWait = (Structure) Literal.parseLiteral("wait_steps(invalid)");
    assertFalse(
        env.executeAction("rescuer_1", invalidWait),
        "Action 'wait_steps' with invalid argument should return false");
  }

  @Test
  void testExecuteActionUnknown() {
    Structure action = (Structure) Literal.parseLiteral("unknown_action");
    boolean result = env.executeAction("rescuer_1", action);
    assertFalse(result, "Unknown action should return false (failure)");
  }

  @Test
  void testUnregisteredResponderHasNoPercepts() {
    Collection<Literal> percepts = env.getPercepts("unregistered_agent");
    assertNotNull(percepts, "getPercepts should not return null");
    assertTrue(percepts.isEmpty(), "Unregistered agent should have empty percepts");
  }
}
