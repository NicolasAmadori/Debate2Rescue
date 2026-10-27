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
  public void testMovesAreAddedToTheory() {
    d.addContender(1);
    d.addValue(2, 1, "severity", 3);
    d.addChallenge(1, 4, "severity");

    String theory = d.theory();

    assertTrue(theory.contains("contender(1)."));
    assertTrue(theory.contains("m2 :=> val(1, severity, 3)."));
    assertTrue(theory.contains("challenged(1, 4, severity)."));
  }

  @Test
  public void testOnlyTheFirstValueOfAReasonCounts() {
    d.addValue(2, 1, "severity", 3);
    d.addValue(5, 1, "severity", 1);

    assertTrue(d.theory().contains("val(1, severity, 3)"));
    assertFalse(d.theory().contains("val(1, severity, 1)"));
  }

  @Test
  public void testSingleClaimWins() {
    d.addContender(1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get(buildClaim(1)));
  }

  @Test
  public void testOldestEmergencyWinsWithClaimsOnly() {
    d.addContender(1);
    d.addContender(4);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(4)));
  }

  @Test
  public void testUnansweredChallengeHolds() {
    d.addContender(1);
    d.addContender(4);
    d.addValue(3, 4, "severity", 2);
    d.addChallenge(4, 1, "severity");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("challenge(severity, 4, 1)"));
    assertEquals(OUT, evaluation.get(buildClaim(1)));
    assertEquals(IN, evaluation.get(buildClaim(4)));
  }

  @Test
  public void testBetterAnswerTurnsTheChallenge() {
    d.addContender(1);
    d.addContender(4);
    d.addValue(3, 4, "severity", 2);
    d.addChallenge(4, 1, "severity");
    d.addValue(4, 1, "severity", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("answer(severity, 1, 4)"));
    assertEquals(OUT, evaluation.get("challenge(severity, 4, 1)"));
    assertEquals(IN, evaluation.get("compare(severity, 1, 4)"));
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(4)));
  }

  @Test
  public void testTieOnAReasonLeavesTheOldestAhead() {
    d.addContender(1);
    d.addContender(4);
    d.addValue(3, 4, "severity", 2);
    d.addChallenge(4, 1, "severity");
    d.addValue(4, 1, "severity", 2);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(4)));
  }

  @Test
  public void testStrongerReasonWinsOverWeakerOne() {
    // 17 is more severe, but 8 waited more and waiting is the stronger reason
    d.addContender(8);
    d.addContender(17);
    d.addValue(1, 17, "severity", 3);
    d.addChallenge(17, 8, "severity");
    d.addValue(2, 8, "severity", 4);
    d.addValue(3, 8, "waiting", 2);
    d.addChallenge(8, 17, "waiting");
    d.addValue(4, 17, "waiting", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(waiting, 8, 17)"));
    assertEquals(OUT, evaluation.get("rank(severity, 17, 8)"));
    assertEquals(IN, evaluation.get(buildClaim(8)));
    assertEquals(OUT, evaluation.get(buildClaim(17)));
  }

  @Test
  public void testStrongerChallengeBreaksACycle() {
    // 3 is ahead of 10 on waiting, 10 of 6 on severity, 6 of 3 on severity: no claim holds
    d.addContender(3);
    d.addContender(6);
    d.addContender(10);
    d.addValue(1, 3, "waiting", 1);
    d.addChallenge(3, 10, "waiting");
    d.addValue(2, 10, "waiting", 0);
    d.addValue(3, 10, "severity", 3);
    d.addChallenge(10, 6, "severity");
    d.addValue(4, 6, "severity", 4);
    d.addChallenge(6, 3, "severity");
    d.addValue(5, 3, "severity", 5);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(OUT, evaluation.get(buildClaim(3)));
    assertEquals(OUT, evaluation.get(buildClaim(6)));
    assertEquals(OUT, evaluation.get(buildClaim(10)));

    // 3 challenges who is ahead of it on the stronger reason
    d.addChallenge(3, 6, "waiting");

    evaluation = d.evaluate();
    assertEquals(IN, evaluation.get(buildClaim(3)));
    assertEquals(OUT, evaluation.get(buildClaim(6)));
    assertEquals(OUT, evaluation.get(buildClaim(10)));
  }

  @Test
  public void testCriticalIsNotOvertakenByWaiting() {
    // 7 waited more, but 3 is critical
    d.addContender(3);
    d.addContender(7);
    d.addValue(1, 7, "waiting", 1);
    d.addChallenge(7, 3, "waiting");
    // 3 does not answer on waiting, it shows it is critical
    d.addValue(2, 3, "severity", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("life_first(7, 3)"));
    assertEquals(OUT, evaluation.get("rank(waiting, 7, 3)"));
    assertEquals(IN, evaluation.get(buildClaim(3)));
    assertEquals(OUT, evaluation.get(buildClaim(7)));
  }

  @Test
  public void testWaitingCountsBetweenCriticalEmergencies() {
    // 7 shows it is critical too, the exception does not hold anymore
    d.addContender(3);
    d.addContender(7);
    d.addValue(1, 7, "waiting", 1);
    d.addChallenge(7, 3, "waiting");
    d.addValue(2, 3, "severity", 1);
    d.addValue(3, 7, "severity", 2);
    d.addChallenge(7, 3, "severity");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(OUT, evaluation.get("life_first(7, 3)"));
    assertEquals(IN, evaluation.get("rank(waiting, 7, 3)"));
    assertEquals(OUT, evaluation.get(buildClaim(3)));
    assertEquals(IN, evaluation.get(buildClaim(7)));
  }

  @Test
  public void testSpreadingEmergencyIsNotOvertakenOnSeverity() {
    // 2 has a lower ESI, but 5 has people trapped in an escalating emergency
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "trapped", 1);
    d.addChallenge(5, 2, "trapped");
    d.addValue(2, 2, "severity", 3);
    d.addChallenge(2, 5, "severity");
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "escalating", 1);
    d.addChallenge(5, 2, "escalating");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("no_time(5, 2)"));
    assertEquals(OUT, evaluation.get("rank(severity, 2, 5)"));
    assertEquals(IN, evaluation.get("rank(trapped, 5, 2)"));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
    assertEquals(IN, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testCriticalStillWinsOverSpreading() {
    // 2 is critical: severity holds again
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "trapped", 1);
    d.addChallenge(5, 2, "trapped");
    d.addValue(2, 2, "severity", 2);
    d.addChallenge(2, 5, "severity");
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "escalating", 1);
    d.addChallenge(5, 2, "escalating");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(OUT, evaluation.get("no_time(5, 2)"));
    assertEquals(IN, evaluation.get("rank(severity, 2, 5)"));
    assertEquals(IN, evaluation.get(buildClaim(2)));
    assertEquals(OUT, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testPreventCatastropheUndercutsSeverity() {
    // 2 has lower ESI (3 vs 4), but 5 threatens collateral damage at scale (size 2, collateral_risk
    // 1)
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "collateral_risk", 1);
    d.addChallenge(5, 2, "collateral_risk");
    d.addValue(2, 2, "severity", 3);
    d.addChallenge(2, 5, "severity");
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "size", 2);
    d.addChallenge(5, 2, "size");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("prevent_catastrophe(5, 2)"));
    assertEquals(OUT, evaluation.get("rank(severity, 2, 5)"));
    assertEquals(IN, evaluation.get("rank(collateral_risk, 5, 2)"));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
    assertEquals(IN, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testCriticalStillWinsOverCatastrophic() {
    // 2 is critical (ESI 2): severity holds against catastrophe exception
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "collateral_risk", 1);
    d.addChallenge(5, 2, "collateral_risk");
    d.addValue(2, 2, "severity", 2);
    d.addChallenge(2, 5, "severity");
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "size", 2);
    d.addChallenge(5, 2, "size");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(OUT, evaluation.get("prevent_catastrophe(5, 2)"));
    assertEquals(IN, evaluation.get("rank(severity, 2, 5)"));
    assertEquals(IN, evaluation.get(buildClaim(2)));
    assertEquals(OUT, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testDisasterScaleUndercutsWaiting() {
    // 1 waited more (waiting 2 vs 0), but 2 is a major disaster (size 2, victims 4)
    d.addContender(1);
    d.addContender(2);
    d.addValue(1, 1, "waiting", 2);
    d.addChallenge(1, 2, "waiting");
    // 2 reveals it is a major disaster
    d.addValue(2, 2, "size", 2);
    d.addChallenge(2, 1, "size");
    d.addValue(3, 2, "victims", 4);
    d.addChallenge(2, 1, "victims");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("disaster_scale(1, 2)"));
    assertEquals(OUT, evaluation.get("rank(waiting, 1, 2)"));
    assertEquals(OUT, evaluation.get(buildClaim(1)));
    assertEquals(IN, evaluation.get(buildClaim(2)));
  }

  @Test
  public void testCriticalStillWinsOverDisasterScale() {
    // 1 is critical (ESI 1): waiting holds even against major disaster
    d.addContender(1);
    d.addContender(2);
    d.addValue(1, 1, "waiting", 2);
    d.addChallenge(1, 2, "waiting");
    d.addValue(2, 1, "severity", 1);
    d.addValue(3, 2, "size", 2);
    d.addChallenge(2, 1, "size");
    d.addValue(4, 2, "victims", 4);
    d.addChallenge(2, 1, "victims");

    Map<String, String> evaluation = d.evaluate();
    assertEquals(OUT, evaluation.get("disaster_scale(1, 2)"));
    assertEquals(IN, evaluation.get("rank(waiting, 1, 2)"));
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
  }
}
