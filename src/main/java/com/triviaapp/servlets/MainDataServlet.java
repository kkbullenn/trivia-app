package com.triviaapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.triviaapp.dao.CategoryDAO;
import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.CategoryDAOImpl;
import com.triviaapp.dao.impl.SessionDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class MainDataServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Check if user is logged in
        HttpSession session = request.getSession(false);
        String ajaxHeader = request.getHeader("AJAX-Requested-With");
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
<<<<<<< HEAD
            obj.put("session_id", Integer.parseInt(quiz.get("session_id")));
            obj.put("quiz_name", quiz.get("quiz_name"));
            obj.put("status", quiz.get("status"));
=======
            obj.put("quiz-id", quiz.get("session_id"));
            obj.put("quiz-name", quiz.get("quiz_name"));
            obj.put("quiz-category", quiz.get("status")); // Temporary since category not stored in session table, and MVP prototype doesn't need it
>>>>>>> feature/BrownieTran_CategoryLobbiesBE
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
