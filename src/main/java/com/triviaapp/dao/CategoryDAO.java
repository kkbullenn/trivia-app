package com.triviaapp.dao;

import java.sql.SQLException;
import java.util.Map;

/**
 * DAO for categories. Provides methods useful for simple UI lists (dropdowns).
 */
public interface CategoryDAO {

    /**
     * Return all categories as a map of id -> name. Implementation may order by display_order then name.
     */
    Map<Integer, String> findAllCategories() throws SQLException;

    /**
     * Find category name by id, or null if not found.
     */
    String findCategoryNameById(int categoryId) throws SQLException;
}
