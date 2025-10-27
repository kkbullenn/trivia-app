package com.triviaapp.dao;

import java.sql.SQLException;

/**
 * DAO for user authentication.
 */
public interface UserDAO {
    /**
     * Return the password for the given email if present, or null when not found.
     */
    String findPasswordByEmail(String email) throws SQLException;

    /**
     * Create a new user with given username, email and password.
     */
    boolean createUser(String username, String email, String password, int roleId) throws SQLException;
}
