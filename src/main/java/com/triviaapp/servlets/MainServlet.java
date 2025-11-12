package com.triviaapp.servlets;

import java.io.IOException;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class MainServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login");
            return;
        } else if (!"user".equals(session.getAttribute("role_name"))) {
            // Not an admin, redirect to main page
            response.sendRedirect("admin");
            return;
        }
        
        RequestDispatcher dispatcher = request.getRequestDispatcher("/select-quiz.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        // Will implement later if needed
    }
}
