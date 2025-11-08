package com.triviaapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.triviaapp.dao.CategoryDAO;
import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.CategoryDAOImpl;
import com.triviaapp.dao.impl.SessionDAOImpl;

public class AdminDataServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // Not logged in, redirect to  login page
            response.sendRedirect("login");
            return;
        }
        int userId = (Integer) session.getAttribute("user_id");

        // Create DAO object to grab available categories from database
        CategoryDAO categoryDAO = new CategoryDAOImpl();
        Map<Integer, String> categories;
        try {
            categories = categoryDAO.findAllCategories();
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        // Create Session DAO object to grab available sessions from database
        SessionDAO sessionDAO = new SessionDAOImpl();
        List<Map<String, String>> sessions;
        try {
            sessions = sessionDAO.findSessionsByHost(userId);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        // Build JSON array for categories using org.json
        JSONArray categoriesArray = new JSONArray();
        for (Map.Entry<Integer, String> entry : categories.entrySet()) {
            JSONObject obj = new JSONObject();
            obj.put("category-id", entry.getKey());
            obj.put("category-name", entry.getValue());
            categoriesArray.put(obj);
        }

        // Build JSON array for sessions (Quiz) using org.json
        JSONArray quizzesArray = new JSONArray();
        for (Map<String, String> quiz : sessions) {
            JSONObject obj = new JSONObject();
            obj.put("quiz-id", Integer.parseInt(quiz.get("session_id")));
            obj.put("quiz-name", quiz.get("quiz_name"));
            obj.put("quiz-status", quiz.get("status")); // Temporary since category not stored in session table, and MVP prototype doesn't need it
            quizzesArray.put(obj);
        }

        // wrap both arrays in a single JSON object
        JSONObject responseJson = new JSONObject();
        responseJson.put("categories", categoriesArray);
        responseJson.put("quizzes", quizzesArray);
        
        // Return JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(responseJson.toString());
        out.flush();
    }
}
