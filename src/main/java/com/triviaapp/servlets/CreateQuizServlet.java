package com.triviaapp.servlets;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.sql.*;


public class CreateQuizServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/quizapp";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password"; // change this when server is set up

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/create_quiz.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        // Get the logged-in user ID from the session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // User not logged in, redirect to login page
            response.sendRedirect("login");
            return;
        }
        int uploadedBy = (Integer) session.getAttribute("user_id");

        // Retrieve form parameters
        int categoryId = Integer.parseInt(request.getParameter("category_id"));
        String xmlQuestion = request.getParameter("xml_question");
        String youtubeUrl = request.getParameter("youtube_url");
        String questionText = request.getParameter("question_text");
        int points = Integer.parseInt(request.getParameter("points"));

        String correctedAnswer = request.getParameter("correctedAnswer");
        String incorrect1 = request.getParameter("incorrectedAnswer1");
        String incorrect2 = request.getParameter("incorrectedAnswer2");
        String incorrect3 = request.getParameter("incorrectedAnswer3");

        // Build the JSON for answers
        JSONArray answersArray = new JSONArray();
        answersArray.put(new JSONObject().put("key", "A").put("text", correctedAnswer));
        answersArray.put(new JSONObject().put("key", "B").put("text", incorrect1));
        answersArray.put(new JSONObject().put("key", "C").put("text", incorrect2));
        answersArray.put(new JSONObject().put("key", "D").put("text", incorrect3));

        String answersJson = answersArray.toString();
        String answersKey = "A"; // correct answer key

        // Insert into database
        String sql = "INSERT INTO questions " +
                "(category_id, xml_question, youtube_url, question_text, answers_option, answers_key, points, uploaded_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoryId);
            stmt.setString(2, xmlQuestion);
            stmt.setString(3, youtubeUrl);
            stmt.setString(4, questionText);
            stmt.setString(5, answersJson);      // JSON stored as string
            stmt.setString(6, answersKey);
            stmt.setInt(7, points);
            stmt.setInt(8, uploadedBy);       // from session

            stmt.executeUpdate();

            // Redirect back to admin dashboard or success page
            response.sendRedirect("admin");

        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error.");
        }
    }

}
