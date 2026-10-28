package arg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.junit.jupiter.api.Test;

public class ChooseTest {

  private VarTerm chosen = new VarTerm("Chosen");
  private Unifier un = new Unifier();

  private boolean choose(String moves, int me, int other, String candidates) throws Exception {
    Term[] args = {
      ASSyntax.parseList(moves),
      ASSyntax.createNumber(me),
      ASSyntax.createNumber(other),
      ASSyntax.parseList(candidates),
      chosen
    };
    return (boolean) new choose().execute(null, un, args);
  }

  @Test
  public void testWeakerReasonIsNotPlayed() throws Exception {
    // 10 is over 16 on waiting, trapped is weaker and cannot change it
    String moves =
        "[move(1, 10, claim), move(2, 16, claim),"
            + " move(3, 10, challenge(16, waiting, 1)), move(4, 16, answer(3, waiting, 0)),"
            + " move(5, 16, challenge(10, severity, 4)), move(6, 10, answer(5, severity, 4))]";

    assertFalse(choose(moves, 16, 10, "[challenge(10, trapped, 1)]"));
  }

  @Test
  public void testValueThatDoesNotBeatTheTargetIsSkipped() throws Exception {
    // 10 revealed 6 victims answering 16, more than the 5 of 11: only trapped can put 11 ahead
    String moves =
        "[move(1, 10, claim), move(2, 11, claim), move(3, 16, claim),"
            + " move(4, 16, challenge(10, victims, 2)), move(5, 10, answer(4, victims, 6))]";

    assertTrue(choose(moves, 11, 10, "[challenge(10, victims, 5), challenge(10, trapped, 1)]"));
    assertEquals("challenge(10,trapped,1)", un.get(chosen).toString());
  }

  @Test
  public void testCriticalAnswersAChallengeOnWaiting() throws Exception {
    // 7 waited more, revealing it would not help 3, showing it is critical does
    String moves = "[move(1, 3, claim), move(2, 7, claim), move(3, 7, challenge(3, waiting, 1))]";

    assertTrue(choose(moves, 3, 7, "[answer(3, waiting, 0), answer(3, severity, 1)]"));
    assertEquals("answer(3,severity,1)", un.get(chosen).toString());
  }

  @Test
  public void testTwoValuesAreNeededForSpreading() throws Exception {
    // 2 is over 5 on severity: trapped or escalating alone do not help, both together do
    String moves =
        "[move(1, 2, claim), move(2, 5, claim),"
            + " move(3, 2, challenge(5, severity, 3)), move(4, 5, answer(3, severity, 4))]";

    assertTrue(choose(moves, 5, 2, "[challenge(2, trapped, 1), challenge(2, escalating, 1)]"));
    assertEquals("challenge(2,trapped,1)", un.get(chosen).toString());
  }

  @Test
  public void testTwoValuesAreNeededForCatastrophic() throws Exception {
    // 2 is over 5 on severity: collateral_risk or size alone do not help, both together do
    String moves =
        "[move(1, 2, claim), move(2, 5, claim),"
            + " move(3, 2, challenge(5, severity, 3)), move(4, 5, answer(3, severity, 4))]";

    assertTrue(choose(moves, 5, 2, "[challenge(2, collateral_risk, 1), challenge(2, size, 2)]"));
    assertEquals("challenge(2,collateral_risk,1)", un.get(chosen).toString());
  }

  @Test
  public void testTwoValuesAreNeededForDisasterScale() throws Exception {
    // 1 is over 2 on waiting: size or victims alone do not help, both together do
    String moves =
        "[move(1, 1, claim), move(2, 2, claim),"
            + " move(3, 1, challenge(2, waiting, 2)), move(4, 2, answer(3, waiting, 0))]";

    assertTrue(choose(moves, 2, 1, "[challenge(1, size, 2), challenge(1, victims, 4)]"));
    assertEquals("challenge(1,size,2)", un.get(chosen).toString());
  }
}
