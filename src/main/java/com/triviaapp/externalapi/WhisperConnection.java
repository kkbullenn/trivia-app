package com.triviaapp.externalapi;

import io.github.cdimascio.dotenv.Dotenv;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * This class offers functionality for managing Whisper server's URLs/URIs.
 *
 * @author Samarjit Bhogal
 */
public final class WhisperConnection extends ServerConnection {
    public static final URI WHISPER_GET_URI;
    public static final URI WHISPER_POST_URI;

    private static final Dotenv ENV;
    private static final String ENV_MODE;
    private static final String HF_API_TOKEN;

    private static final String DEV_MODE = "dev";
    private static final int WHISPER_LOCAL_PORT = 8888;

    private static final String HOST;
    private static final int PORT;
    private static final URI WHISPER_URI;
    private static final String HF_API_URL = "https://router.huggingface.co/hf-inference/models/sentence-transformers/all-MiniLM-L6-v2/pipeline/sentence-similarity";

    static {
        ENV = Dotenv.load();
        ENV_MODE = getMode(ENV.get("WHISPER_MODE"));
        HOST = ENV_MODE.equals(DEV_MODE) ? "localhost" : ENV.get("WHISPER_HOST");
        PORT = ENV_MODE.equals(DEV_MODE) ? WHISPER_LOCAL_PORT : Integer.parseInt(Objects.requireNonNull(ENV.get("WHISPER_PORT")));
        WHISPER_URI = createURI();
        WHISPER_GET_URI = URI.create(WHISPER_URI + "/whisper");
        WHISPER_POST_URI = URI.create(WHISPER_GET_URI + "/transcribe");
        HF_API_TOKEN = ENV.get("WHISPER_HF_API_KEY");
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
     * Calls the whisper service api with the given body and returns it's output (via InputStream).
     *
     * @param body The body of the request to forward (multipart/form-data)
     * @return The output from the server.
     */
    public static InputStream getTranscription(final byte[] body,
                                               final String fileName,
                                               final HttpURLConnection connection) throws IOException {
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("file", body, ContentType.create("audio/*"), fileName);

        final HttpEntity multipart = builder.build();
        final String contentType = multipart.getContentType();
        final long contentLength = multipart.getContentLength();

        // For multipart form data
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Content-Length", "" + contentLength);

        // forward data to whisper by writing body to outputStream
        final OutputStream whisperOutputStream = connection.getOutputStream();
        multipart.writeTo(whisperOutputStream);
        whisperOutputStream.close();

        return connection.getInputStream();
    }

    @NotNull
    public static JSONArray getMatchResult(final String translatedText, final List<String> answersList) throws IOException {
        final JSONObject payload = WhisperConnection.getTransformationPayload(translatedText, answersList);
        final String payloadStr = payload.toString();

        // Setup HttpURLConnection
        final URL url = new URL(HF_API_URL);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + HF_API_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        // Send request
        try (final OutputStream os = conn.getOutputStream()) {
            os.write(payloadStr.getBytes());
            os.flush();
        }

        final InputStream inputStream;
        if (conn.getResponseCode() >= 400) {
            inputStream = conn.getErrorStream();
        } else {
            inputStream = conn.getInputStream();
        }

        // Read response
        final byte[] responseBytes = inputStream.readAllBytes();
        final String responseString = new String(responseBytes);
        conn.disconnect();

        //System.out.println("Success Probability: " + successProbability);

        // Parse response (Hugging Face returns JSON array)
        // Extract probability JSONArray
        return new JSONArray(responseString);
    }

    @NotNull
    private static JSONObject getTransformationPayload(final String translatedText, final List<String> answers) {
        final JSONObject inputs = new JSONObject();
        inputs.put("source_sentence", translatedText);
        inputs.put("sentences", answers.toArray());

        final JSONObject payload = new JSONObject();
        payload.put("inputs", inputs);

        return payload;
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
