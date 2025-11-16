package com.triviaapp.dao.impl;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles JDBC operations for user authentication, profiles, and role lookups.
 *
 * @author Haven Zhang
 * @author Timothy Kim
 * @author Brownie Tran
 * @author Jerry Xing
 */
public class UserDAOImpl implements UserDAO {

    private static final String SQL_FIND_PASSWORD_BY_EMAIL = "SELECT password_hash FROM users WHERE email = ?";
    private static final String SQL_FIND_PASSWORD_BY_USERNAME = "SELECT password_hash FROM users WHERE username = ?";
    private static final String SQL_INSERT = "INSERT INTO users (username, email, password_hash, role_id) VALUES (?, ?, ?, ?)";
    private static final String SQL_FIND_USERID_BY_EMAIL = "SELECT user_id FROM users WHERE email = ?";
    private static final String SQL_FIND_USERID_BY_USERNAME = "SELECT user_id FROM users WHERE username = ?";
    private static final String SQL_FIND_USERNAME_BY_ID = "SELECT username FROM users WHERE user_id = ?";
    private static final String SQL_FIND_PROFILE_BY_ID = "SELECT username, email, avatar_url FROM users WHERE user_id = ?";
    private static final String SQL_UPDATE_PROFILE = "UPDATE users SET username = ?, email = ?, avatar_url = ? WHERE user_id = ?";
    private static final String SQL_IS_USERNAME_TAKEN = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?) AND user_id <> ?";
    private static final String SQL_IS_EMAIL_TAKEN = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?) AND user_id <> ?";
    private static final String SQL_FIND_PASSWORD_BY_ID = "SELECT password_hash FROM users WHERE user_id = ?";
    private static final String SQL_UPDATE_PASSWORD = "UPDATE users SET password_hash = ? WHERE user_id = ?";

    @Override
    public String findPasswordByEmail(String email) throws SQLException {
        return findPasswordHash(SQL_FIND_PASSWORD_BY_EMAIL, email);
    }
    @Override
    public boolean createUser(String username, String email, String password, int roleId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setInt(4, roleId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    @Override
    public int findUserIdByEmail(String email) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection()){
            PreparedStatement ps = conn.prepareStatement(SQL_FIND_USERID_BY_EMAIL);
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("user_id");
            }
        }
        return -1;
    }

    @Override
    public String findPasswordByUsername(String username) throws SQLException {
        return findPasswordHash(SQL_FIND_PASSWORD_BY_USERNAME, username);
    }

    @Override
    public int findUserIdByUsername(String username) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_USERID_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }
        return -1;
    }

    @Override
    public int findUserRoleIdById(int userId) throws SQLException {
        String sql = "SELECT role_id FROM users WHERE user_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("role_id");
                }
            }
        }
        return -1;
    }

    @Override
    public String findUsernameById(int userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_USERNAME_BY_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }
        return null;
    }

    @Override
    public Map<String, String> findUserProfileById(int userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PROFILE_BY_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, String> profile = new HashMap<>();
                    profile.put("username", rs.getString("username"));
                    profile.put("email", rs.getString("email"));
                    profile.put("avatar_url", rs.getString("avatar_url"));
                    return profile;
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateUserProfile(int userId, String username, String email, String avatarUrl) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PROFILE)) {
            ps.setString(1, username);
            ps.setString(2, email);
            if (avatarUrl == null || avatarUrl.isBlank()) {
                ps.setNull(3, Types.VARCHAR);
            } else {
                ps.setString(3, avatarUrl);
            }
            ps.setInt(4, userId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean isUsernameTaken(String username, int excludeUserId) throws SQLException {
        return existsWithExclusion(SQL_IS_USERNAME_TAKEN, username, excludeUserId);
    }

    @Override
    public boolean isEmailTaken(String email, int excludeUserId) throws SQLException {
        return existsWithExclusion(SQL_IS_EMAIL_TAKEN, email, excludeUserId);
    }

    @Override
    public String findPasswordHashById(int userId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PASSWORD_BY_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        }
        return null;
    }

    @Override
    public boolean updatePassword(int userId, String passwordHash) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PASSWORD)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private String findPasswordHash(String sql, String identifier) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        }
        return null;
    }

    private boolean existsWithExclusion(String sql, String identifier, int excludeUserId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setInt(2, excludeUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
