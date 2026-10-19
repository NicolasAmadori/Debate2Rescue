package arg;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  public void testPrintTheoryAndClaims() {
    d.addClaim(1, 2, 3);
    d.addClaim(2, 4, 0);

    System.out.println(d.theory());

    System.out.println("Graph evaluation");
    d.evaluate().forEach((k, v) -> System.out.println(k + " -> " + v));
  }

  @Test
  public void testSingleClaimWins() {
    d.addClaim(1, 2, 3);
    d.addClaim(2, 4, 0);

    Map<String, String> evaluation = d.evaluate();
    assertTrue(evaluation.containsValue(IN));
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
  }

  @Test
  public void testOldestEmergencyWinsWithoutAttacks() {
    d.addClaim(1, 2, 3);
    d.addClaim(4, 4, 0);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get(buildClaim(1)));
  }
}
