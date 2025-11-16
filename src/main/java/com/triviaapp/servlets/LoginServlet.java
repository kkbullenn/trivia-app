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

        String identifier = request.getParameter("user_id");
        String password = request.getParameter("password");

        try {
            String storedHash = null;
            int userId = -1;

            if (identifier != null && identifier.contains("@")) {
                storedHash = userDAO.findPasswordByEmail(identifier);
                if (storedHash != null) {
                    userId = userDAO.findUserIdByEmail(identifier);
                }
            } else if (identifier != null) {
                storedHash = userDAO.findPasswordByUsername(identifier);
                if (storedHash != null) {
                    userId = userDAO.findUserIdByUsername(identifier);
                }
            }

            if (storedHash != null && BCrypt.checkpw(password, storedHash) && userId != -1) {

                HttpSession session = request.getSession(true);
                int roleId = userDAO.findUserRoleIdById(userId);
                String roleName = roleDAO.findRoleNameById(roleId);

                session.setAttribute("user_id", userId);
                session.setAttribute("role_name", roleName);
                response.sendRedirect(request.getContextPath() + "/main");


            } else {

                String error = URLEncoder.encode("Invalid email or password.", StandardCharsets.UTF_8);
                String remembered = identifier != null ? URLEncoder.encode(identifier, StandardCharsets.UTF_8) : "";
                String redirectUrl = request.getContextPath() + "/login?error=" + error;
                if (!remembered.isEmpty()) {
                    redirectUrl += "&user=" + remembered;
                }
                response.sendRedirect(redirectUrl);
            }


        } catch (SQLException e) {
            e.printStackTrace();
            throw new ServletException("Login failed due to a database error.", e);
        }
    }
}