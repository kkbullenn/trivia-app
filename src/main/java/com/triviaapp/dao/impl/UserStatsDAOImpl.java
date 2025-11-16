package com.triviaapp.dao.impl;

import com.triviaapp.dao.UserStatsDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * JDBC implementation for aggregating multiplayer statistics of a user.
 *
 * @author Jerry Xing
 */
public class UserStatsDAOImpl implements UserStatsDAO {

    private static final String SQL_COUNT_PARTICIPATIONS =
            "SELECT COUNT(DISTINCT session_id) AS participated " +
            "FROM session_participants WHERE participant_id = ? AND status = 'left'";

    private static final String SQL_COUNT_WINS = String.join("\n",
            "SELECT COUNT(*) AS win_count",
            "FROM (",
            "  SELECT ma.session_id, ma.participant_id,",
            "         RANK() OVER (PARTITION BY ma.session_id ORDER BY SUM(ma.score) DESC) AS rank_pos",
            "  FROM moderated_answers ma",
            "  GROUP BY ma.session_id, ma.participant_id",
            ") ranked",
            "JOIN session_participants sp ON sp.session_id = ranked.session_id",
            "  AND sp.participant_id = ranked.participant_id",
            "WHERE ranked.participant_id = ?",
            "  AND ranked.rank_pos = 1",
            "  AND sp.status = 'left'");

    private static final String SQL_TOP_CATEGORY = String.join("\n",
            "SELECT s.category_id, c.name AS category_name, SUM(ma.score) AS total_score",
            "FROM moderated_answers ma",
            "JOIN sessions s ON ma.session_id = s.session_id",
            "JOIN categories c ON s.category_id = c.category_id",
            "WHERE ma.participant_id = ?",
            "GROUP BY s.category_id, c.name",
            "ORDER BY total_score DESC",
            "LIMIT 1");

    @Override
    public int countSessionsParticipated(int userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_PARTICIPATIONS)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("participated");
                }
            }
        }
        return 0;
    }

    @Override
    public int countWins(int userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_WINS)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("win_count");
                }
            }
        }
        return 0;
    }

    @Override
    public Map<String, String> findTopCategoryByScore(int userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_TOP_CATEGORY)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> result = new HashMap<>();
                    result.put("category_id", String.valueOf(rs.getInt("category_id")));
                    result.put("category_name", rs.getString("category_name"));
                    result.put("total_score", String.valueOf(rs.getInt("total_score")));
                    return result;
                }
            }
        }
        return null;
    }
}
