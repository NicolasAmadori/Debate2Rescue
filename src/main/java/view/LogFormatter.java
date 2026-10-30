package view;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Utility class providing styled formatting and human-readable text generation for simulation and
 * debate view logging.
 */
public final class LogFormatter {

  public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

  // Semantic Colors
  public static final Color COLOR_TIME = new Color(130, 130, 130);
  public static final Color COLOR_MUTED = new Color(125, 133, 140);
  public static final Color COLOR_DEFAULT = new Color(30, 30, 30);
  public static final Color COLOR_ASSIGN = new Color(24, 90, 157);
  public static final Color COLOR_RESOLVE = new Color(25, 111, 61);
  public static final Color COLOR_DEBATE_HEADER = new Color(40, 53, 147);
  public static final Color COLOR_DEBATE_END = new Color(20, 90, 50);
  public static final Color COLOR_STATUS = new Color(14, 102, 85);
  public static final Color COLOR_ARGUMENT = new Color(20, 40, 80);

  // Performative Colors
  public static final Color COLOR_PERF_CLAIM = new Color(21, 101, 192);
  public static final Color COLOR_PERF_CHALLENGE = new Color(211, 84, 0);
  public static final Color COLOR_PERF_ANSWER = new Color(125, 60, 152);
  public static final Color COLOR_PERF_CONCEDE = new Color(140, 40, 30);

  // Contender Palette (cycled per contender)
  private static final Color[] CONTENDER_COLORS = {
    new Color(26, 82, 118), // Deep Blue
    new Color(146, 43, 33), // Crimson
    new Color(17, 122, 101), // Teal
    new Color(175, 96, 26), // Burnt Orange
    new Color(108, 52, 131) // Violet
  };

  private static final Pattern CHALLENGE_PATTERN =
      Pattern.compile("challenge\\s*\\(\\s*(\\d+)\\s*,\\s*([a-zA-Z0-9_]+)\\s*,\\s*(-?\\d+)\\s*\\)");
  private static final Pattern ANSWER_PATTERN =
      Pattern.compile("answer\\s*\\(\\s*(\\d+)\\s*,\\s*([a-zA-Z0-9_]+)\\s*,\\s*(-?\\d+)\\s*\\)");
  private static final Pattern RANK_PATTERN =
      Pattern.compile("rank\\s*\\(\\s*([a-zA-Z0-9_]+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)");

  private LogFormatter() {}

  /** Appends text with the specified attributes to a StyledDocument. */
  public static void append(StyledDocument doc, String text, AttributeSet attr) {
    try {
      doc.insertString(doc.getLength(), text, attr);
    } catch (BadLocationException ignored) {
    }
  }

  /** Builds an attribute set with the given properties. */
  public static SimpleAttributeSet createStyle(int size, boolean bold, boolean italic, Color fg) {
    SimpleAttributeSet set = new SimpleAttributeSet();
    StyleConstants.setFontFamily(set, "Segoe UI, Sans-Serif");
    StyleConstants.setFontSize(set, size);
    StyleConstants.setBold(set, bold);
    StyleConstants.setItalic(set, italic);
    if (fg != null) {
      StyleConstants.setForeground(set, fg);
    }
    return set;
  }

  /** Appends a timestamp prefix, e.g. "14:22:01 ". */
  public static void appendTimestamp(StyledDocument doc) {
    SimpleAttributeSet timeAttr = createStyle(10, false, false, COLOR_TIME);
    append(doc, LocalTime.now().format(TIME_FMT) + "  ", timeAttr);
  }

