package arg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

public class DebateTest {

  public static final String IN = "in";
  public static final String OUT = "out";

  private Debate d = new Debate();

  private String buildClaim(int id) {
    return "claim(" + id + ")";
  }

  @Test
  public void testClaimFactsAreAddedToTheory() {
    d.addClaim(1, 2, 3, true, true, 5);

    String theory = d.theory();

    assertTrue(theory.contains("contender(1)."));
    assertTrue(theory.contains("severity(1, 2)."));
    assertTrue(theory.contains("victims(1, 3)."));
    assertTrue(theory.contains("trapped(1)."));
    assertTrue(theory.contains("escalating(1)."));
    assertTrue(theory.contains("waiting(1, 5)."));
  }

  @Test
  public void testUnsetFlagsAreNotAddedToTheory() {
    d.addClaim(1, 3, 0, false, false, 0);

    String theory = d.theory();

    assertFalse(theory.contains("trapped(1)."));
    assertFalse(theory.contains("escalating(1)."));
    assertTrue(theory.contains("waiting(1, 0)."));
  }

  @Test
  public void testSingleClaimWins() {
    d.addClaim(1, 2, 3, true, true, 10);

    Map<String, String> evaluation = d.evaluate();
    assertTrue(evaluation.containsValue(IN));
    assertEquals(IN, evaluation.get(buildClaim(1)));
  }

  @Test
  public void testOldestEmergencyWins() {
    d.addClaim(1, 4, 0, false, false, 0);
    d.addClaim(4, 2, 3, true, true, 7);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(4)));
  }
}
