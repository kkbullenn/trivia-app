package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.util.DBConnectionManager;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.sql.*;


public class LoginServlet extends HttpServlet {


    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/login.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        final UserDAO userDAO = new UserDAOImpl();

        String email = request.getParameter("user_id");
        String password = request.getParameter("password");

        try {
            String userPasswordByEmail = userDAO.findPasswordByEmail(email);


            if (userPasswordByEmail != null && BCrypt.checkpw(password, userPasswordByEmail)) {

                // Successful login
                HttpSession session = request.getSession(true);
                int user_id = userDAO.findUserIDByEmail(email);
                if (user_id == -1) {
                    throw new SQLException("Database inconsistency: User found but ID not found.");
                } else {
                    session.setAttribute("user_id", user_id);
                    response.sendRedirect("main");
                }


            } else {
                // Invalid login
                response.setContentType("text/html");
                PrintWriter out = response.getWriter();
                out.println("<h3>Invalid email or password.</h3>");
                out.println("<a href='login'>Try again</a>");
                response.sendRedirect("login");
            }


        } catch (SQLException e) {
            e.printStackTrace();
            throw new ServletException("Login failed due to a database error.", e);
        }
    }
}