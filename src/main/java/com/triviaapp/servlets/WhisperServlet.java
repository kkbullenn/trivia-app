package com.triviaapp.servlets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.triviaapp.connection.WhisperConnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * BE endpoint for transcribing an audio file (of any language supported) to English text.
 * <p>
 * Available methods: POST (see doPost).
 */
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
     * Takes an audio file and optionally the source language of the auto and transcribes it into English text.
     * <p>
     * Can accept almost all modern audio file extensions.
     * <p>
     * Please see this <a href="https://github.com/SYSTRAN/faster-whisper">link</a> for supported languages and file extensions
     *
     * @param request  multipart form-data containing the file and optionally the source_lang
     *                 <p>
     *                 Sample multipart form-data:
     *                 <p>
     *                 POST /whisper/transcribe HTTP/1.1
     *                 Content-Type: multipart/form-data; boundary=----WebKitFormBoundaryXyZ123
     *                 <p>
     *                 ------WebKitFormBoundaryXyZ123
     *                 Content-Disposition: form-data; name="file"; filename="audio.wav"
     *                 Content-Type: audio/wav
     *                 <p>
     *                 <binary audio data here>
     *                 ------WebKitFormBoundaryXyZ123
     *                 Content-Disposition: form-data; name="source_lang"
     *                 <p>
     *                 auto
     *                 ------WebKitFormBoundaryXyZ123--
     * @param response Gives the response as a JSON object in the following format:
     *                 <p>
     *                 {
     *                 "detected_language":"pt",
     *                 "duration_sec":8.96,
     *                 "translated_text":"I don't want the terrible limitation of the one who lived only the one who was able to make a sense.I don't want the truth invented."
     *                 }
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        // TODO: need to verify request integrity

        final String contentType = request.getContentType();
        final String contentLength = String.valueOf(request.getContentLength());

        // For multipart form data
        final HttpURLConnection connection = (HttpURLConnection) CONNECTION.getPostURL().openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Content-Length", contentLength);

        final InputStream reqInputStream = request.getInputStream();
        final OutputStream whisperOutputStream = connection.getOutputStream();

        // forward data to whisper by writing inputStream to outputStream
        if (reqInputStream != null) {
            reqInputStream.transferTo(whisperOutputStream);
            reqInputStream.close();
        }
        whisperOutputStream.close();

        // getting response from whisper service and giving it to FE
        final int responseCode = connection.getResponseCode();

        response.setStatus(responseCode);
        response.setContentType("application/json");

        final InputStream whisperInputStream = connection.getInputStream();
        final OutputStream resOutputStream = response.getOutputStream();

        if (whisperInputStream != null) {
            whisperInputStream.transferTo(resOutputStream);
            whisperInputStream.close();
        }

        resOutputStream.close();
    }
}