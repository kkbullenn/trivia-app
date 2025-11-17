package com.triviaapp.servlets;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Validates quiz session access and forwards players to the active quiz view.
 *
 * @author Brownie Tran
 */
public class QuizServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Check if user is logged in and has joined a lobby
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login");
            return;
        } else if (session.getAttribute("category_id") == null) {
            response.sendRedirect("main");
            return;
        } else if (session.getAttribute("lobby_id") == null) {
            response.sendRedirect("category-lobbies");
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/quiz.html");
        dispatcher.forward(request, response);
    }

}
