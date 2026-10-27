package arg;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * arg.choose(Moves, Me, Other, Candidates, Chosen): Chosen is the first of the Candidates that
 * improves the position of emergency Me against Other, according to arg2p. It fails if none does.
 */
public class choose extends DefaultInternalAction {

  @Override
  public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
    ListTerm moves = (ListTerm) args[0];
    int me = (int) ((NumberTerm) args[1]).solve();
    int other = (int) ((NumberTerm) args[2]).solve();
    List<Term> candidates = ((ListTerm) args[3]).getAsList();

    int next = 0;
    for (Term t : moves) {
      next = Math.max(next, deliberate.intArg((Literal) t, 0));
    }

    int before = position(moves, List.of(), me, other, next);
    for (Term candidate : candidates) {
      if (position(moves, List.of(candidate), me, other, next) > before) {
        return un.unifies(args[4], candidate);
      }
    }
    // some situations need two values to be revealed (trapped and escalating), one move at a time
    for (int i = 0; i < candidates.size(); i++) {
      for (int j = i + 1; j < candidates.size(); j++) {
        if (position(moves, List.of(candidates.get(i), candidates.get(j)), me, other, next)
            > before) {
          return un.unifies(args[4], candidates.get(i));
        }
      }
    }
    return false;
  }

  /** 1 if Me is over Other after the extra moves of Me, -1 if Other is over Me, 0 otherwise. */
  private static int position(ListTerm moves, List<Term> extra, int me, int other, int last)
      throws Exception {
    ListTerm all = moves.cloneLT();
    for (Term content : extra) {
      all.add(
          ASSyntax.createStructure(
              "move", ASSyntax.createNumber(++last), ASSyntax.createNumber(me), content));
    }
    Map<String, String> labels = deliberate.debate(all, Set.of(me, other)).evaluate();

    int position = 0;
    for (Map.Entry<String, String> l : labels.entrySet()) {
      Literal rule = ASSyntax.parseLiteral(l.getKey());
      if (rule.getFunctor().equals("rank") && l.getValue().equals("in")) {
        int a = deliberate.intArg(rule, 1);
        int b = deliberate.intArg(rule, 2);
        if (a == other && b == me) {
          return -1;
        }
        if (a == me && b == other) {
          position = 1;
        }
      }
    }
    return position;
  }
}
