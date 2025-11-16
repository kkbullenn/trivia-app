package com.triviaapp.servlets;

import com.triviaapp.dao.RoleDAO;
import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.UserStatsDAO;
import com.triviaapp.dao.impl.RoleDAOImpl;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.dao.impl.UserStatsDAOImpl;
import jakarta.servlet.ServletException;
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

/**
 * Provides aggregated statistics about the logged-in user as JSON.
 *
 * @author Jerry Xing
 */
public class UserDataServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return;
        }

        int userId = (Integer) session.getAttribute("user_id");
        UserDAO userDAO = new UserDAOImpl();
        UserStatsDAO statsDAO = new UserStatsDAOImpl();

        try {
            String username = userDAO.findUsernameById(userId);
            if (username == null || username.isBlank()) {
                username = "Player" + userId;
            }

            String roleName = (String) session.getAttribute("role_name");
            if (roleName == null || roleName.isBlank()) {
                int roleId = userDAO.findUserRoleIdById(userId);
                if (roleId > 0) {
                    RoleDAO roleDAO = new RoleDAOImpl();
                    roleName = roleDAO.findRoleNameById(roleId);
                }
            }
            if (roleName == null || roleName.isBlank()) {
                roleName = "User";
            }

            int participations = statsDAO.countSessionsParticipated(userId);
            int wins = statsDAO.countWins(userId);
            double winRate = participations > 0 ? (double) wins / participations : 0.0d;

            Map<String, String> topCategory = statsDAO.findTopCategoryByScore(userId);

            JSONObject payload = new JSONObject();
            payload.put("username", username);
            payload.put("initial", username.substring(0, 1).toUpperCase());
            payload.put("role_name", roleName);
            payload.put("participations", participations);
            payload.put("wins", wins);
            payload.put("win_rate", Math.round(winRate * 1000d) / 10d);
            payload.put("avatar_url", buildAvatarUrl(username));

            if (topCategory != null) {
                JSONObject categoryJson = new JSONObject();
                categoryJson.put("category_id", topCategory.get("category_id"));
                categoryJson.put("category_name", topCategory.get("category_name"));
                categoryJson.put("total_score", topCategory.get("total_score"));
                payload.put("top_category", categoryJson);
            } else {
                payload.put("top_category", JSONObject.NULL);
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            try (PrintWriter out = response.getWriter()) {
                out.write(payload.toString());
                out.flush();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
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
