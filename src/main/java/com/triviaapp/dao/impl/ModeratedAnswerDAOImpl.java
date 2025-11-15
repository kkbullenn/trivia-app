package com.triviaapp.dao.impl;

import com.triviaapp.dao.ModeratedAnswerDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModeratedAnswerDAOImpl implements ModeratedAnswerDAO {

    private static final String SQL_INSERT = "INSERT INTO moderated_answers (session_id, question_id, participant_id, selected_answer, is_correct, score) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_BY_SESSION = "SELECT * FROM moderated_answers WHERE session_id = ? ORDER BY created_at ASC";
    private static final String SQL_SELECT_BY_PARTICIPANT = "SELECT * FROM moderated_answers WHERE participant_id = ? AND session_id = ? ORDER BY created_at DESC";
    private static final String SQL_SELECT_BY_PARTICIPANT_AND_QUESTION = String.join(" ",
            "SELECT selected_answer, is_correct, score, created_at",
            "FROM moderated_answers",
            "WHERE session_id = ? AND question_id = ? AND participant_id = ?",
            "ORDER BY created_at DESC",
            "LIMIT 1");
    private static final String SQL_COUNT_PARTICIPANT_SESSION =
            "SELECT COUNT(*) AS answer_count FROM moderated_answers WHERE session_id = ? AND participant_id = ?";
    private static final String SQL_SESSION_LEADERBOARD = String.join("\n",
        "SELECT t.participant_id, u.username, t.total_score, RANK() OVER (ORDER BY t.total_score DESC) AS rank_pos",
        "FROM (",
        "  SELECT participant_id, SUM(score) AS total_score",
        "  FROM moderated_answers",
        "  WHERE session_id = ?",
        "  GROUP BY participant_id",
        ") t",
        "JOIN users u ON t.participant_id = u.user_id",
        "ORDER BY t.total_score DESC");

    @Override
    public boolean createModeratedAnswer(int sessionId, int questionId, int participantId, String selectedAnswer, boolean isCorrect, int score) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, questionId);
            ps.setInt(3, participantId);
            ps.setString(4, selectedAnswer);
            ps.setBoolean(5, isCorrect);
            ps.setInt(6, score);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public List<Map<String, String>> findAnswersBySession(int sessionId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_SESSION)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("answer_id", String.valueOf(rs.getInt("answer_id")));
                    row.put("session_id", String.valueOf(rs.getInt("session_id")));
                    row.put("question_id", String.valueOf(rs.getInt("question_id")));
                    row.put("participant_id", String.valueOf(rs.getInt("participant_id")));
                    row.put("selected_answer", rs.getString("selected_answer"));
                    row.put("is_correct", String.valueOf(rs.getBoolean("is_correct")));
                    row.put("score", String.valueOf(rs.getInt("score")));
                    row.put("created_at", rs.getString("created_at"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    @Override
    public List<Map<String, String>> findAnswersByParticipant(int participantId, int sessionId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_PARTICIPANT)) {
            ps.setInt(1, participantId);
            ps.setInt(2, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("answer_id", String.valueOf(rs.getInt("answer_id")));
                    row.put("session_id", String.valueOf(rs.getInt("session_id")));
                    row.put("question_id", String.valueOf(rs.getInt("question_id")));
                    row.put("participant_id", String.valueOf(rs.getInt("participant_id")));
                    row.put("selected_answer", rs.getString("selected_answer"));
                    row.put("is_correct", String.valueOf(rs.getBoolean("is_correct")));
                    row.put("score", String.valueOf(rs.getInt("score")));
                    row.put("created_at", rs.getString("created_at"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    @Override
    public List<Map<String, String>> getSessionLeaderboard(int sessionId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SESSION_LEADERBOARD)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("participant_id", String.valueOf(rs.getInt("participant_id")));
                    row.put("username", rs.getString("username"));
                    row.put("total_score", String.valueOf(rs.getInt("total_score")));
                    row.put("rank_pos", String.valueOf(rs.getInt("rank_pos")));
                    out.add(row);
                }
            }
        }
        return out;
    }

    @Override
    public boolean isAnswerCorrect(int questionId, String selectedAnswer) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT answers_key FROM questions WHERE question_id = ?")) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String correctAnswer = rs.getString("answers_key");
                    if (correctAnswer == null || selectedAnswer == null) {
                        return false;
                    }
                    return correctAnswer.trim().equalsIgnoreCase(selectedAnswer.trim());
                }
            }
        }
        return false;
    }

    @Override
    public Map<String, String> findAnswerForParticipantAndQuestion(int sessionId, int questionId, int participantId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_PARTICIPANT_AND_QUESTION)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, questionId);
            ps.setInt(3, participantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("selected_answer", rs.getString("selected_answer"));
                    row.put("is_correct", String.valueOf(rs.getBoolean("is_correct")));
                    row.put("score", String.valueOf(rs.getInt("score")));
                    row.put("created_at", rs.getString("created_at"));
                    return row;
                }
            }
        }
        return null;
    }

    @Override
    public int countAnswersForParticipantInSession(int sessionId, int participantId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_PARTICIPANT_SESSION)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, participantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("answer_count");
                }
            }
        }
        return 0;
    }
}
