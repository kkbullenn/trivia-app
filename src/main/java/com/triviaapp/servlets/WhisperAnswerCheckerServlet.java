package com.triviaapp.servlets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.triviaapp.connection.WhisperConnection;

import java.io.IOException;

public class WhisperAnswerCheckerServlet extends HttpServlet {
    private static final WhisperConnection CONNECTION = new WhisperConnection();

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        // transcribe audio
        // get question and answers from database of this quiz
        // get correct answer to question
        // send to a model to depict if the user is any amount of correct
        // if so send a response body with "y"
        // if not send a response body with "n"
    }
}
