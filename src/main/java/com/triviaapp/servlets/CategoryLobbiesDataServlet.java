package com.triviaapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.SessionDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CategoryLobbiesDataServlet extends HttpServlet {
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Check if user is logged in
        HttpSession session = request.getSession(false);
        String ajaxHeader = request.getHeader("AJAX-Requested-With");

        if (session == null || session.getAttribute("user_id") == null) {
            // Not logged in -> redirect to login page
            response.sendRedirect("login");
            return;
        } else if (!"fetch".equals(ajaxHeader)) {
            // Not an AJAX fetch request → redirect to main page
            response.sendRedirect("main");
            return;
        }

        Integer categoryId = (Integer) session.getAttribute("category_id");
        if (categoryId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No category selected");
            return;
        }

        // Create Session DAO object to grab available sessions from database for this category
        SessionDAO sessionDAO = new SessionDAOImpl();
        List<Map<String, String>> sessions;
        try {
            sessions = sessionDAO.listActiveSessionsSummary(categoryId);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        // Build JSON array for sessions using org.json
        JSONArray sessionsArray = new JSONArray();
        for (Map<String, String> sessionData : sessions) {
            JSONObject sessionJson = new JSONObject();
            sessionJson.put("lobby_id", sessionData.get("session_id"));
            sessionJson.put("lobby_name", sessionData.get("session_name"));
            sessionJson.put("host_user_id", sessionData.get("host_user_id"));
            sessionJson.put("status", sessionData.get("status"));
            sessionJson.put("current_participants", sessionData.getOrDefault("current_participants", "0"));
            sessionJson.put("max_participants", sessionData.getOrDefault("max_participants", ""));
            sessionsArray.put(sessionJson);
        }

        // Send JSON response
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(sessionsArray);
        out.flush();
    }
}
