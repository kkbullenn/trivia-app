package com.triviaapp.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Data access for the `moderated_answers` table.
 *
 * @author Haven Zhang
 * @author Brownie Tran
 * @author Jerry Xing
 */
public interface ModeratedAnswerDAO {

    /**
     * Insert a moderated answer record.
     */
    boolean createModeratedAnswer(int sessionId, int questionId, int participantId, String selectedAnswer, boolean isCorrect, int score) throws SQLException;

    /**
     * Return all moderated answers for a session ordered by created_at asc.
     */
    List<Map<String, String>> findAnswersBySession(int sessionId) throws SQLException;

    /**
     * Return moderated answers by a participant within a session ordered by created_at desc.
     */
    List<Map<String, String>> findAnswersByParticipant(int participantId, int sessionId) throws SQLException;

    /**
     * Leaderboard for a session: participants ranked by total score.
     * Each map contains: participant_id, username, total_score, rank_pos
     */
    List<Map<String, String>> getSessionLeaderboard(int sessionId) throws SQLException;

    /**
     * Check if the selected answer is correct for the given question.
     */
    boolean isAnswerCorrect(int questionId, String selectedAnswer) throws SQLException;

    /**
     * Retrieve an answer the participant already submitted for the given question within the session.
     */
    Map<String, String> findAnswerForParticipantAndQuestion(int sessionId, int questionId, int participantId) throws SQLException;

    /**
     * Count how many answers the participant has submitted within the session.
     */
    int countAnswersForParticipantInSession(int sessionId, int participantId) throws SQLException;
}
