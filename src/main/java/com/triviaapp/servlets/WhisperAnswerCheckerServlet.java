package com.triviaapp.servlets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.triviaapp.connection.WhisperConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

public class WhisperAnswerCheckerServlet extends HttpServlet {
    private static final WhisperConnection CONNECTION = new WhisperConnection();

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

    }
}
