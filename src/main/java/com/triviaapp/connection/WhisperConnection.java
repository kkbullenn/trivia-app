package com.triviaapp.connection;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * This class offers functionality for managing Whisper server's URLs/URIs.
 */
public final class WhisperConnection extends ServerConnection {
    public static final URI WHISPER_GET_URI;
    public static final URI WHISPER_POST_URI;

    private static final Dotenv ENV;
    private static final String ENV_MODE;

    private static final String DEV_MODE = "dev";
    private static final int WHISPER_LOCAL_PORT = 8888;

    private static final String HOST;
    private static final int PORT;
    private static final URI WHISPER_URI;

    static {
        ENV = Dotenv.load();
        ENV_MODE = getMode(ENV.get("WHISPER_MODE"));
        HOST = ENV_MODE.equals(DEV_MODE) ? "localhost" : ENV.get("WHISPER_HOST");
        PORT = ENV_MODE.equals(DEV_MODE) ? WHISPER_LOCAL_PORT : Integer.parseInt(ENV.get("WHISPER_PORT"));
        WHISPER_URI = createURI();
        WHISPER_GET_URI = URI.create(WHISPER_URI + "/whisper");
        WHISPER_POST_URI = URI.create(WHISPER_GET_URI + "/transcribe");
    }

    @Override
    public URL getPostURL() {
        try {
            return WHISPER_POST_URI.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(WHISPER_POST_URI + "is not a valid URL extension", e);
        }
    }

    @Override
    public URL getGetURL() {
        try {
            return WHISPER_GET_URI.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(WHISPER_POST_URI + "is not a valid URL extension", e);
        }
    }

    /**
     * Calls the whisper service api with the given request and returns it's output (via InputStream).
     *
     * @param request The request to forward
     * @return The output from the server.
     */
    public InputStream getTranscription(final HttpServletRequest request, final HttpURLConnection connection) throws IOException {
        final String contentType = request.getContentType();
        final String contentLength = String.valueOf(request.getContentLength());

        // For multipart form data
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

        return connection.getInputStream();
    }

    private static String getMode(final String mode) {
        if (mode == null || mode.isEmpty()) {
            return DEV_MODE;
        }

        return mode;
    }

    private static URI createURI() {
        final String http = DEV_MODE.equals(ENV_MODE) ? "http" : "https";

        return URI.create(String.format("%s://%s:%s", http, HOST, PORT));
    }
}
