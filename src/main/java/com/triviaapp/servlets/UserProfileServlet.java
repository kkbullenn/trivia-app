package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.util.SessionUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles profile data retrieval and updates for the authenticated user.
 *
 * @author Jerry Xing
 */
public class UserProfileServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = SessionUtils.requireSession(request, response);
        if (session == null) {
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");
        UserDAO userDAO = new UserDAOImpl();

        try {
            Map<String, String> profile = userDAO.findUserProfileById(userId);
            String username = profile != null && profile.get("username") != null
                    ? profile.get("username")
                    : "Player" + userId;
            if (username.isBlank()) {
                username = "Player" + userId;
            }

            String storedAvatar = profile != null ? profile.get("avatar_url") : null;
            boolean usingDefault = storedAvatar == null || storedAvatar.isBlank();
            String avatarUrl = usingDefault ? buildAvatarUrl(username) : storedAvatar;

            JSONObject payload = new JSONObject();
            payload.put("username", username);
            payload.put("avatar_url", avatarUrl);
            payload.put("using_default", usingDefault);

            writeJsonResponse(response, payload);
        } catch (SQLException ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = SessionUtils.requireSession(request, response);
        if (session == null) {
            return;
        }

        String body;
        try (BufferedReader reader = request.getReader()) {
            body = reader.lines().collect(Collectors.joining());
        }

        if (body.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Empty payload");
            return;
        }

        JSONObject payload;
        try {
            payload = new JSONObject(body);
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
            return;
        }

        String username = payload.optString("username", "").trim();
        String avatarUrl = payload.has("avatar_url") ? payload.optString("avatar_url", null) : null;
        if (avatarUrl != null) {
            avatarUrl = avatarUrl.trim();
        }

        if (username.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            String contextPath = request.getContextPath();
            boolean isHttp = avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://");
            boolean isContextRelative = contextPath != null && !contextPath.isEmpty() && avatarUrl.startsWith(contextPath + "/");
            boolean isRootRelative = avatarUrl.startsWith("/");

            if (!isHttp && !isContextRelative && !isRootRelative) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", "Avatar URL must start with http(s) or be a relative path on this server.");
                writeJsonResponse(response, error);
                return;
            }

            if (isRootRelative && !isContextRelative && contextPath != null && !contextPath.isEmpty()) {
                avatarUrl = contextPath + avatarUrl;
            }
        } else {
            avatarUrl = null;
        }

        int userId = (Integer) session.getAttribute("user_id");
        UserDAO userDAO = new UserDAOImpl();

        try {
            if (userDAO.isUsernameTaken(username, userId)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", "Username is already taken. Please choose another one.");
                writeJsonResponse(response, error);
                return;
            }

            boolean updated = userDAO.updateUserProfile(userId, username, avatarUrl);
            if (!updated) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update profile");
                return;
            }

            boolean usingDefault = avatarUrl == null;
            String resolvedAvatar = usingDefault ? buildAvatarUrl(username) : avatarUrl;
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("username", username);
            result.put("avatar_url", resolvedAvatar);
            result.put("using_default", usingDefault);

            writeJsonResponse(response, result);
        } catch (SQLException ex) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    private void writeJsonResponse(HttpServletResponse response, JSONObject json)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(json.toString());
        }
    }

    private String buildAvatarUrl(String username) {
        String seed = username == null ? "player" : username.trim();
        if (seed.isEmpty()) {
            seed = "player";
        }
        String encoded = URLEncoder.encode(seed, StandardCharsets.UTF_8);
        return "https://github.com/identicons/" + encoded + ".png";
    }
}
