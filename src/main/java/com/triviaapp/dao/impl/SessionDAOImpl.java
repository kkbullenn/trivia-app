package com.triviaapp.dao.impl;

import com.triviaapp.dao.SessionDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionDAOImpl implements SessionDAO {

    private static final String SQL_FIND_BY_ID = "SELECT * FROM sessions WHERE session_id = ?";
    private static final String SQL_LIST_BY_HOST = "SELECT * FROM sessions WHERE host_user_id = ? ORDER BY start_at DESC";
    private static final String SQL_INSERT = "INSERT INTO sessions (host_user_id, session_name, category_id, max_participants, status, start_at, end_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE_STATUS = "UPDATE sessions SET status = ?, end_at = CASE WHEN ? = 'completed' THEN CURRENT_TIMESTAMP ELSE end_at END WHERE session_id = ?";
    private static final String SQL_END_SESSION_NOW = "UPDATE sessions SET status = 'completed', end_at = CURRENT_TIMESTAMP WHERE session_id = ? AND status <> 'completed'";
    private static final String SQL_DELETE = "DELETE FROM sessions WHERE session_id = ?";
    // session_questions helper SQL
    private static final String SQL_DELETE_SESSION_QUESTIONS = "DELETE FROM session_questions WHERE session_id = ?";
    private static final String SQL_INSERT_FROM_CATEGORY = "INSERT INTO session_questions (session_id, question_id) SELECT ?, q.question_id FROM questions q WHERE q.category_id = ?";
    private static final String SQL_INSERT_SESSSION_QUESTIONS = "INSERT INTO session_questions (session_id, question_id) VALUES (?, ?)";
    private static final String SQL_SELECT_SESSION_QUESTION_IDS = "SELECT question_id FROM session_questions WHERE session_id = ? ORDER BY question_id ASC";

    // current_index management on sessions table (multiplayer host-driven sync)
    private static final String SQL_GET_CURRENT_INDEX = "SELECT current_index FROM sessions WHERE session_id = ?";
    private static final String SQL_INCREMENT_CURRENT_INDEX = "UPDATE sessions SET current_index = current_index + 1 WHERE session_id = ?";
    private static final String SQL_DECREMENT_CURRENT_INDEX = "UPDATE sessions SET current_index = current_index - 1 WHERE session_id = ? AND current_index > 0";
    
    private static final String SQL_LIST_ACTIVE_SUMMARY = String.join("\n",
            "SELECT s.session_id, s.session_name, s.host_user_id, s.max_participants, s.status, COALESCE(sp.cnt,0) AS current_participants",
            "FROM sessions s",
            "LEFT JOIN (",
            "  SELECT session_id, COUNT(*) AS cnt",
            "  FROM session_participants",
            "  WHERE status = 'joined'",
            "  GROUP BY session_id",
            ") sp ON s.session_id = sp.session_id",
            "WHERE s.status = 'active'",
            "ORDER BY current_participants DESC, s.start_at DESC");

    private static final String SQL_LIST_PARTICIPANTS_BY_SESSION = String.join("\n",
            "SELECT sp.participant_id, u.username, sp.joined_at, sp.left_at, sp.status",
            "FROM session_participants sp",
            "JOIN users u ON sp.participant_id = u.user_id",
            "WHERE sp.session_id = ?",
            "ORDER BY sp.joined_at ASC");

    // Insert or update: when duplicate PK exists update to joined state
    private static final String SQL_INSERT_OR_UPDATE_JOIN = String.join("\n",
            "INSERT INTO session_participants (session_id, participant_id, joined_at, left_at, status)",
            "VALUES (?, ?, CURRENT_TIMESTAMP, NULL, 'joined')",
            "ON DUPLICATE KEY UPDATE status = 'joined', joined_at = CURRENT_TIMESTAMP, left_at = NULL");

    private static final String SQL_MARK_LEFT =
        "UPDATE session_participants SET status = 'left', left_at = CURRENT_TIMESTAMP WHERE session_id = ? AND participant_id = ? AND status = 'joined'";

    @Override
    public Map<String, String> findSessionById(int sessionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> out = new LinkedHashMap<>();
                    out.put("session_id", String.valueOf(rs.getInt("session_id")));
                    out.put("host_user_id", String.valueOf(rs.getInt("host_user_id")));
                    out.put("session_name", rs.getString("session_name"));
                    out.put("max_participants", String.valueOf(rs.getObject("max_participants")));
                    out.put("status", rs.getString("status"));
                    out.put("start_at", rs.getString("start_at"));
                    out.put("end_at", rs.getString("end_at"));
                    return out;
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, String>> findSessionsByHost(int hostUserId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LIST_BY_HOST)) {
            ps.setInt(1, hostUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("session_id", String.valueOf(rs.getInt("session_id")));
                    row.put("host_user_id", String.valueOf(rs.getInt("host_user_id")));
                    row.put("session_name", rs.getString("session_name"));
                    row.put("max_participants", String.valueOf(rs.getObject("max_participants")));
                    row.put("status", rs.getString("status"));
                    row.put("start_at", rs.getString("start_at"));
                    row.put("end_at", rs.getString("end_at"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    @Override
    public boolean createSession(int hostUserId, String sessionName, Integer categoryId, Integer maxParticipants, String status, Timestamp startAt, Timestamp endAt) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, hostUserId);
            ps.setString(2, sessionName);
            if (categoryId != null) ps.setInt(3, categoryId); else ps.setNull(3, java.sql.Types.INTEGER);
            if (maxParticipants != null) ps.setInt(4, maxParticipants); else ps.setNull(4, java.sql.Types.INTEGER);
            ps.setString(5, status);
            // start_at column is NOT NULL with DEFAULT CURRENT_TIMESTAMP in schema.
            // If caller provides null, insert current time instead of explicit NULL to avoid constraint violation.
            if (startAt != null) {
                ps.setTimestamp(6, startAt);
            } else {
                ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            }
            if (endAt != null) ps.setTimestamp(7, endAt); else ps.setNull(7, java.sql.Types.TIMESTAMP);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean updateSessionStatus(int sessionId, String status) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUS)) {
            // status param is bound twice: once for status column, once for CASE check
            ps.setString(1, status); // bind value for `status` column
            ps.setString(2, status); // bind value for CASE WHEN ? = 'completed' check
            ps.setInt(3, sessionId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean endSessionNow(int sessionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_END_SESSION_NOW)) {
            ps.setInt(1, sessionId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean deleteSession(int sessionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, sessionId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public List<Map<String, String>> listActiveSessionsSummary() throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LIST_ACTIVE_SUMMARY);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("session_id", String.valueOf(rs.getInt("session_id")));
                row.put("session_name", rs.getString("session_name"));
                row.put("host_user_id", String.valueOf(rs.getInt("host_user_id")));
                row.put("max_participants", String.valueOf(rs.getObject("max_participants")));
                row.put("status", rs.getString("status"));
                row.put("current_participants", String.valueOf(rs.getInt("current_participants")));
                out.add(row);
            }
        }
        return out;
    }

    @Override
    public List<Map<String, String>> findParticipantsBySession(int sessionId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LIST_PARTICIPANTS_BY_SESSION)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("participant_id", String.valueOf(rs.getInt("participant_id")));
                    row.put("username", rs.getString("username"));
                    row.put("joined_at", rs.getString("joined_at"));
                    row.put("left_at", rs.getString("left_at"));
                    row.put("status", rs.getString("status"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    @Override
    public boolean joinSession(int sessionId, int participantId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_OR_UPDATE_JOIN)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, participantId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean leaveSession(int sessionId, int participantId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_MARK_LEFT)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, participantId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    // insertRandomQuestionsForSession removed — callers should use insertAllQuestionsForSession and, if random selection is desired,
    // perform random selection in application logic or add a separate utility that uses SQL with ORDER BY RAND() when appropriate.

    @Override
    public boolean insertAllQuestionsForSession(int sessionId, int categoryId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection()) {
            boolean oldAuto = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement del = conn.prepareStatement(SQL_DELETE_SESSION_QUESTIONS)) {
                    del.setInt(1, sessionId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(SQL_INSERT_FROM_CATEGORY)) {
                    ins.setInt(1, sessionId);
                    ins.setInt(2, categoryId);
                    ins.executeUpdate();
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(oldAuto);
            }
        }
    }

    @Override
    public boolean insertQuestionForSession(int sessionId, int questionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_SESSSION_QUESTIONS)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, questionId);
            ps.executeUpdate();
            return true;
        } catch (SQLException ex) {
            throw ex;
        }
    }

    @Override
    public List<Integer> findQuestionIdsForSession(int sessionId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_SESSION_QUESTION_IDS)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("question_id"));
                }
            }
        }
        return ids;
    }

    @Override
    public Integer getCurrentIndex(int sessionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_CURRENT_INDEX)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int v = rs.getInt("current_index");
                    if (rs.wasNull()) return null;
                    return v;
                }
            }
        }
        return null;
    }

    @Override
    public Integer incrementAndGetCurrentIndex(int sessionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection()) {
            boolean oldAuto = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement up = conn.prepareStatement(SQL_INCREMENT_CURRENT_INDEX)) {
                    up.setInt(1, sessionId);
                    int rows = up.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        return null;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_GET_CURRENT_INDEX)) {
                    ps.setInt(1, sessionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int v = rs.getInt("current_index");
                            conn.commit();
                            return v;
                        }
                    }
                }
                conn.rollback();
                return null;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(oldAuto);
            }
        }
    }

    @Override
    public Integer decrementAndGetCurrentIndex(int sessionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection()) {
            boolean oldAuto = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement down = conn.prepareStatement(SQL_DECREMENT_CURRENT_INDEX)) {
                    down.setInt(1, sessionId);
                    int rows = down.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        return null;
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(SQL_GET_CURRENT_INDEX)) {
                    ps.setInt(1, sessionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int v = rs.getInt("current_index");
                            conn.commit();
                            return v;
                        }
                    }
                }
                conn.rollback();
                return null;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(oldAuto);
            }
        }
    }
}
