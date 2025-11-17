package com.triviaapp.dao;

import java.sql.SQLException;
import java.util.Map;

/**
 * Aggregated statistics for a player's multiplayer performance.
 *
 * @author Jerry Xing
 */
public interface UserStatsDAO {

    /**
     * Count distinct multiplayer sessions the user has participated in.
     */
    int countSessionsParticipated(int userId) throws SQLException;

    /**
     * Count how many multiplayer sessions the user has won (ranked first by total score).
     */
    int countWins(int userId) throws SQLException;

    /**
     * Returns the category in which the user earned the highest total score. The map contains keys: category_id,
     * category_name, total_score. Returns {@code null} when no scored categories exist for the user.
     */
    Map<String, String> findTopCategoryByScore(int userId) throws SQLException;
}
