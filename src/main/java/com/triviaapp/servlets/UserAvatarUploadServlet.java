package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles avatar file uploads for the authenticated user.
 *
 * @author Jerry Xing
 */
@MultipartConfig(maxFileSize = 2 * 1024 * 1024, maxRequestSize = 3 * 1024 * 1024)
public class UserAvatarUploadServlet extends HttpServlet {

    private static final Map<String, String> IMAGE_EXTENSIONS = new HashMap<>();

    static {
        IMAGE_EXTENSIONS.put("image/png", "png");
        IMAGE_EXTENSIONS.put("image/jpeg", "jpg");
        IMAGE_EXTENSIONS.put("image/jpg", "jpg");
        IMAGE_EXTENSIONS.put("image/gif", "gif");
        IMAGE_EXTENSIONS.put("image/webp", "webp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return;
        }

        Part avatarPart = request.getPart("avatar");
        if (avatarPart == null || avatarPart.getSize() == 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file uploaded");
            return;
        }

        String contentType = avatarPart.getContentType();
        if (contentType == null || !IMAGE_EXTENSIONS.containsKey(contentType.toLowerCase())) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported image type");
            return;
        }

        long size = avatarPart.getSize();
        if (size > 2L * 1024L * 1024L) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "File exceeds 2MB limit");
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");
        String extension = IMAGE_EXTENSIONS.get(contentType.toLowerCase());
        String fileName = "user-" + userId + "-" + System.currentTimeMillis() + "." + extension;

        String uploadDirPath = getServletContext().getRealPath("/uploads/avatars");
        if (uploadDirPath == null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Upload path unavailable");
            return;
        }

        File uploadDir = new File(uploadDirPath);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create upload directory");
            return;
        }

        File destination = new File(uploadDir, fileName);
        try (InputStream input = avatarPart.getInputStream()) {
            Files.copy(input, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        String avatarUrl = request.getContextPath() + "/uploads/avatars/" + fileName;
        UserDAO userDAO = new UserDAOImpl();

        try {
            Map<String, String> profile = userDAO.findUserProfileById(userId);
            String username = profile != null ? profile.get("username") : null;
            if (username == null || username.isBlank()) {
                username = "Player" + userId;
            }
            String email = profile != null ? profile.get("email") : null;
            if (email == null || email.isBlank()) {
                Files.deleteIfExists(destination.toPath());
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User email not found; cannot update avatar");
                return;
            }
            userDAO.updateUserProfile(userId, username, email, avatarUrl);
        } catch (SQLException ex) {
            Files.deleteIfExists(destination.toPath());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        JSONObject result = new JSONObject();
        result.put("success", true);
        result.put("avatar_url", avatarUrl);
        result.put("using_default", false);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.write(result.toString());
        }
    }
}
