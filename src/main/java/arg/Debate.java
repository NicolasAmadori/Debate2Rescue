package arg;

import it.unibo.tuprolog.argumentation.core.Arg2pSolverFactory;
import it.unibo.tuprolog.argumentation.core.libs.basic.FlagsBuilder;
import it.unibo.tuprolog.argumentation.core.model.Graph;
import it.unibo.tuprolog.argumentation.core.model.LabelledArgument;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Debate {
  private static final String RULES = load("debate.pl");

  // the top rules whose labels are returned by evaluate
  private static final List<String> RETURNED =
      List.of(
          "claim(",
          "rank(",
          "compare(",
          "challenge(",
          "answer(",
          "life_first(",
          "no_time(",
          "prevent_catastrophe(",
          "disaster_scale(");

  // from the best to the worst label for who makes the argument
  private static final List<String> LABELS = List.of("in", "und", "out");

  private List<String> facts = new ArrayList<>();

  // reasons already revealed, as emergency:reason, a value is revealed only once
  private Set<String> revealed = new HashSet<>();

  /**
   * Adds an emergency claiming the resource.
   *
   * @param emergency the emergency id
   */
  public void addContender(int emergency) {
    facts.add("contender(" + emergency + ").");
  }

  /**
   * Adds the value revealed by the rescuer of an emergency as a premise. Only the first value
   * revealed for a reason counts.
   *
   * @param move the move id, used as the premise label
   * @param emergency the emergency id
   * @param reason the reason
   * @param value the value of the reason
   */
  public void addValue(int move, int emergency, String reason, int value) {
    if (revealed.add(emergency + ":" + reason)) {
      facts.add("m" + move + " :=> val(" + emergency + ", " + reason + ", " + value + ").");
    }
  }

  /**
   * Move of the emergency author: its claim is ahead of the one of target for reason
   *
   * @param author the emergency author of the move
   * @param target the target emergency of the move
   * @param reason the reason of the challenge
   */
  public void addChallenge(int author, int target, String reason) {
    facts.add("challenged(" + author + ", " + target + ", " + reason + ").");
  }

  /**
   * Creates the arg2p theory of the debate with rules and facts
   *
   * @return a string with the theory
   */
  public String theory() {
    return RULES + "\n" + String.join("\n", facts);
  }

  /**
   * Evaluates the theory with grounded semantics.
   *
   * @return the labels of the claims, comparisons, principles and exceptions, keyed by top rule
   */
  public Map<String, String> evaluate() {
    Map<String, String> labels = new LinkedHashMap<>();
    // we use grounded labelling so the result is only one graph
    Iterator<Graph> graphs =
        Arg2pSolverFactory.INSTANCE.evaluate(theory(), new FlagsBuilder()).iterator();
    if (graphs.hasNext()) {
      for (LabelledArgument arg : graphs.next().getLabellings()) {
        String rule = arg.getArgument().getTopRule();
        if (RETURNED.stream().anyMatch(rule::startsWith)) {
          // the same rule can top more arguments (a rank from a challenge and from a comparison),
          // the rule holds if one of them holds
          labels.merge(rule, arg.getLabel(), Debate::best);
        }
      }
    }
    return labels;
  }

  private static String best(String l1, String l2) {
    return LABELS.indexOf(l1) <= LABELS.indexOf(l2) ? l1 : l2;
  }

  private static String load(String name) {
    try (InputStream in = Debate.class.getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
