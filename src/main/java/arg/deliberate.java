package arg;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.ListTermImpl;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;
import java.util.Map;
import java.util.logging.Logger;

public class deliberate extends DefaultInternalAction {
  private static final Logger LOGGER = Logger.getLogger(deliberate.class.getName());

  @Override
  public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    Debate debate = new Debate();
    for (Term t : (ListTerm) args[0]) {
      Literal move = (Literal) t;
      int id = intArg(move, 0);
      int author = intArg(move, 1);
      Literal content = (Literal) move.getTerm(2);
      switch (content.getFunctor()) {
        case "claim" -> addClaim(debate, author, (ListTerm) content.getTerm(0));
        case "attack" ->
            debate.addAttack(id, author, intArg(content, 0), content.getTerm(1).toString());
        case "reject" ->
            debate.addReject(id, author, intArg(content, 0), content.getTerm(1).toString());
        default -> LOGGER.warning("Unknown move: " + move);
      }
    }
    LOGGER.fine("Debate theory:\n" + debate.theory());

    ListTerm labels = new ListTermImpl();
    for (Map.Entry<String, String> l : debate.evaluate().entrySet()) {
      labels.add(
          ASSyntax.createStructure(
              "label", ASSyntax.parseTerm(l.getKey()), ASSyntax.createAtom(l.getValue())));
    }
    return un.unifies(args[1], labels);
  }

  private static void addClaim(Debate debate, int emergency, ListTerm facts) throws Exception {
    // Low severity would have high value, so a high number should be the default
    int severity = 999;
    int victims = 0;
    boolean trapped = false;
    boolean escalating = false;
    int waiting = 0;
    for (Term t : facts) {
      Literal fact = (Literal) t;
      switch (fact.getFunctor()) {
        case "severity" -> severity = intArg(fact, 0);
        case "victims" -> victims = intArg(fact, 0);
        case "trapped" -> trapped = true;
        case "escalating" -> escalating = true;
        case "waiting" -> waiting = intArg(fact, 0);
        default -> LOGGER.warning("Unknown fact in claim: " + fact);
      }
    }
    debate.addClaim(emergency, severity, victims, trapped, escalating, waiting);
  }

  private static int intArg(Literal l, int index) throws Exception {
    return (int) ((NumberTerm) l.getTerm(index)).solve();
  }
}
