package agents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jason.asSemantics.Agent;
import jason.asSyntax.Literal;
import jason.util.Config;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import model.EmergencyType;
import model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the rescuer agent defined in rescuer.asl. Each Nested group covers one aspect of the
 * agent.
 */
class RescuerAgentTest {

  private static final String RESCUER_ASL = "/rescuer.asl";

  /** The rescuer under test, freshly parsed before each test. */
  private Agent rescuer;

  /** Emergency type atom to resource atom, exactly as written in rescuer.asl. */
  private Map<String, String> resourceMapping;

  @BeforeAll
  static void disableMindInspector() {
    // Parsing an agent would otherwise start a web server inside the test JVM
    Config.get().put(Config.START_WEB_MI, "false");
  }

  @BeforeEach
  void loadRescuer() throws Exception {
    rescuer = parse(RESCUER_ASL);
    resourceMapping = readResourceMapping(rescuer);
  }

  @Nested
  class ResourceMapping {

    @Test
    void mapsEveryEmergencyType() {
      for (EmergencyType type : EmergencyType.values()) {
        String typeAtom = type.name().toLowerCase(Locale.ROOT);
        assertTrue(
            resourceMapping.containsKey(typeAtom),
            "rescuer.asl should map emergency type " + typeAtom + " to a resource");
      }
    }

    @Test
    void mapsEachEmergencyTypeAtMostOnce() {
      assertEquals(
          EmergencyType.values().length,
          resourceMapping.size(),
          "each emergency type should be mapped exactly once");
    }

    @Test
    void mapsOnlyToKnownResources() {
      for (Map.Entry<String, String> mapping : resourceMapping.entrySet()) {
        assertTrue(
            isKnownResource(mapping.getValue()),
            "rescuer.asl maps "
                + mapping.getKey()
                + " to unknown resource '"
                + mapping.getValue()
                + "'");
      }
    }

    @Test
    void coversEveryResource() {
      Set<Resource> mapped = EnumSet.noneOf(Resource.class);
      for (String resourceAtom : resourceMapping.values()) {
        if (isKnownResource(resourceAtom)) {
          mapped.add(Resource.valueOf(resourceAtom.toUpperCase(Locale.ROOT)));
        }
      }
      assertEquals(
          EnumSet.allOf(Resource.class),
          mapped,
          "every resource should be reachable from some emergency type");
    }
  }

  // Helpers

  /** Parses an AgentSpeak file from the classpath into a ready-to-inspect agent. */
  private static Agent parse(String aslResource) throws Exception {
    Agent agent = new Agent();
    agent.initAg();
    try (InputStream asl = RescuerAgentTest.class.getResourceAsStream(aslResource)) {
      agent.parseAS(asl, aslResource);
    }
    agent.addInitialBelsInBB();
    return agent;
  }

  /** Collects the resource_for(Type, Resource) beliefs of an agent. */
  private static Map<String, String> readResourceMapping(Agent agent) {
    Map<String, String> mapping = new HashMap<>();
    for (Literal belief : agent.getBB()) {
      if (!belief.getFunctor().equals("resource_for")) {
        continue;
      }
      String type = belief.getTerm(0).toString();
      String resource = belief.getTerm(1).toString();
      assertNull(mapping.put(type, resource), "duplicate mapping for emergency type " + type);
    }
    return mapping;
  }

  private static boolean isKnownResource(String atom) {
    for (Resource resource : Resource.values()) {
      if (resource.name().equalsIgnoreCase(atom)) {
        return true;
      }
    }
    return false;
  }
}
