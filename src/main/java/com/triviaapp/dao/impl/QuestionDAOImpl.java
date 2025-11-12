package com.triviaapp.dao.impl;

import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAOImpl implements QuestionDAO {

    private static final String SQL_FIND_QUESTION_BY_ID = "SELECT * FROM questions WHERE question_id = ?";
    private static final String SQL_LIST_QUESTIONS_BY_CATEGORY = "SELECT * FROM questions WHERE category_id = ? ORDER BY question_id ASC";
    private static final String SQL_SELECT_IDS_BY_CATEGORY = "SELECT question_id FROM questions WHERE category_id = ? ORDER BY question_id ASC";
    private static final String SQL_INSERT = "INSERT INTO questions (category_id, xml_question, youtube_url, question_text, answers_option, answers_key, points, uploaded_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_UPDATE = "UPDATE questions SET category_id = ?, xml_question = ?, youtube_url = ?, question_text = ?, answers_option = ?, answers_key = ?, points = ? WHERE question_id = ?";
    private static final String SQL_DELETE = "DELETE FROM questions WHERE question_id = ?";

    @Override
    public Map<String, String> findQuestionById(int questionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_QUESTION_BY_ID)) {
            ps.setInt(1, questionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> out = new LinkedHashMap<>();
                    out.put("question_id", String.valueOf(rs.getInt("question_id")));
                    out.put("category_id", String.valueOf(rs.getInt("category_id")));
                    out.put("xml_question", rs.getString("xml_question"));
                    out.put("youtube_url", rs.getString("youtube_url"));
                    out.put("question_text", rs.getString("question_text"));
                    out.put("answers_option", rs.getString("answers_option"));
                    out.put("answers_key", rs.getString("answers_key"));
                    out.put("points", String.valueOf(rs.getInt("points")));
                    out.put("uploaded_by", String.valueOf(rs.getInt("uploaded_by")));
                    out.put("created_at", rs.getString("created_at"));
                    out.put("updated_at", rs.getString("updated_at"));
                    return out;
                }
            }
        }
        return null;
    }

    @Override
    public List<Map<String, String>> findQuestionsByCategory(int categoryId) throws SQLException {
        List<Map<String, String>> out = new ArrayList<>();
       try (Connection conn = DBConnectionManager.getConnection();
           PreparedStatement ps = conn.prepareStatement(SQL_LIST_QUESTIONS_BY_CATEGORY)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("question_id", String.valueOf(rs.getInt("question_id")));
                    row.put("category_id", String.valueOf(rs.getInt("category_id")));
                    row.put("xml_question", rs.getString("xml_question"));
                    row.put("youtube_url", rs.getString("youtube_url"));
                    row.put("question_text", rs.getString("question_text"));
                    row.put("answers_option", rs.getString("answers_option"));
                    row.put("answers_key", rs.getString("answers_key"));
                    row.put("points", String.valueOf(rs.getInt("points")));
                    row.put("uploaded_by", String.valueOf(rs.getInt("uploaded_by")));
                    row.put("created_at", rs.getString("created_at"));
                    row.put("updated_at", rs.getString("updated_at"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    @Override
    public List<Integer> findQuestionIdsByCategory(int categoryId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_IDS_BY_CATEGORY)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("question_id"));
                }
            }
        }
        return ids;
    }

    @Override
    public boolean createQuestion(int categoryId, String xmlQuestion, String youtubeUrl, String questionText, String answersOptionJson, String answersKey, int points, int uploadedBy) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setInt(1, categoryId);
            ps.setString(2, xmlQuestion);
            ps.setString(3, youtubeUrl);
            ps.setString(4, questionText);
            ps.setString(5, answersOptionJson);
            ps.setString(6, answersKey);
            ps.setInt(7, points);
            ps.setInt(8, uploadedBy);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean updateQuestion(int questionId, int categoryId, String xmlQuestion, String youtubeUrl, String questionText, String answersOptionJson, String answersKey, int points) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {
            ps.setInt(1, categoryId);
            ps.setString(2, xmlQuestion);
            ps.setString(3, youtubeUrl);
            ps.setString(4, questionText);
            ps.setString(5, answersOptionJson);
            ps.setString(6, answersKey);
            ps.setInt(7, points);
            ps.setInt(8, questionId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public boolean deleteQuestion(int questionId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, questionId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }
}
