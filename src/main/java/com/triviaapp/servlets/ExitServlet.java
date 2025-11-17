package com.triviaapp.servlets;

import java.io.IOException;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Ends the current user session and redirects to the login page.
 *
 * @author Yang Li
 * @author Jerry Xing
 */
public class ExitServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        HttpSession s = req.getSession(false);
        if(s != null)
        {
            s.invalidate();
        }
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    // Optional: also handle GET (in case someone links to /EXIT)
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
    {
        doPost(req, resp);
    }
}
