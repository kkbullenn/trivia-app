package com.triviaapp.servlets;

import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONObject;

import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.SessionDAOImpl;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class QuizControlServlet extends HttpServlet {
    private final SessionDAO sessionDAO = new SessionDAOImpl();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String ajaxHeader = request.getHeader("AJAX-Requested-With");
        HttpSession session = request.getSession(false);

        // Only allow AJAX/fetch requests from admin users
        if (ajaxHeader == null || !ajaxHeader.equals("fetch")) {
            response.sendRedirect("/quiz");
            return;
        } else if (session == null || session.getAttribute("user_role") == null ||
            !"admin".equals(session.getAttribute("user_role"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Admin only");
            return;
        }

        // Read the action from query parameter instead of path
        String action = request.getParameter("action"); // expects "next" or "prev"
        if (action == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing action parameter");
            return;
        }

        Integer lobbyId = (Integer) session.getAttribute("lobby_id");
        if (lobbyId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Lobby not found in session");
            return;
        }

        try {
            int newIndex;
            switch (action) {
                case "next":
                    newIndex = sessionDAO.incrementAndGetCurrentIndex(lobbyId);
                    break;
                case "prev":
                    newIndex = sessionDAO.decrementAndGetCurrentIndex(lobbyId);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid action value");
                    return;
            }

            // Return JSON with the new current index
            JSONObject json = new JSONObject();
            json.put("currentIndex", newIndex);
            response.setContentType("application/json");
            response.getWriter().write(json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("lobby_id") == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Lobby not found in session");
            return;
        }

        int lobbyId = (Integer) session.getAttribute("lobby_id");
        try {
            int currentIndex = sessionDAO.getCurrentIndex(lobbyId);
            JSONObject json = new JSONObject();
            json.put("currentIndex", currentIndex);
            response.setContentType("application/json");
            response.getWriter().write(json.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }
}
