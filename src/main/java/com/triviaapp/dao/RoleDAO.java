package com.triviaapp.dao;

import java.sql.SQLException;
import java.util.Map;

/**
 * DAO for roles. Returns a map of role_id -> role_name.
 */
public interface RoleDAO {

    /**
     * Return all roles as a map of id -> name.
     */
    Map<Integer, String> findAllRoles() throws SQLException;

    /**
     * Find role name by id, or null if not found.
     */
    String findRoleNameById(int roleId) throws SQLException;
}
