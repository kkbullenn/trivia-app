package com.triviaapp.servlets;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * BE proxy for the Python M2M100 translation service.
 *
 * Proxied Python base: http://localhost:8892
 *
 * Endpoints exposed by this servlet (same paths as Python):
 *  - GET /m2m100 : health check JSON from Python
 *  - GET /m2m100/languages : list of language codes (limited or full)
 *  - POST /m2m100/translate : translate single text (multipart/form-data)
 *  - POST /m2m100/translate/questions-answers : batch translate Q/A arrays (application/json)
 *
 * Notes:
 *  - This servlet streams request bodies through to the Python service and
 *    streams responses back to the FE unchanged (including status + content type).
 *  - Keep the Python service running locally on the configured host/port.
 *
 * @author Aira Bassig
 */
public final class M2M100Servlet extends HttpServlet {

    // ---------- Python service location ----------
    private static final String M2M_HOST = "localhost";
    private static final int M2M_PORT = 8892;
    private static final URI BASE = URI.create("http://" + M2M_HOST + ":" + M2M_PORT);

    // Python endpoints
    private static final URI URI_HEALTH = BASE.resolve("/m2m100");
    private static final URI URI_LANGS = BASE.resolve("/m2m100/languages");
    private static final URI URI_TEXT = BASE.resolve("/m2m100/translate");
    private static final URI URI_QA = BASE.resolve("/m2m100/translate/questions-answers");

    // Pre-built URLs for streaming POSTs
    private static final URL URL_TEXT;
    private static final URL URL_QA;
    static {
        try {
            URL_TEXT = URI_TEXT.toURL();
            URL_QA = URI_QA.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException("Invalid M2M100 Python URLs", e);
        }
    }

    // ---------- GET /m2m100 and /m2m100/languages ----------
    /**
     * Connects to the M2M100 service to proxy simple GET requests.
     * (For connectivity tests and for FE language dropdowns.)
     *
     * @param req incoming request (path must be /m2m100 or /m2m100/languages)
     * @param resp JSON response from Python (status + body forwarded)
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        // Build a servlet-local path (ignores context path)
        final String sp = req.getServletPath();   // e.g. "/m2m100"
        final String pi = req.getPathInfo();      // e.g. "/languages" or null
        final String path = (pi == null) ? sp : (sp + pi);

        if ("/m2m100".equals(path)) {
            proxySimpleGet(URI_HEALTH, resp);
            return;
        }
        if ("/m2m100/languages".equals(path)) {
            proxySimpleGet(URI_LANGS, resp);
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unsupported GET path: " + path);
    }

    /** Helper to proxy a simple GET to Python, with short timeouts. */
    private void proxySimpleGet(final URI target, final HttpServletResponse resp) throws IOException {
        final HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(target)
                .timeout(Duration.ofSeconds(10))
                .build();

        try {
            final HttpResponse<String> py = client.send(request, HttpResponse.BodyHandlers.ofString());
            resp.setStatus(py.statusCode());
            resp.setContentType("application/json");
            resp.getWriter().println(py.body());
        } catch (final Exception ex) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "M2M100 GET error: " + ex.getMessage());
        }
    }

    // ---------- POST /m2m100/translate and /m2m100/translate/questions-answers ----------
    /**
     * Proxies translation POST requests to the Python service.
     *
     * Supported POST paths:
     *  - /m2m100/translate
     *      Expected multipart form-data fields:
     *          text : string (required)
     *          target : ISO 639-1 code, e.g., "es" (required)
     *          source : ISO 639-1 code, default "en" (optional)
     *
     *  - /m2m100/translate/questions-answers
     *      Expected JSON body:
     *      {
     *        "questions": ["..."],    // optional
     *        "answers": ["..."],      // optional
     *        "source": "en",
     *        "target": "tl"
     *      }
     *
     * @param req request with body (multipart or JSON)
     * @param resp JSON response from Python (status + body forwarded)
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        final String sp = req.getServletPath();
        final String pi = req.getPathInfo();
        final String path = (pi == null) ? sp : (sp + pi);

        if ("/m2m100/translate".equals(path)) {
            proxyStreamingPost(req, resp, URL_TEXT);   // multipart passthrough
            return;
        }
        if ("/m2m100/translate/questions-answers".equals(path)) {
            proxyStreamingPost(req, resp, URL_QA);     // JSON passthrough
            return;
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Unsupported POST path: " + path);
    }

    /**
     * Streams the incoming request body to Python and streams the response back to the client.
     * Preserves Content-Type and HTTP status codes from the Python service.
     */
    private void proxyStreamingPost(final HttpServletRequest req,
                                    final HttpServletResponse resp,
                                    final URL target) throws IOException {

        final String contentType = req.getContentType();
        final int len = req.getContentLength();

        final HttpURLConnection conn = (HttpURLConnection) target.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(30_000);

        if (contentType != null) conn.setRequestProperty("Content-Type", contentType);
        if (len >= 0) conn.setRequestProperty("Content-Length", String.valueOf(len));

        // Forward body to Python
        try (InputStream in = req.getInputStream();
             OutputStream out = conn.getOutputStream()) {
            if (in != null) in.transferTo(out);
        }

        // Relay Python response
        final int code = conn.getResponseCode();
        resp.setStatus(code);

        final String pyCT = conn.getHeaderField("Content-Type");
        resp.setContentType(pyCT != null ? pyCT : "application/json");

        try (InputStream pin = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
             OutputStream pout = resp.getOutputStream()) {
            if (pin != null) pin.transferTo(pout);
        }
    }
}
