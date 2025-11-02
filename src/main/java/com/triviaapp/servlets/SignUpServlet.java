package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.sql.*;

public class SignUpServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/signup.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        final UserDAO userDAO = new UserDAOImpl();
        final int DEFAULT_ROLE_ID = 100;

        String email = request.getParameter("user_id");
        String password = request.getParameter("password");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try{

            if (userDAO.findPasswordByEmail(email) != null) {
                out.println("<h3>Email already registered!</h3>");
                out.println("<a href='signup'>Try again</a>");
                response.sendRedirect("signup");
            }
            // generate username based on the email input
            String userName = email.substring(0, email.indexOf('@'));
            //hashing password for secure the user password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            boolean success = userDAO.createUser(userName, email, hashedPassword, DEFAULT_ROLE_ID);

            if (success) {
                response.sendRedirect("login");
            } else {
                out.println("<h3>Sign up failed unexpectedly.</h3>");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new ServletException("signup failed due to a database error.", e);
        }
    }
}

