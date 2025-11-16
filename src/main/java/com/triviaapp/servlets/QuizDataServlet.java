package com.triviaapp.servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import com.triviaapp.dao.impl.SessionDAOImpl;
import com.triviaapp.dao.impl.CategoryDAOImpl;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.dao.impl.ModeratedAnswerDAOImpl;
import com.triviaapp.dao.CategoryDAO;   
import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.ModeratedAnswerDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Provides quiz content and session status data to participants during gameplay.
 *
 * @author Brownie Tran
 * @author Jerry Xing
 */
public class QuizDataServlet extends HttpServlet {
    private final SessionDAO sessionDAO = new SessionDAOImpl();
    private final QuestionDAO questionDAO = new QuestionDAOImpl();
    private final CategoryDAO categoryDAO = new CategoryDAOImpl();
    private final UserDAO userDAO = new UserDAOImpl();
    private final ModeratedAnswerDAO moderatedAnswerDAO = new ModeratedAnswerDAOImpl();

    private int parseScore(String value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("/login");
            return;
        }

        Integer lobbyId = (Integer) session.getAttribute("lobby_id");
        if (lobbyId == null) {
            response.sendRedirect("/category-lobbies");
            return;
        }

        try {
            List<Integer> questionIds = sessionDAO.findQuestionIdsForSession(lobbyId);
            if (questionIds == null || questionIds.isEmpty()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No questions configured for this lobby");
                return;
            }

            // Get current index from sessionDAO, initialize to zero when absent
            Integer currentIndexObj = sessionDAO.getCurrentIndex(lobbyId);
            int currentIndex;
            if (currentIndexObj == null) {
                currentIndex = 0;
                boolean updated = sessionDAO.updateCurrentIndex(lobbyId, currentIndex);
                if (!updated) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to initialize current question index");
                    return;
                }
            } else {
                currentIndex = currentIndexObj;
            }

            if (currentIndex < 0 || currentIndex >= questionIds.size()) {
                int clampedIndex = currentIndex;
                if (clampedIndex < 0) {
                    clampedIndex = 0;
                } else {
                    clampedIndex = Math.min(clampedIndex, questionIds.size() - 1);
                }

                if (!sessionDAO.updateCurrentIndex(lobbyId, clampedIndex)) {
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to reconcile question index");
                    return;
                }

                currentIndex = clampedIndex;
            }

            int questionId = questionIds.get(currentIndex);
            Map<String, String> questionData = questionDAO.findQuestionById(questionId);

            if (questionData == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Question not found");
                return;
            }

            int totalQuestions = questionIds.size();
            String categoryName = categoryDAO.findCategoryNameById(
                Integer.parseInt(questionData.get("category_id")));

            JSONObject question = new JSONObject();
            question.put("question_number", currentIndex);
            question.put("total_questions", totalQuestions);
            question.put("category_name", categoryName);
            question.put("question_text", questionData.get("question_text"));
            question.put("answers_option", questionData.get("answers_option"));
            question.put("answers_key", questionData.get("answers_key"));
            question.put("points", questionData.get("points"));
            question.put("youtube_url", questionData.get("youtube_url"));

            Integer userId = (Integer) session.getAttribute("user_id");
            question.put("user_id", userId);
            question.put("username", userId != null ? userDAO.findUsernameById(userId) : null);
            question.put("lobby_id", lobbyId);
            if (userId != null) {
                Map<String, String> existingAnswer = moderatedAnswerDAO.findAnswerForParticipantAndQuestion(lobbyId, questionId, userId);
                if (existingAnswer != null) {
                    question.put("selected_answer", existingAnswer.get("selected_answer"));
                    question.put("is_correct", existingAnswer.get("is_correct"));
                    question.put("score_awarded", existingAnswer.get("score"));
                }
                int answeredCount = moderatedAnswerDAO.countAnswersForParticipantInSession(lobbyId, userId);
                question.put("answered_count", answeredCount);

                if (answeredCount >= totalQuestions) {
                    List<Map<String, String>> leaderboard = moderatedAnswerDAO.getSessionLeaderboard(lobbyId);
                    int totalScore = 0;
                    if (leaderboard != null) {
                        for (Map<String, String> entry : leaderboard) {
                            if (entry == null) {
                                continue;
                            }
                            String participantIdStr = entry.get("participant_id");
                            if (participantIdStr == null) {
                                continue;
                            }
                            try {
                                if (Integer.parseInt(participantIdStr) == userId) {
                                    String scoreStr = entry.get("total_score");
                                    if (scoreStr != null) {
                                        try {
                                            totalScore = Integer.parseInt(scoreStr.trim());
                                        } catch (NumberFormatException ignored) {
                                            totalScore = 0;
                                        }
                                    }
                                    break;
                                }
                            } catch (NumberFormatException ignored) {
                                // skip malformed rows
                            }
                        }
                    }

                    int totalPossibleScore = 0;
                    for (Integer id : questionIds) {
                        if (id == null) {
                            continue;
                        }
                        Map<String, String> qRow = questionDAO.findQuestionById(id);
                        if (qRow == null) {
                            continue;
                        }
                        totalPossibleScore += parseScore(qRow.get("points"));
                    }

                    JSONObject completion = new JSONObject();
                    completion.put("type", "quizComplete");
                    completion.put("lobby_id", lobbyId);
                    completion.put("total_questions", totalQuestions);
                    completion.put("answered_count", answeredCount);
                    completion.put("total_score", totalScore);
                    completion.put("total_max_score", totalPossibleScore);
                    completion.put("category_name", categoryName);
                    completion.put("username", question.get("username"));

                    if (leaderboard != null) {
                        JSONArray leaderboardArray = new JSONArray();
                        for (Map<String, String> entry : leaderboard) {
                            if (entry == null) {
                                continue;
                            }
                            JSONObject entryJson = new JSONObject();
                            for (Map.Entry<String, String> kv : entry.entrySet()) {
                                entryJson.put(kv.getKey(), kv.getValue() == null ? JSONObject.NULL : kv.getValue());
                            }
                            leaderboardArray.put(entryJson);
                        }
                        completion.put("leaderboard", leaderboardArray);
                    }

                    response.setContentType("application/json");
                    response.getWriter().write(completion.toString());
                    return;
                }
            }

            response.setContentType("application/json");
            response.getWriter().write(question.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("/error");
        }
    }
}