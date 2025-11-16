package com.triviaapp.dao.impl;

import com.triviaapp.dao.CategoryDAO;
import com.triviaapp.util.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides JDBC implementations for category-related queries used by the UI layer.
 *
 * @author Haven Zhang
 */
public class CategoryDAOImpl implements CategoryDAO {

    private static final String SQL_SELECT_ALL = "SELECT category_id, name FROM categories ORDER BY display_order, name";
    private static final String SQL_FIND_BY_ID = "SELECT name FROM categories WHERE category_id = ?";

    @Override
    public Map<Integer, String> findAllCategories() throws SQLException {
        Map<Integer, String> out = new LinkedHashMap<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("category_id");
                String name = rs.getString("name");
                out.put(id, name);
            }
        }
        return out;
    }

    @Override
    public String findCategoryNameById(int categoryId) throws SQLException {
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        return null;
    }
}
