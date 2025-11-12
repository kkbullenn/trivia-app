package com.triviaapp.dao.impl;

import com.triviaapp.dao.RoleDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class RoleDAOImpl implements RoleDAO {

    private static final String SQL_SELECT_ALL = "SELECT role_id, role_name FROM roles ORDER BY role_name";
    private static final String SQL_FIND_BY_ID = "SELECT role_name FROM roles WHERE role_id = ?";

    @Override
    public Map<Integer, String> findAllRoles() throws SQLException {
        Map<Integer, String> out = new LinkedHashMap<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("role_id");
                String name = rs.getString("role_name");
                out.put(id, name);
            }
        }
        return out;
    }

    @Override
    public String findRoleNameById(int roleId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role_name");
                }
            }
        }
        return null;
    }
}
