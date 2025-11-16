package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.util.RequestUtils;
import com.triviaapp.util.SessionUtils;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Handles profile data retrieval and updates for the authenticated user.
 *
 * @author Jerry Xing
 */
public class UserProfileServlet extends HttpServlet {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

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

            String email = profile != null ? profile.get("email") : null;
            String storedAvatar = profile != null ? profile.get("avatar_url") : null;
            boolean usingDefault = storedAvatar == null || storedAvatar.isBlank();
            String avatarUrl = usingDefault ? buildAvatarUrl(username) : storedAvatar;

            JSONObject payload = new JSONObject();
            payload.put("username", username);
            if (email != null) {
                payload.put("email", email);
            }
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

        JSONObject payload = RequestUtils.readJsonPayload(request, response);
        if (payload == null) {
            return;
        }

        String username = payload.optString("username", "").trim();
        String email = payload.optString("email", "").trim();
        String avatarUrl = payload.has("avatar_url") ? payload.optString("avatar_url", null) : null;
        if (avatarUrl != null) {
            avatarUrl = avatarUrl.trim();
        }

        if (username.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Username is required");
            return;
        }

        if (email.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email is required");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("message", "Please provide a valid email address.");
            writeJsonResponse(response, error);
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

            if (userDAO.isEmailTaken(email, userId)) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", "Email is already in use. Try logging in instead.");
                writeJsonResponse(response, error);
                return;
            }

            boolean updated = userDAO.updateUserProfile(userId, username, email, avatarUrl);
            if (!updated) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update profile");
                return;
            }

            boolean usingDefault = avatarUrl == null;
            String resolvedAvatar = usingDefault ? buildAvatarUrl(username) : avatarUrl;
            JSONObject result = new JSONObject();
            result.put("success", true);
            result.put("username", username);
            result.put("email", email);
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
