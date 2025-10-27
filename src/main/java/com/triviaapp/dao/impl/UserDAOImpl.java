package com.triviaapp.dao.impl;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAOImpl implements UserDAO {

    private static final String SQL_FIND_PASSWORD_BY_EMAIL = "SELECT password_hash FROM users WHERE email = ?";
    private static final String SQL_INSERT = "INSERT INTO users (username, email, password_hash, role_id) VALUES (?, ?, ?, ?)";

    @Override
    public String findPasswordByEmail(String email) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PASSWORD_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String pw = rs.getString("password_hash");
                    return pw;
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
}
