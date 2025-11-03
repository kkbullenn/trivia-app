package com.triviaapp.dao;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Data access for the `sessions` and `session_participants` tables.
 *
 * Provides CRUD and query operations:
 * - fetch a single session by id
 * - fetch sessions by host
 * - list active sessions summary with current participant counts
 * - list participants for a session
 * - join and leave participant operations
 *
 */
public interface SessionDAO {

    /**
     * Return a single session row as a map or null when not found.
     */
    Map<String, String> findSessionById(int sessionId) throws SQLException;

    /**
     * Return sessions created by the given host, ordered by start_at desc.
     */
    List<Map<String, String>> findSessionsByHost(int hostUserId) throws SQLException;

    /**
     * Create a session row.
     * Returns true when insertion succeeded.
     */
    boolean createSession(int hostUserId,
                          String sessionName,
                          Integer categoryId,
                          Integer maxParticipants,
                          String status,
                          Timestamp startAt,
                          Timestamp endAt) throws SQLException;

    /**
     * Update the session status. Returns true if a row was updated.
     */
    boolean updateSessionStatus(int sessionId, String status) throws SQLException;

    /**
     * Delete a session by id. Returns true if a row was deleted.
     */
    boolean deleteSession(int sessionId) throws SQLException;

    /**
     * Set status='completed' and end_at = CURRENT_TIMESTAMP when session is ended.
     * Returns true if the session was transitioned.
     */
    boolean endSessionNow(int sessionId) throws SQLException;

    /**
     * Return a summary list for active sessions. Each map contains:
     * session_id, session_name, host_user_id, max_participants, status, current_participants
     */
    List<Map<String, String>> listActiveSessionsSummary() throws SQLException;

    /**
     * Return participants for a session. Each map contains:
     * participant_id, username, joined_at, left_at, status
     */
    List<Map<String, String>> findParticipantsBySession(int sessionId) throws SQLException;

    /**
     * Insert or refresh a participant row marking the user as joined.
     * Returns true when the insert/update affected rows.
     */
    boolean joinSession(int sessionId, int participantId) throws SQLException;

    /**
     * Mark a participant as left (sets left_at and status='left').
     * Returns true when a joined row was updated.
     */
    boolean leaveSession(int sessionId, int participantId) throws SQLException;

    /**
     * Insert all question IDs from a category into session_questions for the given session.
     * This operation is idempotent from the caller's perspective (DAO may delete existing rows first).
     */
    boolean insertAllQuestionsForSession(int sessionId, int categoryId) throws SQLException;

    /**
     * Return the list of question IDs bound to the session (ordered by question_id by default).
     */
    List<Integer> findQuestionIdsForSession(int sessionId) throws SQLException;

    /**
     * Read the authoritative current_index for the session (used for multiplayer host-driven sync).
     */
    Integer getCurrentIndex(int sessionId) throws SQLException;

    /**
     * Atomically increment current_index and return the new value.
     */
    Integer incrementAndGetCurrentIndex(int sessionId) throws SQLException;

    /**
     * Atomically decrement current_index and return the new value. Returns null if session not found
     * or cannot be decremented (e.g. already at 0).
     */
    Integer decrementAndGetCurrentIndex(int sessionId) throws SQLException;
}
