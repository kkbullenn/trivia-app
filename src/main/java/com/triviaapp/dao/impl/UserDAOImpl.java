package com.triviaapp.dao.impl;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
    private static final String SQL_INSERT = "INSERT INTO users (username, email, password_hash, role_id) VALUES (?, ?, ?, ?)";
    private static final String SQL_FIND_USERID_BY_EMAIL = "SELECT user_id FROM users WHERE email = ?";
    private static final String SQL_FIND_USERNAME_BY_ID = "SELECT username FROM users WHERE user_id = ?";

    @Override
    public String findPasswordByEmail(String email) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PASSWORD_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash");
                }
            }
        }
        return null;
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
}
