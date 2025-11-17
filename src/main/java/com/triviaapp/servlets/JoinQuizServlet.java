package com.triviaapp.servlets;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.ModeratedAnswerDAO;
import com.triviaapp.dao.impl.SessionDAOImpl;
import com.triviaapp.dao.impl.ModeratedAnswerDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Handles lobby join requests and prepares the session for quiz participation.
 *
 * @author Brownie Tran
 * @author Jerry Xing
 */
public class JoinQuizServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(JoinQuizServlet.class.getName());

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException
    {

        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("user_id") == null)
        {
            // User not logged in → redirect to login page
            response.sendRedirect("login");
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");
        String lobbyIdParam = request.getParameter("lobby_id");
        if(lobbyIdParam == null || lobbyIdParam.isBlank())
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing lobby_id");
            return;
        }

        int lobbyId;
        try
        {
            lobbyId = Integer.parseInt(lobbyIdParam);
        } catch(NumberFormatException ex)
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid lobby_id");
            return;
        }

        // Add user to the selected lobby (session) in the database
        SessionDAO sessionDAO = new SessionDAOImpl();
        ModeratedAnswerDAO moderatedAnswerDAO = new ModeratedAnswerDAOImpl();
        try
        {
            sessionDAO.joinSession(lobbyId, userId);
            if(moderatedAnswerDAO.findAnswersBySession(lobbyId).isEmpty())
            {
                sessionDAO.updateCurrentIndex(lobbyId, 0);
            }
        } catch(SQLException e)
        {
            LOGGER.log(Level.SEVERE, "Failed to join lobby " + lobbyId + " for user " + userId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        session.setAttribute("lobby_id", lobbyId);
        response.sendRedirect(request.getContextPath() + "/quiz");
    }
}
