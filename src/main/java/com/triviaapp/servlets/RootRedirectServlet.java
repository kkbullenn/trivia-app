package com.triviaapp.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Handles requests to the application root and redirects users to the correct entry point based on their session
 * state:
 * <ul>
 *   <li>Unauthenticated users are sent to the login page.</li>
 *   <li>Authenticated admins are sent to the admin landing page.</li>
 *   <li>Authenticated regular users are sent to the main page.</li>
 * </ul>
 *
 * @author Jerry Xing
 */
public class RootRedirectServlet extends HttpServlet {

    /**
     * Handles GET requests to the application root and redirects users to the correct entry point based on their
     * session state.
     *
     * @param request  The HttpServletRequest object
     * @param response The HttpServletResponse object
     * @throws IOException      If an I/O error occurs
     * @throws ServletException If a servlet error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        HttpSession session = request.getSession(false);
        String contextPath = request.getContextPath();

        if(session == null || session.getAttribute("user_id") == null)
        {
            response.sendRedirect(contextPath + "/login");
            return;
        }

        String roleName = (String) session.getAttribute("role_name");
        if("admin".equals(roleName))
        {
            response.sendRedirect(contextPath + "/admin");
        } else
        {
            response.sendRedirect(contextPath + "/main");
        }
    }
}