  /** Formats general simulation event messages according to LogCategory (always single line). */
  public static void appendSimulationEvent(
      StyledDocument doc, LogCategory category, String message) {
    appendTimestamp(doc);
    if (category == LogCategory.GUI_VISIBLE) {
      // Small, muted font for events already visible on the grid canvas
      SimpleAttributeSet muted = createStyle(10, false, false, COLOR_MUTED);
      append(doc, message + "\n", muted);
    } else {
      // Meaningful simulation events (assignments, resolutions, agent messages)
      if (message.contains("assigned to")) {
        SimpleAttributeSet assignStyle = createStyle(12, true, false, COLOR_ASSIGN);
        append(doc, "📋 " + message + "\n", assignStyle);
      } else if (message.contains("resolved by")) {
        SimpleAttributeSet resolveStyle = createStyle(12, true, false, COLOR_RESOLVE);
        append(doc, "✅ " + message + "\n", resolveStyle);
      } else {
        SimpleAttributeSet regularStyle = createStyle(11, false, false, COLOR_DEFAULT);
        append(doc, message + "\n", regularStyle);
      }
    }
  }

  /** Appends a single-line Debate Start event for the top simulation log. */
  public static void appendDebateStartOneLine(
      StyledDocument doc, int debateId, String resourceType, List<String> contenders) {
    appendTimestamp(doc);
    SimpleAttributeSet titleAttr = createStyle(11, true, false, COLOR_DEBATE_HEADER);
    append(
        doc,
        "⚔️ DEBATE #" + debateId + " STARTED: [" + resourceType.toUpperCase() + "] between ",
        titleAttr);

    SimpleAttributeSet normalAttr = createStyle(11, false, false, COLOR_DEFAULT);
    for (int i = 0; i < contenders.size(); i++) {
      String contender = contenders.get(i);
      Color cColor = getContenderColor(i);
      append(doc, contender, createStyle(11, true, false, cColor));
      if (i < contenders.size() - 1) {
        append(doc, " vs ", normalAttr);
      }
    }
    append(doc, "\n", normalAttr);
  }

  /** Appends a single-line Debate Concluded event for the top simulation log. */
  public static void appendDebateEndOneLine(
      StyledDocument doc,
      int debateId,
      String resourceType,
      int winner,
      String winningRescuer,
      String justification) {
    appendTimestamp(doc);
    SimpleAttributeSet titleAttr = createStyle(11, true, false, COLOR_DEBATE_END);
    append(doc, "🏆 DEBATE #" + debateId + " WON by Emergency #" + winner + " — ", titleAttr);

    SimpleAttributeSet grantAttr = createStyle(11, true, false, COLOR_RESOLVE);
    if (winningRescuer != null && !winningRescuer.isBlank()) {
      append(doc, "[" + resourceType.toUpperCase() + "] granted to " + winningRescuer, grantAttr);
    } else {
      append(
          doc, "[" + resourceType.toUpperCase() + "] granted to Emergency #" + winner, grantAttr);
    }

    String justClean = formatJustification(justification);
    SimpleAttributeSet reasonAttr = createStyle(11, false, true, COLOR_MUTED);
    append(doc, " (" + justClean + ")\n", reasonAttr);
  }

  /** Appends a rich Debate Start banner to the debate document without emergency infos. */
  public static void appendDebateStartBanner(
      StyledDocument doc, int debateId, String resourceType, List<String> contenders) {
    appendDebateStartBanner(doc, debateId, resourceType, contenders, Collections.emptyList());
  }

  /** Appends a rich Debate Start banner to the debate document with emergency infos recap. */
  public static void appendDebateStartBanner(
      StyledDocument doc,
      int debateId,
      String resourceType,
      List<String> contenders,
      List<String> emergencyInfos) {
    SimpleAttributeSet borderAttr = createStyle(11, true, false, COLOR_DEBATE_HEADER);
    SimpleAttributeSet titleAttr = createStyle(13, true, false, COLOR_DEBATE_HEADER);
    SimpleAttributeSet normalAttr = createStyle(11, false, false, COLOR_DEFAULT);

    append(doc, "\n───────────────────────────────────────────────────\n", borderAttr);
    appendTimestamp(doc);
    append(
        doc,
        "⚔️  DEBATE #" + debateId + " STARTED  |  Resource: [" + resourceType.toUpperCase() + "]\n",
        titleAttr);
    append(doc, "    Contenders: ", createStyle(11, true, false, COLOR_DEFAULT));

    for (int i = 0; i < contenders.size(); i++) {
      String contender = contenders.get(i);
      Color cColor = getContenderColor(i);
      append(doc, contender, createStyle(11, true, false, cColor));
      if (i < contenders.size() - 1) {
        append(doc, " vs ", normalAttr);
      }
    }
    append(doc, "\n", normalAttr);

    if (emergencyInfos != null && !emergencyInfos.isEmpty()) {
      append(doc, "    Emergency Info Recap:\n", createStyle(11, true, false, COLOR_ARGUMENT));
      for (int i = 0; i < emergencyInfos.size(); i++) {
        String info = emergencyInfos.get(i);
        Color cColor = getContenderColor(i);
        append(doc, "      • ", createStyle(11, true, false, cColor));
        append(doc, info + "\n", createStyle(11, false, false, COLOR_DEFAULT));
      }
    }
    append(doc, "───────────────────────────────────────────────────\n", borderAttr);
  }

