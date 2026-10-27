package arg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles persistent file logging for argumentation debates. Each debate session is written to a
 * dedicated log file capturing its starting setup, debating actions (moves, turn states), and final
 * decision.
 */
public class DebateLogger {
  private static final Logger LOGGER = Logger.getLogger(DebateLogger.class.getName());
  private static final DateTimeFormatter TIME_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter EXECUTION_FOLDER_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private final Path logDirectory;
  private final Map<Integer, DebateSession> activeSessions = new HashMap<>();

  public DebateLogger() {
    this(Path.of("logs", LocalDateTime.now().format(EXECUTION_FOLDER_FMT)));
  }

  public DebateLogger(Path logDirectory) {
    this.logDirectory = logDirectory;
  }

  public Path getLogDirectory() {
    return logDirectory;
  }

  /**
   * Logs the start of a debate.
   *
   * @param debateId unique debate id
   * @param resourceType the contested resource type
   * @param contenders list of contenders, e.g. c(1, rescuer_1)
   */
  public synchronized void logStart(int debateId, String resourceType, List<String> contenders) {
    try {
      Files.createDirectories(logDirectory);
      Path logFile = logDirectory.resolve(String.format("debate%d_%s.log", debateId, resourceType));
      BufferedWriter writer =
          Files.newBufferedWriter(
              logFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

      DebateSession session = new DebateSession(debateId, resourceType, contenders, writer);
      activeSessions.put(debateId, session);

      writer.write("=================================================================\n");
      writer.write(
          String.format("DEBATE #%d STARTED: Resource Type [%s]\n", debateId, resourceType));
      writer.write("Timestamp: " + LocalDateTime.now().format(TIME_FMT) + "\n");
      writer.write("Contenders: " + String.join(", ", contenders) + "\n");
      writer.write("=================================================================\n\n");
      writer.write("--- DEBATE MOVES & TURNS ---\n");
      writer.flush();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize debate log file for debate #" + debateId, e);
    }
  }

  /**
   * Logs a move made during the debate.
   *
   * @param debateId unique debate id
   * @param moveId move id
   * @param rescuer agent making or associated with the move
   * @param emergency emergency id
   * @param type performative / move type (claim, challenge, answer, concede)
   * @param content content of the move
   */
  public synchronized void logMove(
      int debateId, int moveId, String rescuer, int emergency, String type, String content) {
    DebateSession session = activeSessions.get(debateId);
    if (session == null) {
      LOGGER.warning("Attempted to log move for unknown or closed debate #" + debateId);
      return;
    }
    if ("concede".equalsIgnoreCase(type)) {
      session.concededEmergencies.add(emergency);
    }
    try {
      session.moveCount++;
      session.writer.write(
          String.format(
              "[%s] Move #%d: %s (Emergency %d) -> %s: %s\n",
              LocalDateTime.now().format(TIME_FMT),
              moveId,
              rescuer,
              emergency,
              type.toUpperCase(),
              content));
      session.writer.flush();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to write move to debate log #" + debateId, e);
    }
  }

  /**
   * Logs the current status of the debate after evaluating the argumentation theory.
   *
   * @param debateId unique debate id
   * @param currentWinner emergency id currently leading
   * @param reason winning reason or justification
   */
  public synchronized void logStatus(int debateId, int currentWinner, String reason) {
    DebateSession session = activeSessions.get(debateId);
    if (session == null) {
      return;
    }
    if (reason != null
        && !reason.isBlank()
        && !reason.equals("[]")
        && !reason.equals("tie_breaker_oldest")) {
      session.lastDecidingReason = reason;
    }
    try {
      session.writer.write(
          String.format(
              "       >>> Status: Leading Emergency = %d | Reason: %s\n\n", currentWinner, reason));
      session.writer.flush();
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to write status to debate log #" + debateId, e);
    }
  }

  /**
   * Logs the final decision and conclusion of the debate.
   *
   * @param debateId unique debate id
   * @param resourceType the contested resource type
   * @param winner winning emergency id
   * @param losers list of denied/losing emergency ids or contenders
   * @param finalReason final argumentation justification
   * @return the resolved effective justification
   */
  public synchronized String logEnd(
      int debateId, String resourceType, int winner, List<String> losers, String finalReason) {
    DebateSession session = activeSessions.remove(debateId);
    if (session == null) {
      LOGGER.warning("Attempted to end unknown or closed debate #" + debateId);
      return finalReason;
    }

    List<String> formattedDenied = new ArrayList<>();
    if (session.initialContenders != null && !session.initialContenders.isEmpty()) {
      for (String contender : session.initialContenders) {
        Integer emId = extractEmergencyId(contender);
        if (emId != null && emId == winner) {
          continue;
        }
        boolean conceded = emId != null && session.concededEmergencies.contains(emId);
        formattedDenied.add(contender + (conceded ? " [conceded]" : " [denied]"));
      }
    } else if (losers != null && !losers.isEmpty()) {
      for (String loser : losers) {
        formattedDenied.add(loser + " [denied]");
      }
    }

    String effectiveJustification;
    if (finalReason != null
        && !finalReason.isBlank()
        && !finalReason.equals("[]")
        && !finalReason.equals("tie_breaker_oldest")) {
      effectiveJustification = finalReason;
    } else if ("tie_breaker_oldest".equals(finalReason)) {
      effectiveJustification = "Tie-breaker (oldest emergency #" + winner + ")";
    } else if (!session.concededEmergencies.isEmpty()) {
      if (session.lastDecidingReason != null) {
        effectiveJustification =
            "All opponent(s) conceded (deciding argument: " + session.lastDecidingReason + ")";
      } else {
        effectiveJustification = "All opponent(s) conceded (no counter-arguments)";
      }
    } else {
      effectiveJustification =
          "Default claim accepted (tie-breaker: oldest emergency #" + winner + ")";
    }

    try {
      session.writer.write("\n=================================================================\n");
      session.writer.write(
          String.format("DEBATE #%d CONCLUDED: Resource Type [%s]\n", debateId, resourceType));
      session.writer.write("Timestamp: " + LocalDateTime.now().format(TIME_FMT) + "\n");
      session.writer.write(String.format("FINAL DECISION: Winner is Emergency %d\n", winner));
      session.writer.write(
          "DENIED CONTENDERS: "
              + (formattedDenied.isEmpty() ? "None" : String.join(", ", formattedDenied))
              + "\n");
      session.writer.write(String.format("JUSTIFICATION: %s\n", effectiveJustification));
      session.writer.write(String.format("TOTAL MOVES: %d\n", session.moveCount));
      session.writer.write("=================================================================\n");
      session.writer.flush();
      session.writer.close();
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Failed to close debate log file for debate #" + debateId, e);
    }
    return effectiveJustification;
  }

  private static Integer extractEmergencyId(String contender) {
    int start = contender.indexOf('(');
    int comma = contender.indexOf(',');
    if (start != -1 && comma != -1 && comma > start) {
      try {
        return Integer.parseInt(contender.substring(start + 1, comma).trim());
      } catch (NumberFormatException ignored) {
      }
    }
    try {
      return Integer.parseInt(contender.trim());
    } catch (NumberFormatException ignored) {
    }
    return null;
  }

  /** Checks if a debate session is currently active. */
  public synchronized boolean isDebateOpen(int debateId) {
    return activeSessions.containsKey(debateId);
  }

  private static class DebateSession {
    final int debateId;
    final String resourceType;
    final List<String> initialContenders;
    final BufferedWriter writer;
    final Set<Integer> concededEmergencies = new LinkedHashSet<>();
    String lastDecidingReason = null;
    int moveCount = 0;

    DebateSession(
        int debateId, String resourceType, List<String> initialContenders, BufferedWriter writer) {
      this.debateId = debateId;
      this.resourceType = resourceType;
      this.initialContenders = initialContenders;
      this.writer = writer;
    }
  }
}
