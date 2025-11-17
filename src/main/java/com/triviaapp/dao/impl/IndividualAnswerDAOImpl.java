package com.triviaapp.dao.impl;

import com.triviaapp.dao.IndividualAnswerDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements persistence operations for individual quiz answers and scoring.
 *
 * @author Haven Zhang
 */
public class IndividualAnswerDAOImpl implements IndividualAnswerDAO {

    private static final String SQL_INSERT = "INSERT INTO individual_answers (question_id, user_id, selected_answer, is_correct, score) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_BY_USER = "SELECT * FROM individual_answers WHERE user_id = ? ORDER BY created_at DESC";
    private static final String SQL_TOTAL_SCORE_FOR_INDIVIDUAL_MODE = "SELECT COALESCE(SUM(score),0) AS total_score FROM individual_answers ia JOIN questions q ON ia.question_id = q.question_id WHERE ia.user_id = ? AND q.category_id = ?";

    @Override
    public boolean createAnswer(int questionId, int userId, String selectedAnswer, boolean isCorrect, int score) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, questionId);
            ps.setInt(2, userId);
            ps.setString(3, selectedAnswer);
            ps.setBoolean(4, isCorrect);
            ps.setInt(5, score);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public List<Map<String, String>> findAnswersByUser(int userId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_USER)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("answer_id", String.valueOf(rs.getInt("answer_id")));
                    row.put("question_id", String.valueOf(rs.getInt("question_id")));
                    row.put("user_id", String.valueOf(rs.getInt("user_id")));
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
    public int getTotalScoreForIndividualMode(int userId, int categoryId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TOTAL_SCORE_FOR_INDIVIDUAL_MODE)) {
            ps.setInt(1, userId);
            ps.setInt(2, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_score");
                }
            }
        }
        return 0;
    }
}
