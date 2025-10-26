import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * BE endpoint for captioning an image file using Moondream.
 * Available methods: POST (see doPost).
 */
public final class MoondreamServlet extends HttpServlet {
    private static final String MOONDREAM_HOST = "localhost";
    private static final int MOONDREAM_PORT = 8082;
    private static final URI MOONDREAM_URI = URI.create("http://" + MOONDREAM_HOST + ":" + MOONDREAM_PORT);
    private static final URI MOONDREAM_GET_URI = URI.create(MOONDREAM_URI + "/ping"); // Assume you add /ping to Flask
    private static final URI MOONDREAM_POST_URI = URI.create(MOONDREAM_URI + "/caption");
    private static final URL MOONDREAM_POST_URL;
    static {
        try {
            MOONDREAM_POST_URL = MOONDREAM_POST_URI.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(MOONDREAM_POST_URI + " is not a valid URL", e);
        }
    }

    /**
     * Takes an image file and generates a short caption using Moondream.
     * <p>
     * Supports common image formats (PNG, JPEG, etc.).
     *
     * @param request  multipart form-data containing the image file (key: "image")
     * @param response Gives the response as a JSON object, e.g.:
     *                 {
     *                 "caption": "A generated caption here"
     *                 }
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final Part filePart = request.getPart("image");
        if (filePart == null || filePart.getSize() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No image file provided");
            return;
        }

        final String contentType = request.getContentType();
        final String contentLength = String.valueOf(request.getContentLength());

        // Forward to Flask using HttpURLConnection (matches teammate's style)
        final HttpURLConnection connection = (HttpURLConnection) MOONDREAM_POST_URL.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", contentType);
        connection.setRequestProperty("Content-Length", contentLength);

        final InputStream reqInputStream = request.getInputStream();
        final OutputStream moondreamOutputStream = connection.getOutputStream();
        if (reqInputStream != null) {
            reqInputStream.transferTo(moondreamOutputStream);
            reqInputStream.close();
        }
        moondreamOutputStream.close();

        // Get response from Flask and forward to client
        final int responseCode = connection.getResponseCode();
        response.setStatus(responseCode);
        response.setContentType("application/json");

        if (responseCode == HttpServletResponse.SC_OK) {
            final InputStream moondreamInputStream = connection.getInputStream();
            final OutputStream resOutputStream = response.getOutputStream();
            if (moondreamInputStream != null) {
                moondreamInputStream.transferTo(resOutputStream);
                moondreamInputStream.close();
            }
            resOutputStream.close();
        } else {
            response.sendError(responseCode, "Moondream service error");
        }
    }
}