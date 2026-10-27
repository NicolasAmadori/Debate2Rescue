package arg;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DebateLoggerTest {

  @Test
  void testDefaultConstructorUsesTimestampedExecutionFolder() {
    DebateLogger logger = new DebateLogger();
    Path logDir = logger.getLogDirectory();
    assertTrue(logDir.startsWith(Path.of("logs")));
    assertFalse(logDir.endsWith("debates"));
  }

  @Test
  void testConcededOpponentsProduceRichDeniedAndJustification(@TempDir Path tempDir)
      throws IOException {
    DebateLogger logger = new DebateLogger(tempDir);
    int debateId = 2;
    String resourceType = "police_car";
    List<String> contenders = List.of("c(10, rescuer_3)", "c(11, rescuer_16)");

    logger.logStart(debateId, resourceType, contenders);
    logger.logMove(debateId, 5, "rescuer_3", 10, "claim", "claim");
    logger.logMove(debateId, 6, "rescuer_16", 11, "claim", "claim");
    logger.logStatus(debateId, 10, "[]");

    logger.logMove(debateId, 7, "rescuer_16", 11, "challenge", "challenge(10, severity, 3)");
    logger.logStatus(debateId, 11, "[rank(severity, 11, 10)]");

    logger.logMove(debateId, 16, "rescuer_3", 10, "concede", "withdrew claim");
    logger.logStatus(debateId, 11, "[]");

    // At conclusion, orchestrator passes empty losers because 10 already conceded
    String justification = logger.logEnd(debateId, resourceType, 11, List.of(), "[]");

    Path expectedFile = tempDir.resolve("debate2_police_car.log");
    assertTrue(Files.exists(expectedFile));

    String content = Files.readString(expectedFile);
    assertTrue(content.contains("DENIED CONTENDERS: c(10, rescuer_3) [conceded]"));
    assertTrue(
        content.contains(
            "JUSTIFICATION: All opponent(s) conceded (deciding argument: [rank(severity, 11, 10)])"));
    assertTrue(justification.contains("[rank(severity, 11, 10)]"));
  }
}
