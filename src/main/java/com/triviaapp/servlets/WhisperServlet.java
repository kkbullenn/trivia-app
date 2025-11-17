package com.triviaapp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;

import com.triviaapp.externalapi.WhisperConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * BE endpoint for transcribing an audio file (of any language supported) to English text.
 * See WhisperAnswerCheckerServlet for answer checking functionality. This servlet is strictly for transcription.
 * <p>
 * Available methods: POST (see doPost).
 *
 * @author Samarjit Bhogal
 */
@MultipartConfig
public final class WhisperServlet extends HttpServlet {
    private static final WhisperConnection CONNECTION = new WhisperConnection();

    /**
     * Connects to the Whisper service in order to test if a connection has been established.
     * <p>
     * (NOT required for MVP only for BE testing purposes)
     *
     * @param request  (Does not matter)
     * @param response Message in JSON saying connection is successful
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        // request given does not matter

        final HttpRequest httpRequest = HttpRequest.newBuilder().uri(WhisperConnection.WHISPER_GET_URI).build();
        final HttpClient httpClient = HttpClient.newHttpClient();
        // response is a string of a JSON object, FE will need to parse this
        final HttpResponse<String> whisperResponse;

        try {
            whisperResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            response.setContentType("application/json");
            response.setStatus(whisperResponse.statusCode());
            response.getWriter().println(whisperResponse.body());
        } catch (final Exception ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "WhisperServlet Error: " + ex.getMessage());
        }
    }

    /**
     * Strictly for transcription (speech to text). To check if the audio answer is correct see doPost in
     * WhisperAnswerCheckerServlet.
     * <p>
     * Takes an audio file and optionally the source language of the auto and transcribes it into English text.
     * <p>
     * Can accept almost all modern audio file extensions.
     * <p>
     * Please see this <a href="https://github.com/SYSTRAN/faster-whisper">link</a> for supported languages and file extensions
     *
     * @param request  multipart form-data containing the file and optionally the source_lang
     * @param response Gives the response as a JSON object in the following format:
     *                 <p>
     *                 {
     *                 "detected_language":"pt",
     *                 "duration_sec":8.96,
     *                 "translated_text":"I don't want the terrible limitation of the one who lived only the one who was able to make a sense.I don't want the truth invented."
     *                 }
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException,
            ServletException {
        final HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("/trivia-app/login");
            return;
        }

        final Part filePart = request.getPart("file");

        // verifies that we have file audio
        if (filePart == null) {
            final PrintWriter resWriter = response.getWriter();
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            final String data = "{"
                    + "\"status\": \"error\","
                    + "\"message\": \"Expected 'file' in multipart/form-data.\""
                    + "}";
            resWriter.write(data);
            return;
        }

        final String fileName = filePart.getSubmittedFileName();
        final byte[] audioBytes = filePart.getInputStream().readAllBytes();

        final HttpURLConnection connection = (HttpURLConnection) CONNECTION.getPostURL().openConnection();
        final InputStream whisperInputStream = WhisperConnection.getTranscription(audioBytes, fileName, connection);
        final OutputStream resOutputStream = response.getOutputStream();

        // getting response from whisper service and giving it to FE
        final int responseCode = connection.getResponseCode();
        response.setStatus(responseCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (whisperInputStream != null) {
            whisperInputStream.transferTo(resOutputStream);
            whisperInputStream.close();
        }

        resOutputStream.close();
    }
}