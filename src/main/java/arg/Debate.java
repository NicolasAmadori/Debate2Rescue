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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Debate {
  private static final String RULES = load("debate.pl");

  private List<String> facts = new ArrayList<>();

  /**
   * Adds a claim to the facts.
   *
   * @param emergency the emergency id
   * @param severity the ESI of the emergency
   * @param victims number of victims
   * @param trapped whether the emergency is trapped
   * @param escalating whether the emergency is escalating
   * @param waiting the waiting time for the emergency
   */
  public void addClaim(
      int emergency, int severity, int victims, boolean trapped, boolean escalating, int waiting) {
    facts.add("contender(" + emergency + ").");
    facts.add("severity(" + emergency + ", " + severity + ").");
    facts.add("victims(" + emergency + ", " + victims + ").");
    if (trapped) {
      facts.add("trapped(" + emergency + ").");
    }
    if (escalating) {
      facts.add("escalating(" + emergency + ").");
    }
    facts.add("waiting(" + emergency + ", " + waiting + ").");
  }

  /**
   * Creates the arg2p theory of the debate with rules and facts
   *
   * @return a string with the theory
   */
  public String theory() {
    return RULES + "\n" + String.join("\n", facts);
  }

  public Map<String, String> evaluate() {
    Map<String, String> labels = new LinkedHashMap<>();
    // we use grounded labelling so the result is only one graph
    Iterator<Graph> graphs =
        Arg2pSolverFactory.INSTANCE.evaluate(theory(), new FlagsBuilder()).iterator();
    if (graphs.hasNext()) {
      for (LabelledArgument arg : graphs.next().getLabellings()) {
        String rule = arg.getArgument().getTopRule();
        if (rule.startsWith("claim(") || rule.startsWith("move(")) {
          labels.put(rule, arg.getLabel());
        }
      }
    }
    return labels;
  }

  private static String load(String name) {
    try (InputStream in = Debate.class.getResourceAsStream(name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
