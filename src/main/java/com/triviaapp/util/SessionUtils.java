package com.triviaapp.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Utility class for session management.
 *
 * @author Jerry Xing
 */
public final class SessionUtils {

    private SessionUtils() {
    }

    public static HttpSession requireSession(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return null;
        }

        Object userIdObj = session.getAttribute("user_id");
        if (!(userIdObj instanceof Integer)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return null;
        }

        return session;
    }

    public static Integer requireUserId(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = requireSession(request, response);
        if (session == null) {
            return null;
        }

        return (Integer) session.getAttribute("user_id");
    }
}
