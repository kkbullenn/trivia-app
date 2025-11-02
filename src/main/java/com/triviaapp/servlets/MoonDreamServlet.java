import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;

/**
 * BE endpoint for captioning an image file or summarizing a video file using
 * Moondream.
 * Available methods: POST (see doPost) for caption/summary, GET (see doGet) for
 * ping.
 */
@MultipartConfig
public final class MoondreamServlet extends HttpServlet {
    private static final String MOONDREAM_HOST = "localhost";
    private static final int MOONDREAM_PORT = 8082;
    private static final URI MOONDREAM_URI = URI.create("http://" + MOONDREAM_HOST + ":" + MOONDREAM_PORT);
    private static final URI MOONDREAM_PING_URI = URI.create(MOONDREAM_URI + "/ping"); // Assume you add /ping to Flask
    private static final String CRLF = "\r\n";

    private static URL getTargetUrl(final String path) {
        try {
            return URI.create(MOONDREAM_URI + path).toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException("Invalid URL for path: " + path, e);
        }
    }

    /**
     * Health check: Pings the Moondream service.
     *
     * @param request  Ignored.
     * @param response Forwards the response from Moondream /ping (e.g., "OK" if
     *                 healthy).
     */
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final URL pingUrl;
        try {
            pingUrl = MOONDREAM_PING_URI.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(MOONDREAM_PING_URI + " is not a valid URL", e);
        }
        final HttpURLConnection connection = (HttpURLConnection) pingUrl.openConnection();
        connection.setRequestMethod("GET");
        final int responseCode = connection.getResponseCode();
        response.setStatus(responseCode);
        if (responseCode == HttpServletResponse.SC_OK) {
            try (final InputStream inputStream = connection.getInputStream();
                    final OutputStream outputStream = response.getOutputStream()) {
                inputStream.transferTo(outputStream);
            }
        } else {
            response.sendError(responseCode, "Moondream service error");
        }
    }

    /**
     * Takes an image or video file and generates a short caption or summary using
     * Moondream.
     * <p>
     * Supports common image/video formats (PNG, JPEG, MP4, MKV, etc.).
     *
     * @param request  multipart form-data containing the file (key: "image" for
     *                 caption or "video" for summary)
     * @param response Gives the response as a JSON object, e.g.:
     *                 {
     *                 "caption": "A generated caption here"
     *                 } or {
     *                 "summary": "A generated summary here"
     *                 }
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final Collection<Part> parts = request.getParts();
        if (parts.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file provided");
            return;
        }

        // Determine target path based on part name (supports "image" or "video";
        // assumes single file part)
        String targetPath = null;
        for (final Part part : parts) {
            final String partName = part.getName();
            if ("image".equals(partName)) {
                targetPath = "/caption";
                break;
            } else if ("video".equals(partName)) {
                targetPath = "/summarize_video";
                break;
            }
        }
        if (targetPath == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported file type (use 'image' or 'video')");
            return;
        }

        // Forward to Flask using HttpURLConnection
        final URL targetUrl = getTargetUrl(targetPath);
        final HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setChunkedStreamingMode(0); // Enable chunked transfer (no need for Content-Length)

        // Generate boundary and set Content-Type
        final String boundary = "----" + Long.toHexString(System.currentTimeMillis());
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        // Write multipart body
        try (final OutputStream output = connection.getOutputStream();
                final PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true)) {
            for (final Part part : parts) {
                writer.append("--" + boundary).append(CRLF);
                writer.append("Content-Disposition: form-data; name=\"" + part.getName() + "\"");
                final String filename = part.getSubmittedFileName();
                if (filename != null && !filename.isEmpty()) {
                    writer.append("; filename=\"" + filename + "\"");
                }
                writer.append(CRLF);
                final String contentType = part.getContentType();
                if (contentType != null) {
                    writer.append("Content-Type: " + contentType).append(CRLF);
                }
                writer.append(CRLF).flush();

                // Transfer part content
                try (final InputStream partInput = part.getInputStream()) {
                    partInput.transferTo(output);
                }
                output.flush();
                writer.append(CRLF).flush();
            }
            writer.append("--" + boundary + "--").append(CRLF).flush();
        }

        // Get response from Flask and forward to client
        final int responseCode = connection.getResponseCode();
        response.setStatus(responseCode);
        response.setContentType("application/json");
        if (responseCode == HttpServletResponse.SC_OK) {
            try (final InputStream inputStream = connection.getInputStream();
                    final OutputStream outputStream = response.getOutputStream()) {
                inputStream.transferTo(outputStream);
            }
        } else {
            final String errorMessage;
            try (final InputStream errorStream = connection.getErrorStream()) {
                errorMessage = errorStream != null ? new String(errorStream.readAllBytes(), "UTF-8")
                        : "Moondream service error";
            }
            response.sendError(responseCode, errorMessage);
        }
    }
}