package arg;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Literal;
import jason.asSyntax.NumberTerm;
import jason.asSyntax.Term;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

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

    if (candidates.isEmpty()) {
      return false;
    }

    int next = 0;
    for (Term t : moves) {
      next = Math.max(next, deliberate.intArg((Literal) t, 0));
    }

    int before = position(moves, List.of(), me, other, next);

    // Evaluate single candidates in parallel while preserving choice order
    int finalNext = next;
    int[] singleScores = new int[candidates.size()];
    if (candidates.size() == 1) {
      singleScores[0] = position(moves, List.of(candidates.get(0)), me, other, finalNext);
    } else {
      IntStream.range(0, candidates.size())
          .parallel()
          .forEach(
              i -> {
                try {
                  singleScores[i] =
                      position(moves, List.of(candidates.get(i)), me, other, finalNext);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
    }

    for (int i = 0; i < candidates.size(); i++) {
      if (singleScores[i] > before) {
        return un.unifies(args[4], candidates.get(i));
      }
    }

    // Some situations need two values to be revealed (trapped and escalating), one move at a time
    List<int[]> pairs = new ArrayList<>();
    for (int i = 0; i < candidates.size(); i++) {
      for (int j = i + 1; j < candidates.size(); j++) {
        pairs.add(new int[] {i, j});
      }
    }

    List<int[]> synergyPairs = new ArrayList<>();
    List<int[]> otherPairs = new ArrayList<>();
    for (int[] p : pairs) {
      if (isSynergy(candidates.get(p[0]), candidates.get(p[1]))) {
        synergyPairs.add(p);
      } else {
        otherPairs.add(p);
      }
    }

    // Evaluate synergy pairs first (in parallel)
    Term chosenPair = evaluatePairs(moves, me, other, finalNext, before, candidates, synergyPairs);
    if (chosenPair != null) {
      return un.unifies(args[4], chosenPair);
    }

    // Evaluate remaining pairs if any
    chosenPair = evaluatePairs(moves, me, other, finalNext, before, candidates, otherPairs);
    if (chosenPair != null) {
      return un.unifies(args[4], chosenPair);
    }

    return false;
  }

  private static Term evaluatePairs(
      ListTerm moves,
      int me,
      int other,
      int next,
      int before,
      List<Term> candidates,
      List<int[]> pairList) {
    if (pairList.isEmpty()) {
      return null;
    }

    int[] scores = new int[pairList.size()];
    if (pairList.size() == 1) {
      int[] p = pairList.get(0);
      try {
        scores[0] =
            position(moves, List.of(candidates.get(p[0]), candidates.get(p[1])), me, other, next);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    } else {
      IntStream.range(0, pairList.size())
          .parallel()
          .forEach(
              k -> {
                int[] p = pairList.get(k);
                try {
                  scores[k] =
                      position(
                          moves,
                          List.of(candidates.get(p[0]), candidates.get(p[1])),
                          me,
                          other,
                          next);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
    }

    // Return the first candidate of the winning pair according to original loop order (i then j)
    int bestI = Integer.MAX_VALUE;
    int bestJ = Integer.MAX_VALUE;
    for (int k = 0; k < pairList.size(); k++) {
      if (scores[k] > before) {
        int[] p = pairList.get(k);
        if (p[0] < bestI || (p[0] == bestI && p[1] < bestJ)) {
          bestI = p[0];
          bestJ = p[1];
        }
      }
    }

    if (bestI != Integer.MAX_VALUE) {
      return candidates.get(bestI);
    }
    return null;
  }

  private static boolean isSynergy(Term t1, Term t2) {
    String r1 = reasonOf(t1);
    String r2 = reasonOf(t2);
    if (r1 == null || r2 == null) {
      return true;
    }
    return (r1.equals("trapped") && r2.equals("escalating"))
        || (r1.equals("escalating") && r2.equals("trapped"))
        || (r1.equals("collateral_risk") && r2.equals("size"))
        || (r1.equals("size") && r2.equals("collateral_risk"))
        || (r1.equals("size") && r2.equals("victims"))
        || (r1.equals("victims") && r2.equals("size"));
  }

  private static String reasonOf(Term t) {
    if (t instanceof Literal l && l.getArity() >= 2) {
      return l.getTerm(1).toString();
    }
    return null;
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
