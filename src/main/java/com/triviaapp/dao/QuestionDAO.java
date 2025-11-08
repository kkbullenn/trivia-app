package com.triviaapp.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Data access for the `questions` table.
 *
 * Provides CRUD and query operations:
 * - fetch a single question by id
 * - fetch all questions for a category
 * - fetch an ordered list of question ids for navigation
 * - create, update and delete questions
 *
 */
public interface QuestionDAO {

    /**
     * Return a single question as a map of column->string, or null if not found.
     */
    Map<String, String> findQuestionById(int questionId) throws SQLException;

    /**
     * Return questions for the specified category.
     */
    List<Map<String, String>> findQuestionsByCategory(int categoryId) throws SQLException;

    /**
     * Return an ordered list of question IDs for the specified category.
     * Intended to store the IDs in the Session and use {@link #findQuestionById(int)}
     * to implement next/prev navigation.
     */
    List<Integer> findQuestionIdsByCategory(int categoryId) throws SQLException;

    /** 
     * Insert a new question row; returns true on success. 
     */
    boolean createQuestion(int categoryId,
                           String xmlQuestion,
                           String youtubeUrl,
                           String questionText,
                           String answersOptionJson,
                           String answersKey,
                           int points,
                           int uploadedBy) throws SQLException;

    /** 
     * Update an existing question; returns true if a row was updated. 
     */
    boolean updateQuestion(int questionId,
                           int categoryId,
                           String xmlQuestion,
                           String youtubeUrl,
                           String questionText,
                           String answersOptionJson,
                           String answersKey,
                           int points) throws SQLException;

    /**
     * Delete the question by id; returns true if deleted. 
     */
    boolean deleteQuestion(int questionId) throws SQLException;
}
