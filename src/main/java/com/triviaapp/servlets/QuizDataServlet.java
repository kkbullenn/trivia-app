package com.triviaapp.servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import com.triviaapp.dao.impl.SessionDAOImpl;
import com.triviaapp.dao.impl.CategoryDAOImpl;
import com.triviaapp.dao.CategoryDAO;   

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class QuizDataServlet extends HttpServlet {
    private final SessionDAO sessionDAO = new SessionDAOImpl();
    private final QuestionDAO questionDAO = new QuestionDAOImpl();
    private final CategoryDAO categoryDAO = new CategoryDAOImpl();

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
            // Get current index from sessionDAO
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

            JSONObject question = new JSONObject();
            question.put("question_number", currentIndex);
            question.put("category_name", categoryDAO.findCategoryNameById(
                Integer.parseInt(questionData.get("category_id"))));
            question.put("question_text", questionData.get("question_text"));
            question.put("answer_option", questionData.get("answer_option"));
            question.put("answer_key", questionData.get("answer_key"));
            question.put("points", questionData.get("points"));
            question.put("youtube_url", questionData.get("youtube_url"));

            response.setContentType("application/json");
            response.getWriter().write(question.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("/error");
        }
    }
}