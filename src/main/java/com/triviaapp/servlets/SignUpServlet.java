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
        RequestDispatcher dispatcher = request.getRequestDispatcher("/signUp.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        final UserDAO userDAO = new UserDAOImpl();


        String email = request.getParameter("user_id");
        String password = request.getParameter("password");
        int roleId = Integer.parseInt(request.getParameter("role_id"));

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

            boolean success = userDAO.createUser(userName, email, hashedPassword, roleId);

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

