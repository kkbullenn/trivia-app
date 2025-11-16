package com.triviaapp.servlets;

import java.io.IOException;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Manages navigation to the category lobbies page after validating session state.
 *
 * @author Brownie Tran
 */
public class CategoryLobbiesServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // User not logged in → redirect to login page
            response.sendRedirect("login");
            return;
        } else if (session.getAttribute("category_id") == null) {
            // No category selected → redirect to main page
            response.sendRedirect("main");
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/category-lobbies.html");
        dispatcher.forward(request, response);
    }
}