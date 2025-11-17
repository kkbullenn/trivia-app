package com.triviaapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;

/**
 * Handles access control and forwarding for the admin landing page.
 *
 * @author Brownie Tran
 */
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // Not logged in, redirect to  login page
            response.sendRedirect("login");
            return;
        } else if (!"admin".equals(session.getAttribute("role_name"))) {
            // Not an admin, redirect to main page
            response.sendRedirect("main");
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/admin_landing.html");
        dispatcher.forward(request, response);
    }
}
