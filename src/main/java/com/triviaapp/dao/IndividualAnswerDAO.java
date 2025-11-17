package com.triviaapp.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Data access for the `individual_answers` table.
 *
 * Provides CRUD and query operations:
 * - insert an individual answer
 * - fetch answers by user
 * - compute total score of a user in a category（individual mode）
 *
 * @author Haven Zhang
 */
public interface IndividualAnswerDAO {

    /**
     * Insert an individual answer record. Returns true when insert succeeded.
     */
    boolean createAnswer(int questionId, int userId, String selectedAnswer, boolean isCorrect, int score) throws SQLException;

    /**
     * Return all answers by a user as a list of maps. Ordered by created_at desc.
     */
    List<Map<String, String>> findAnswersByUser(int userId) throws SQLException;

    /**
     * Return total score for individual mode for the given user in the given category; returns 0 when none.
     */
    int getTotalScoreForIndividualMode(int userId, int categoryId) throws SQLException;
}
