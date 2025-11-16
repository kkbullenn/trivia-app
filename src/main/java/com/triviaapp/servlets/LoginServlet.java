package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.dao.RoleDAO;
import com.triviaapp.dao.impl.RoleDAOImpl;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

/**
 * Processes login requests, verifies credentials, and initializes user sessions.
 *
 * @author Timothy Kim
 * @author Brownie Tran
 * @author Jerry Xing
 */
public class LoginServlet extends HttpServlet {


    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/login.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        final UserDAO userDAO = new UserDAOImpl();
        final RoleDAO roleDAO = new RoleDAOImpl();

        String email = request.getParameter("user_id");
        String password = request.getParameter("password");

        try {
            String userPasswordByEmail = userDAO.findPasswordByEmail(email);


            if (userPasswordByEmail != null && BCrypt.checkpw(password, userPasswordByEmail)) {

                // Successful login
                HttpSession session = request.getSession(true);
                int userId = userDAO.findUserIdByEmail(email);
                int roleId = userDAO.findUserRoleIdById(userId);
                String roleName = roleDAO.findRoleNameById(roleId);

                if (userId == -1) {
                    throw new SQLException("Database inconsistency: User found but ID not found.");
                } else {
                    session.setAttribute("user_id", userId);
                    session.setAttribute("role_name", roleName);
                    response.sendRedirect(request.getContextPath() + "/main");
                }


            } else {

                String error = URLEncoder.encode("Invalid email or password.", StandardCharsets.UTF_8);
                String remembered = email != null ? URLEncoder.encode(email, StandardCharsets.UTF_8) : "";
                String redirectUrl = request.getContextPath() + "/login?error=" + error;
                if (!remembered.isEmpty()) {
                    redirectUrl += "&user=" + remembered;
                }
                response.sendRedirect(redirectUrl);
                return;


            }


        } catch (SQLException e) {
            e.printStackTrace();
            throw new ServletException("Login failed due to a database error.", e);
        }
    }
}