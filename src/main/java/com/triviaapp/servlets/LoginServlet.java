package com.triviaapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.*;


public class LoginServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/quizapp";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password"; // change this when server is set up


    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException{
        RequestDispatcher dispatcher = request.getRequestDispatcher("/login.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String userEmail = request.getParameter("email");
        String password = request.getParameter("password");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userEmail);
                stmt.setString(2, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        // Successful login
                        int userId = rs.getInt("user_id");
                        HttpSession session = request.getSession(true);
                        session.setAttribute("user_id", userId);
                        response.sendRedirect("main");
                    } else {
                        // Invalid login
                        response.setContentType("text/html");
                        PrintWriter out = response.getWriter();
                        out.println("<h3>Invalid username or password.</h3>");
                        out.println("<a href='login'>Try again</a>");
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}