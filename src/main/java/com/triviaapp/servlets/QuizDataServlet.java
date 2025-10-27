import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.SessionDAOImpl;
import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import com.triviaapp.dao.ModeratedAnswerDAO;
import com.triviaapp.dao.impl.ModeratedAnswerDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class QuizDataServlet extends HttpServlet {
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Check if user is logged in and has joined a lobby
        HttpSession session = request.getSession(false);
        String ajaxHeader = request.getHeader("AJAX-Requested-With");
        if (session == null || session.getAttribute("user_id") == null) {
            // Not logged in, redirect to login page
            response.sendRedirect("login");
            return;
        } else if (session.getAttribute("lobby_id") == null) {
            // Not in a lobby, redirect to category lobbies page
            response.sendRedirect("category-lobbies");
            return;
        } else if (ajaxHeader == null || !ajaxHeader.equals("fetch")) {
            response.sendRedirect("/quiz");
            return;
        }

        int lobbyId = (Integer) session.getAttribute("lobby_id");

        // Create DAOs to fetch questions and answers
        QuestionDAO questionDAO = new QuestionDAOImpl();
        ModeratedAnswerDAO answerDAO = new ModeratedAnswerDAOImpl();

        List<Map<String, Object>> questions;
        try {
            questions = questionDAO.getQuestionsBySessionId(lobbyId);
            // For each question, fetch its moderated answers
            for (Map<String, Object> question : questions) {
                String questionId = (String) question.get("question_id");
                List<Map<String, Object>> answers = answerDAO.getModeratedAnswersByQuestionId(questionId);
                question.put("answers", answers);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        // Build JSON array for questions and their answers
        JSONArray questionsArray = new JSONArray();
        for (Map<String, Object> questionData : questions) {
            JSONObject questionJson = new JSONObject();
            questionJson.put("question_id", questionData.get("question_id"));
            questionJson.put("question_text", questionData.get("question_text"));
            questionJson.put("point_value", questionData.get("point_value"));

            JSONArray answersArray = new JSONArray();
            List<Map<String, Object>> answers = (List<Map<String, Object>>) questionData.get("answers");
            for (Map<String, Object> answerData : answers) {
            }
        }
    }
}