  /** Appends a turn move to the debate document in human-readable card style. */
  public static void appendDebateMove(
      StyledDocument doc,
      int moveId,
      String rescuer,
      int emergency,
      String type,
      String content,
      int contenderIndex) {
    appendTimestamp(doc);

    // Move index & Rescuer
    SimpleAttributeSet moveHeader = createStyle(11, true, false, COLOR_DEFAULT);
    append(doc, "Move #" + moveId + " │ ", moveHeader);

    Color cColor = getContenderColor(contenderIndex);
    SimpleAttributeSet agentAttr = createStyle(11, true, false, cColor);
    append(doc, rescuer + " (Em #" + emergency + ") ", agentAttr);

    // Performative Badge
    Color perfColor = getPerformativeColor(type);
    SimpleAttributeSet perfAttr = createStyle(11, true, false, perfColor);
    append(doc, "➔ [" + type.toUpperCase() + "]\n", perfAttr);

    // Human-readable content indented
    String humanReadable = formatMoveContent(type, content);
    SimpleAttributeSet contentAttr = createStyle(11, false, false, COLOR_ARGUMENT);
    append(doc, "    ↳ " + humanReadable + "\n", contentAttr);
  }

  /** Appends status evaluation / turn winner callout to the debate document. */
  public static void appendDebateStatus(StyledDocument doc, int currentWinner, String reason) {
    String formattedReason = formatStatusReason(currentWinner, reason);
    SimpleAttributeSet statusAttr = createStyle(11, false, true, COLOR_STATUS);
    append(doc, "       >>> Status: " + formattedReason + "\n\n", statusAttr);
  }

  /** Appends a rich Debate Concluded banner to the debate document. */
  public static void appendDebateEndBanner(
      StyledDocument doc,
      int debateId,
      String resourceType,
      int winner,
      String winningRescuer,
      List<String> losers,
      String justification) {
    SimpleAttributeSet borderAttr = createStyle(11, true, false, COLOR_DEBATE_END);
    SimpleAttributeSet titleAttr = createStyle(13, true, false, COLOR_DEBATE_END);
    SimpleAttributeSet boldAttr = createStyle(11, true, false, COLOR_DEFAULT);
    SimpleAttributeSet bodyAttr = createStyle(11, false, false, COLOR_DEFAULT);

    append(doc, "\n═══════════════════════════════════════════════════\n", borderAttr);
    appendTimestamp(doc);
    append(
        doc,
        "🏆  DEBATE #" + debateId + " CONCLUDED  |  Winner: Emergency #" + winner + "\n",
        titleAttr);

    if (winningRescuer != null && !winningRescuer.isBlank()) {
      append(doc, "    Resource [" + resourceType.toUpperCase() + "] GRANTED to: ", boldAttr);
      append(
          doc,
          winningRescuer + " for Emergency #" + winner + "\n",
          createStyle(11, true, false, COLOR_RESOLVE));
    } else {
      append(
          doc,
          "    Resource ["
              + resourceType.toUpperCase()
              + "] GRANTED for Emergency #"
              + winner
              + "\n",
          boldAttr);
    }

    append(doc, "    Justification: ", boldAttr);
    append(
        doc,
        formatJustification(justification) + "\n",
        createStyle(11, false, true, COLOR_DEBATE_END));

    if (losers != null && !losers.isEmpty()) {
      append(doc, "    Denied: ", boldAttr);
      append(doc, String.join(", ", losers) + "\n", bodyAttr);
    }
    append(doc, "═══════════════════════════════════════════════════\n\n", borderAttr);
  }

