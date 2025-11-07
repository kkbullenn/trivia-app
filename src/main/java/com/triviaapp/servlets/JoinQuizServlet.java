package com.triviaapp.servlets;

import java.io.IOException;
import java.sql.SQLException;

import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.SessionDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class JoinQuizServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // User not logged in → redirect to login page
            response.sendRedirect("login");
            return;
        }

        int user_id = (Integer) session.getAttribute("user_id");
        int lobbyId = Integer.parseInt(request.getParameter("lobby_id"));

        // Add user to the selected lobby (session) in the database
        SessionDAO sessionDAO = new SessionDAOImpl();
        try {
            sessionDAO.joinSession(lobbyId, user_id);
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        session.setAttribute("lobby_id", lobbyId);
        response.sendRedirect("/quiz");
    }
}
