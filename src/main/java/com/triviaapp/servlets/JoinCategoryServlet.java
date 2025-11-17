package com.triviaapp.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Records the selected category in session and routes the user to lobby listings.
 *
 * @author Brownie Tran
 */
public class JoinCategoryServlet extends HttpServlet {

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user_id") == null) {
            // User not logged in → redirect to login page
            response.sendRedirect("login");
            return;
        }

        int categoryId = Integer.parseInt(request.getParameter("category_id"));
        session.setAttribute("category_id", categoryId);
        response.sendRedirect("category-lobbies");
    }
    
}