  /** Formats raw move content into human-readable text (Option A). */
  public static String formatMoveContent(String type, String content) {
    if (type == null) {
      return content != null ? content : "";
    }
    String lowerType = type.toLowerCase().trim();
    switch (lowerType) {
      case "claim" -> {
        return "Claims the contested resource";
      }
      case "concede" -> {
        return "Conceded and withdrew claim";
      }
      case "challenge" -> {
        Matcher m = CHALLENGE_PATTERN.matcher(content);
        if (m.find()) {
          String target = m.group(1);
          String reason = formatReasonName(m.group(2));
          String val = m.group(3);
          return "Challenges Emergency #" + target + " on " + reason + " (value = " + val + ")";
        }
        return content;
      }
      case "answer" -> {
        Matcher m = ANSWER_PATTERN.matcher(content);
        if (m.find()) {
          String moveId = m.group(1);
          String reason = formatReasonName(m.group(2));
          String val = m.group(3);
          return "Answers Move #" + moveId + " with " + reason + " (value = " + val + ")";
        }
        return content;
      }
      default -> {
        return content;
      }
    }
  }

  /** Formats status reasons into a clear human-readable sentence. */
  public static String formatStatusReason(int winner, String reason) {
    if (reason == null || reason.isBlank() || "[]".equals(reason)) {
      return "Emergency #" + winner + " currently leading (default claim)";
    }
    if ("tie_breaker_oldest".equals(reason)) {
      return "Emergency #" + winner + " leading by tie-breaker (oldest emergency)";
    }
    Matcher m = RANK_PATTERN.matcher(reason);
    if (m.find()) {
      String criterion = formatReasonName(m.group(1));
      String win = m.group(2);
      String lose = m.group(3);
      return "Emergency #" + win + " leads over #" + lose + " by " + criterion;
    }
    return "Leading: Emergency #" + winner + " | Reason: " + reason;
  }

  public static String formatJustification(String justification) {
    if (justification == null || justification.isBlank() || "[]".equals(justification)) {
      return "Default claim accepted (no contested counter-arguments)";
    }
    Matcher m = RANK_PATTERN.matcher(justification);
    if (m.find()) {
      String criterion = formatReasonName(m.group(1));
      String win = m.group(2);
      String lose = m.group(3);
      return justification.replace(
          m.group(0), "Emergency #" + win + " leads over #" + lose + " by " + criterion);
    }
    return justification;
  }

  public static String formatReasonName(String raw) {
    if (raw == null) {
      return "";
    }
    return switch (raw.toLowerCase().trim()) {
      case "severity" -> "Severity";
      case "victims" -> "Victim Count";
      case "trapped" -> "Trapped Victims";
      case "escalating" -> "Escalation Risk";
      case "waiting" -> "Wait Time / Denials";
      case "critical" -> "Critical Condition";
      case "catastrophic" -> "Catastrophic Risk";
      case "major_disaster" -> "Major Disaster";
      case "spreading" -> "Spreading Emergency";
      default -> raw;
    };
  }

  public static Color getContenderColor(int index) {
    return CONTENDER_COLORS[Math.abs(index) % CONTENDER_COLORS.length];
  }

  public static Color getPerformativeColor(String type) {
    if (type == null) {
      return COLOR_DEFAULT;
    }
    return switch (type.toLowerCase().trim()) {
      case "claim" -> COLOR_PERF_CLAIM;
      case "challenge" -> COLOR_PERF_CHALLENGE;
      case "answer" -> COLOR_PERF_ANSWER;
      case "concede" -> COLOR_PERF_CONCEDE;
      default -> COLOR_DEFAULT;
    };
  }
}
