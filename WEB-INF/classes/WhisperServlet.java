import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public final class WhisperServlet extends HttpServlet {
    private static final String WHISPER_HOST = "localhost";
    private static final int WHISPER_PORT = 8888;
    private static final URI WHISPER_URI = URI.create("http://" + WHISPER_HOST + ":" + WHISPER_PORT);
    private static final URI WHISPER_GET_URI = URI.create(WHISPER_URI + "/whisper");
    private static final URI WHISPER_POST_URI = URI.create(WHISPER_GET_URI + "/transcribe");

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

        final HttpRequest httpRequest = HttpRequest.newBuilder().uri(WHISPER_GET_URI).build();
        final HttpClient httpClient = HttpClient.newHttpClient();
        // response is a string of a JSON object, FE will need to parse this
        final HttpResponse<String> whisperResponse;

        try {
            whisperResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            response.setContentType("application/json");
            response.setStatus(whisperResponse.statusCode());
            response.getWriter().println(whisperResponse.body());
        } catch (final Exception ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "WhisperServlet Error: " + ex.getMessage());
        }
    }
}