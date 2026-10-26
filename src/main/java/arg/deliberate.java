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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class deliberate extends DefaultInternalAction {
  private static final Logger LOGGER = Logger.getLogger(deliberate.class.getName());

  @Override
  public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    Debate debate = debate((ListTerm) args[0], null);
    LOGGER.fine("Debate theory:\n" + debate.theory());
    return un.unifies(args[1], toTerm(debate.evaluate()));
  }

  /**
   * Builds the debate from the moves, as move(Id, Author, Content) terms.
   *
   * @param moves the moves of the debate
   * @param only the emergencies to keep, all of them if null
   * @return the debate
   */
  static Debate debate(ListTerm moves, Set<Integer> only) throws Exception {
    // who conceded has no claim anymore, its moves and the ones against it do not count
    Set<Integer> contenders = new HashSet<>();
    for (Term t : moves) {
      Literal move = (Literal) t;
      int author = intArg(move, 1);
      if (((Literal) move.getTerm(2)).getFunctor().equals("claim")
          && (only == null || only.contains(author))) {
        contenders.add(author);
      }
    }

    Debate debate = new Debate();
    for (Term t : moves) {
      Literal move = (Literal) t;
      int id = intArg(move, 0);
      int author = intArg(move, 1);
      Literal content = (Literal) move.getTerm(2);
      if (!contenders.contains(author)) {
        continue;
      }
      switch (content.getFunctor()) {
        case "claim" -> debate.addContender(author);
        // challenge(Target, Reason, Value)
        case "challenge" -> {
          int target = intArg(content, 0);
          String reason = content.getTerm(1).toString();
          debate.addValue(id, author, reason, intArg(content, 2));
          if (contenders.contains(target)) {
            debate.addChallenge(author, target, reason);
          }
        }
        // answer(Move, Reason, Value)
        case "answer" ->
            debate.addValue(id, author, content.getTerm(1).toString(), intArg(content, 2));
        default -> LOGGER.warning("Unknown move: " + move);
      }
    }
    return debate;
  }

  /** The labels as a Jason list of label(Rule, Label). */
  static ListTerm toTerm(Map<String, String> labels) throws Exception {
    ListTerm list = new ListTermImpl();
    for (Map.Entry<String, String> l : labels.entrySet()) {
      list.add(
          ASSyntax.createStructure(
              "label", ASSyntax.parseTerm(l.getKey()), ASSyntax.createAtom(l.getValue())));
    }
    return list;
  }

  static int intArg(Literal l, int index) throws Exception {
    return (int) ((NumberTerm) l.getTerm(index)).solve();
  }
}
