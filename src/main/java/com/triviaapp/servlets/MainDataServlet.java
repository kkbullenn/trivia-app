package com.triviaapp.servlets;

import com.triviaapp.dao.CategoryDAO;
import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.CategoryDAOImpl;
import com.triviaapp.dao.impl.SessionDAOImpl;
import com.triviaapp.util.SessionUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides category and quiz data for the main dashboard after verifying login status.
 *
 * @author Brownie Tran
 * @author Jerry Xing
 */
public class MainDataServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(MainDataServlet.class.getName());

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException
    {

        // Check if user is logged in
        HttpSession session = SessionUtils.requireSession(request, response);
        if(session == null)
        {
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");

        // DAO objects for categories and quizzes
        CategoryDAO categoryDAO = new CategoryDAOImpl();
        SessionDAO sessionDAO = new SessionDAOImpl();
        Map<Integer, String> categories;
        List<Map<String, String>> quizzes;
        try
        {
            categories = categoryDAO.findAllCategories();
            quizzes = sessionDAO.findSessionsByHost(userId);
        } catch(SQLException e)
        {
            LOGGER.log(Level.SEVERE, "Failed to fetch dashboard data for user " + userId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        // Build JSON array for categories using org.json
        JSONArray categoriesArray = new JSONArray();
        categories.forEach((id, name) ->
        {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("name", name);
            categoriesArray.put(obj);
        });

        // Build JSON array for quizzes created by this host
        JSONArray quizzesArray = new JSONArray();
        for(Map<String, String> quiz : quizzes)
        {
            JSONObject obj = new JSONObject();
            obj.put("session_id", quiz.get("session_id"));
            obj.put("quiz_name", quiz.get("session_name"));
            obj.put("status", quiz.get("status"));
            obj.put("max_participants", quiz.get("max_participants"));
            obj.put("start_at", quiz.get("start_at"));
            obj.put("end_at", quiz.get("end_at"));
            quizzesArray.put(obj);
        }

        // Return JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        JSONObject payload = new JSONObject();
        payload.put("categories", categoriesArray);
        payload.put("quizzes", quizzesArray);
        out.write(payload.toString());
        out.flush();
    }
}
