package com.triviaapp.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Utility helpers for working with HTTP request payloads.
 *
 * @author Jerry Xing
 */
public final class RequestUtils {

    private RequestUtils() {
    }

    public static JSONObject readJsonPayload(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String body;
        try (BufferedReader reader = request.getReader()) {
            body = reader.lines().collect(Collectors.joining());
        }

        if (body.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
            return null;
        }

        try {
            return new JSONObject(body);
        } catch (JSONException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
            return null;
        }
    }
}
