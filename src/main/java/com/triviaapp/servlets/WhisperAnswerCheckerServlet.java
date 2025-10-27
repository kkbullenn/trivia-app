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



//    @Override
//    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
//        // TODO: need to verify request integrity
//
//        final String contentType = request.getContentType();
//        final String contentLength = String.valueOf(request.getContentLength());
//
//        // For multipart form data
//        final HttpURLConnection connection = (HttpURLConnection) WHISPER_POST_URL.openConnection();
//        connection.setDoOutput(true);
//        connection.setRequestMethod("POST");
//        connection.setRequestProperty("Content-Type", contentType);
//        connection.setRequestProperty("Content-Length", contentLength);
//
//        final InputStream reqInputStream = request.getInputStream();
//        final OutputStream whisperOutputStream = connection.getOutputStream();
//
//        // forward data to whisper by writing inputStream to outputStream
//        if (reqInputStream != null) {
//            reqInputStream.transferTo(whisperOutputStream);
//            reqInputStream.close();
//        }
//        whisperOutputStream.close();
//
//        // getting response from whisper service and giving it to FE
//        final int responseCode = connection.getResponseCode();
//
//        response.setStatus(responseCode);
//        response.setContentType("application/json");
//
//        final InputStream whisperInputStream = connection.getInputStream();
//        final OutputStream resOutputStream = response.getOutputStream();
//
//        if (whisperInputStream != null) {
//            whisperInputStream.transferTo(resOutputStream);
//            whisperInputStream.close();
//        }
//
//        resOutputStream.close();
//    }
}
