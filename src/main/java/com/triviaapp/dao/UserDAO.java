package com.triviaapp.dao;

import java.sql.SQLException;

/**
 * DAO for user authentication.
 *
 * @author Haven Zhang
 * @author Brownie Tran
 * @author Timothy Kim
 * @author Jerry Xing
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
    
    /**
     *
     * Return the User_ID for the given email
     * */
    int findUserIdByEmail(String email) throws SQLException;
    
    /**
     * Return user role ID for the given user ID
     */
    int findUserRoleIdById(int userId) throws SQLException;

    /**
     * Return the username for the given user ID.
     */
    String findUsernameById(int userId) throws SQLException;
}
