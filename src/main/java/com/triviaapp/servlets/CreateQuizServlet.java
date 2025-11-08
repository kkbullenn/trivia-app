package com.triviaapp.servlets;

import com.triviaapp.dao.impl.QuestionDAOImpl;
import com.triviaapp.dao.impl.SessionDAOImpl;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

public class CreateQuizServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login");
            return;
        } else if (!"admin".equals(session.getAttribute("role_name"))) {
            response.sendRedirect("main");
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/create_quiz.html");
        dispatcher.forward(request, response);
    }

@Override
protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {

    String sessionName = request.getParameter("session_name");
    String categoryIdStr = request.getParameter("category_id");
    String maxParticipantsStr = request.getParameter("max_participants");

    if (sessionName == null || categoryIdStr == null || sessionName.isEmpty() || categoryIdStr.isEmpty()) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
        return;
    }

    HttpSession session = request.getSession(false);
    int hostUserId = (Integer) session.getAttribute("user_id");
    int categoryId = Integer.parseInt(categoryIdStr);
    Integer maxParticipants = null;
    if (maxParticipantsStr != null && !maxParticipantsStr.isEmpty()) {
        maxParticipants = Integer.parseInt(maxParticipantsStr);
    }

    try {
        SessionDAOImpl sessionDAO = new SessionDAOImpl();
        QuestionDAOImpl questionDAO = new QuestionDAOImpl();

        List<Integer> questionIds = new ArrayList<>();
        int index = 1;

        while (request.getParameter("question_text_" + index) != null) {
            String questionText = request.getParameter("question_text_" + index);
            String correctAnswer = request.getParameter("correct_option_" + index);

            // Collect options as JSON string for answers_option
            JSONObject options = new JSONObject();
            options.put("A", request.getParameter("option_a_" + index));
            options.put("B", request.getParameter("option_b_" + index));
            options.put("C", request.getParameter("option_c_" + index));
            options.put("D", request.getParameter("option_d_" + index));
            String answersOptionJson = options.toString();

            boolean created = questionDAO.createQuestion(
                    categoryId,
                    null, // xml_question
                    null, // youtube_url
                    questionText,
                    answersOptionJson,
                    correctAnswer,
                    1, // default points
                    hostUserId
            );

            if (!created) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create question " + index);
                return;
            }

            // Retrieve latest question ID in this category (last inserted)
            List<Integer> ids = questionDAO.findQuestionIdsByCategory(categoryId);
            questionIds.add(ids.get(ids.size() - 1));

            index++;
        }

        if (questionIds.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No questions provided");
            return;
        }

        boolean sessionCreated = sessionDAO.createSession(
                hostUserId,
                sessionName,
                categoryId,
                maxParticipants,
                "active",
                new Timestamp(System.currentTimeMillis()),
                null
        );

        if (!sessionCreated) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create quiz session");
            return;
        }

        // Retrieve latest session ID
        List<Map<String, String>> sessionsByHost = sessionDAO.findSessionsByHost(hostUserId);
        int newSessionId = Integer.parseInt(sessionsByHost.get(sessionsByHost.size() - 1).get("session_id"));


        for (Integer qId : questionIds) {
            sessionDAO.insertQuestionForSession(newSessionId, qId);
        }

        response.setContentType("text/html");
        response.getWriter().println("<html><body style='background:#001D3D;color:#FFD60A;font-family:sans-serif;'>");
        response.getWriter().println("<h2>Quiz created successfully!</h2>");
        response.getWriter().println("<p>Session Name: " + sessionName + "</p>");
        response.getWriter().println("<p>Category ID: " + categoryId + "</p>");
        response.getWriter().println("<p>Session ID: " + newSessionId + "</p>");
        response.getWriter().println("<p>Total Questions Added: " + questionIds.size() + "</p>");
        response.getWriter().println("<a href='main' style='color:#FFC300;'>Return to Dashboard</a>");
        response.getWriter().println("</body></html>");

    } catch (Exception e) {
        e.printStackTrace();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
    }
}

}
