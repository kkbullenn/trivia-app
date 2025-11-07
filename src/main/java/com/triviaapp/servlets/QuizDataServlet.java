package com.triviaapp.servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.triviaapp.dao.ModeratedAnswerDAO;
import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.ModeratedAnswerDAOImpl;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import com.triviaapp.dao.impl.SessionDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class QuizDataServlet extends HttpServlet {
    private final SessionDAO sessionDAO = new SessionDAOImpl();
    private final QuestionDAO questionDAO = new QuestionDAOImpl();  

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        String ajaxHeader = request.getHeader("AJAX-Requested-With");
        HttpSession session = request.getSession(false);

        // Only allow AJAX/fetch requests
        if (ajaxHeader == null || !ajaxHeader.equals("fetch")) {
            response.sendRedirect("/quiz");
            return;
        }

        // User not logged in → redirect to login page
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("/login");
            return;
        }

        // User not in a lobby → redirect to lobby selection page
        Integer lobbyId = (Integer) session.getAttribute("lobby_id");
        if (lobbyId == null) {
            response.sendRedirect("/category-lobbies");
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");

        try {
            List<Map<String, String>> participants = sessionDAO.findParticipantsBySession(lobbyId);
            boolean userInSession = participants.stream()
                .anyMatch(p -> Integer.parseInt(p.get("participant_id")) == userId);

            if (!userInSession) {
                // User doesn’t belong to this session → redirect to main page
                response.sendRedirect("/main");
                return;
            }

            // Find the current index and get question data
            int currentIndex = sessionDAO.getCurrentIndex(lobbyId);
            List<Integer> questionIds = sessionDAO.findQuestionIdsForSession(lobbyId);

            if (currentIndex < 0 || currentIndex >= questionIds.size()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid question index");
                return;
            }

            int questionId = questionIds.get(currentIndex);
            Map<String, String> questionData = questionDAO.findQuestionById(questionId);
            if (questionData == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Question not found");
                return;
            }

            // Build JSON response for question
            JSONObject question = new JSONObject();
            question.put("index", currentIndex);
            question.put("question_id", questionData.get("question_id"));
            question.put("category_id", questionData.get("category_id"));
            question.put("question_text", questionData.get("question_text"));
            question.put("answer_option", questionData.get("answer_option"));
            question.put("answer_key", questionData.get("answer_key"));
            question.put("points", questionData.get("points"));
            question.put("youtube_url", questionData.get("youtube_url"));
            
            response.setContentType("application/json");
            response.getWriter().write(question.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            // Redirect to a general error page or home
            response.sendRedirect("/error");
        }
    }
}
