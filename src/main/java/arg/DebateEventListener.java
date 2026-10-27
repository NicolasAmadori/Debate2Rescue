package arg;

import java.util.List;

/** Listener interface for observing debate events dispatched by DebateLogger. */
public interface DebateEventListener {

  /**
   * Called when a debate session starts.
   *
   * @param debateId unique debate id
   * @param resourceType the contested resource type
   * @param contenders list of contenders (e.g. "c(1, rescuer_1)")
   */
  void onDebateStart(int debateId, String resourceType, List<String> contenders);

  /**
   * Called when a debate session starts, including emergency info recap for each contender.
   *
   * @param debateId unique debate id
   * @param resourceType the contested resource type
   * @param contenders list of contenders (e.g. "c(1, rescuer_1)")
   * @param emergencyInfos list of formatted emergency info strings per rescuer
   */
  default void onDebateStart(
      int debateId, String resourceType, List<String> contenders, List<String> emergencyInfos) {
    onDebateStart(debateId, resourceType, contenders);
  }

  /**
   * Called when a move is played during a debate.
   *
   * @param debateId unique debate id
   * @param moveId move id
   * @param rescuer rescuer making the move
   * @param emergency emergency id associated with the move
   * @param type move performative (claim, challenge, answer, concede)
   * @param content content of the move
   */
  void onDebateMove(
      int debateId, int moveId, String rescuer, int emergency, String type, String content);

  /**
   * Called when the status/ranking of the debate is updated.
   *
   * @param debateId unique debate id
   * @param currentWinner emergency id currently leading
   * @param reason winning reason or justification
   */
  void onDebateStatus(int debateId, int currentWinner, String reason);

  /**
   * Called when the debate concludes with a final decision.
   *
   * @param debateId unique debate id
   * @param resourceType contested resource type
   * @param winner winning emergency id
   * @param losers list of denied/conceded contenders
   * @param justification resolved effective justification
   */
  void onDebateEnd(
      int debateId, String resourceType, int winner, List<String> losers, String justification);
}
