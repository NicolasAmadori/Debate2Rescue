package arg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    String theory = d.theory();

    assertTrue(theory.contains("contender(1)."));
    assertTrue(theory.contains("m2 :=> val(1, severity, 3)."));
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
    d.addValue(2, 8, "severity", 4);
    d.addValue(3, 8, "waiting", 2);
    d.addValue(4, 17, "waiting", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(waiting, 8, 17)"));
    assertEquals(OUT, evaluation.get("rank(severity, 17, 8)"));
    assertEquals(IN, evaluation.get(buildClaim(8)));
    assertEquals(OUT, evaluation.get(buildClaim(17)));
  }

  @Test
  public void testValuesAreComparedBetweenEveryone() {
    // 6 revealed its severity answering 3, 10 answering 6: 10 is ahead of 3 too
    d.addContender(3);
    d.addContender(6);
    d.addContender(10);
    d.addValue(1, 3, "severity", 4);
    d.addValue(2, 6, "severity", 3);
    d.addValue(3, 10, "severity", 2);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("compare(severity, 10, 3)"));
    assertEquals(OUT, evaluation.get(buildClaim(3)));
    assertEquals(OUT, evaluation.get(buildClaim(6)));
    assertEquals(IN, evaluation.get(buildClaim(10)));
  }

  @Test
  public void testRevealedValueChallengesEveryone() {
    // 28 revealed its severity against 14, 29 cannot overtake it on a weaker reason
    d.addContender(14);
    d.addContender(28);
    d.addContender(29);
    d.addValue(1, 28, "severity", 1);
    d.addValue(2, 29, "escalating", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(severity, 28, 29)"));
    assertEquals(OUT, evaluation.get("rank(escalating, 29, 28)"));
    assertEquals(OUT, evaluation.get(buildClaim(14)));
    assertEquals(IN, evaluation.get(buildClaim(28)));
    assertEquals(OUT, evaluation.get(buildClaim(29)));
  }

  @Test
  public void testValueThatIsNotAReasonDoesNotChallenge() {
    // not being trapped does not put 4 ahead of who did not reveal it
    d.addContender(1);
    d.addContender(4);
    d.addValue(1, 4, "trapped", 0);

    Map<String, String> evaluation = d.evaluate();
    assertNull(evaluation.get("challenge(trapped, 4, 1)"));
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(4)));
  }

  @Test
  public void testWorstValueDoesNotChallenge() {
    // severity 5 and size 1 cannot beat anyone, they do not put 4 ahead of who did not reveal them
    d.addContender(1);
    d.addContender(4);
    d.addValue(1, 4, "severity", 5);
    d.addValue(2, 4, "size", 1);

    Map<String, String> evaluation = d.evaluate();
    assertNull(evaluation.get("challenge(severity, 4, 1)"));
    assertNull(evaluation.get("challenge(size, 4, 1)"));
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(4)));
  }

  @Test
  public void testCriticalIsNotOvertakenByWaiting() {
    // 7 waited more, but 3 is critical
    d.addContender(3);
    d.addContender(7);
    d.addValue(1, 7, "waiting", 1);
    // 3 does not reveal its waiting, it shows it is critical
    d.addValue(2, 3, "severity", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("in_situation(critical, 3, 7)"));
    assertEquals(IN, evaluation.get("rank(critical, 3, 7)"));
    assertEquals(OUT, evaluation.get("rank(waiting, 7, 3)"));
    assertEquals(IN, evaluation.get(buildClaim(3)));
    assertEquals(OUT, evaluation.get(buildClaim(7)));
  }

  @Test
  public void testWaitingCountsBetweenCriticalEmergencies() {
    // 7 shows it is critical too, being critical does not put 3 ahead anymore
    d.addContender(3);
    d.addContender(7);
    d.addValue(1, 7, "waiting", 1);
    d.addValue(2, 3, "severity", 1);
    d.addValue(3, 7, "severity", 2);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(OUT, evaluation.get("in_situation(critical, 3, 7)"));
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
    d.addValue(2, 2, "severity", 3);
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "escalating", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(spreading, 5, 2)"));
    assertEquals(OUT, evaluation.get("rank(severity, 2, 5)"));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
    assertEquals(IN, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testSpreadingWinsOverCollateralRisk() {
    // 2 has a lower ESI and collateral risk, both weaker than the spreading emergency of 5
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "trapped", 1);
    d.addValue(2, 5, "escalating", 1);
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "collateral_risk", 0);
    d.addValue(5, 2, "severity", 3);
    d.addValue(6, 2, "collateral_risk", 1);
    d.addValue(7, 2, "trapped", 0);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(spreading, 5, 2)"));
    assertEquals(OUT, evaluation.get("rank(collateral_risk, 2, 5)"));
    assertEquals(IN, evaluation.get(buildClaim(5)));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
  }

  @Test
  public void testSeverityDecidesBetweenSpreadingEmergencies() {
    // both are spreading, so the next reason decides: 5 has the lower ESI
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 2, "trapped", 1);
    d.addValue(2, 2, "escalating", 1);
    d.addValue(3, 2, "severity", 5);
    d.addValue(4, 5, "trapped", 1);
    d.addValue(5, 5, "escalating", 1);
    d.addValue(6, 5, "severity", 3);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(OUT, evaluation.get("in_situation(spreading, 2, 5)"));
    assertEquals(OUT, evaluation.get("in_situation(spreading, 5, 2)"));
    assertEquals(IN, evaluation.get("rank(severity, 5, 2)"));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
    assertEquals(IN, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testCriticalStillWinsOverSpreading() {
    // 2 is critical: it is ahead of the spreading emergency of 5
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "trapped", 1);
    d.addValue(2, 2, "severity", 2);
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "escalating", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(critical, 2, 5)"));
    assertEquals(OUT, evaluation.get("rank(spreading, 5, 2)"));
    assertEquals(IN, evaluation.get(buildClaim(2)));
    assertEquals(OUT, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testCatastrophicIsNotOvertakenOnSeverity() {
    // 2 has lower ESI (3 vs 4), but 5 threatens collateral damage at scale (size 2, collateral_risk
    // 1)
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "collateral_risk", 1);
    d.addValue(2, 2, "severity", 3);
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "size", 2);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(catastrophic, 5, 2)"));
    assertEquals(OUT, evaluation.get("rank(severity, 2, 5)"));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
    assertEquals(IN, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testCriticalStillWinsOverCatastrophic() {
    // 2 is critical (ESI 2): it is ahead of the catastrophic emergency of 5
    d.addContender(2);
    d.addContender(5);
    d.addValue(1, 5, "collateral_risk", 1);
    d.addValue(2, 2, "severity", 2);
    d.addValue(3, 5, "severity", 4);
    d.addValue(4, 5, "size", 2);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(critical, 2, 5)"));
    assertEquals(OUT, evaluation.get("rank(catastrophic, 5, 2)"));
    assertEquals(IN, evaluation.get(buildClaim(2)));
    assertEquals(OUT, evaluation.get(buildClaim(5)));
  }

  @Test
  public void testMajorDisasterIsNotOvertakenOnSeverity() {
    // 1 has lower ESI (3 vs 4), but 2 is a major disaster (size 3, victims 8)
    d.addContender(1);
    d.addContender(2);
    d.addValue(1, 1, "severity", 3);
    d.addValue(2, 2, "severity", 4);
    d.addValue(3, 2, "size", 3);
    d.addValue(4, 2, "victims", 8);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(major_disaster, 2, 1)"));
    assertEquals(OUT, evaluation.get("rank(severity, 1, 2)"));
    assertEquals(OUT, evaluation.get(buildClaim(1)));
    assertEquals(IN, evaluation.get(buildClaim(2)));
  }

  @Test
  public void testWaitingStillWinsOverMajorDisaster() {
    // 1 waited more (waiting 2 vs 0): only a critical emergency gets ahead of it
    d.addContender(1);
    d.addContender(2);
    d.addValue(1, 1, "waiting", 2);
    d.addValue(2, 2, "size", 3);
    d.addValue(3, 2, "victims", 8);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(waiting, 1, 2)"));
    assertEquals(OUT, evaluation.get("rank(major_disaster, 2, 1)"));
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
  }

  @Test
  public void testCriticalStillWinsOverMajorDisaster() {
    // 2 waited more and is a major disaster, but 1 is critical (ESI 1)
    d.addContender(1);
    d.addContender(2);
    d.addValue(1, 2, "waiting", 2);
    d.addValue(2, 1, "severity", 1);
    d.addValue(3, 2, "size", 3);
    d.addValue(4, 2, "victims", 8);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(critical, 1, 2)"));
    assertEquals(OUT, evaluation.get("rank(waiting, 2, 1)"));
    assertEquals(IN, evaluation.get(buildClaim(1)));
    assertEquals(OUT, evaluation.get(buildClaim(2)));
  }

  @Test
  public void testCatastrophicWinsOverMajorDisaster() {
    // 1 is a major disaster, 2 is catastrophic
    d.addContender(1);
    d.addContender(2);
    d.addValue(1, 1, "size", 3);
    d.addValue(2, 1, "victims", 8);
    d.addValue(3, 1, "collateral_risk", 0);
    d.addValue(4, 2, "size", 2);
    d.addValue(5, 2, "victims", 1);
    d.addValue(6, 2, "collateral_risk", 1);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(catastrophic, 2, 1)"));
    assertEquals(OUT, evaluation.get("rank(major_disaster, 1, 2)"));
    assertEquals(OUT, evaluation.get(buildClaim(1)));
    assertEquals(IN, evaluation.get(buildClaim(2)));
  }

  @Test
  public void testMajorDisasterWinsOverSpreading() {
    // 1 is spreading, 2 is a major disaster
    d.addContender(1);
    d.addContender(2);
    d.addValue(1, 1, "trapped", 1);
    d.addValue(2, 1, "escalating", 1);
    d.addValue(3, 1, "size", 1);
    d.addValue(4, 1, "victims", 2);
    d.addValue(5, 2, "trapped", 0);
    d.addValue(6, 2, "size", 3);
    d.addValue(7, 2, "victims", 8);

    Map<String, String> evaluation = d.evaluate();
    assertEquals(IN, evaluation.get("rank(major_disaster, 2, 1)"));
    assertEquals(OUT, evaluation.get("rank(spreading, 1, 2)"));
    assertEquals(OUT, evaluation.get(buildClaim(1)));
    assertEquals(IN, evaluation.get(buildClaim(2)));
  }
}
