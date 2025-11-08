package com.triviaapp.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


public class JoinCreateQuizServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // User not logged in → redirect to login page
            response.sendRedirect("login");
            return;
        } else if (!"admin".equals(session.getAttribute("role_name"))) {
            // Not an admin, redirect to admin page
            response.sendRedirect("main");
            return;
        }

        response.sendRedirect("/create-quiz");
    }
}
