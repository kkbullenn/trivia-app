package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles user registration by validating inputs, hashing passwords, and persisting accounts.
 *
 * @author Timothy Kim
 * @author Jerry Xing
 */
public class SignUpServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SignUpServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/signUp.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {

        final UserDAO userDAO = new UserDAOImpl();


        String email = request.getParameter("user_id");
        String password = request.getParameter("password");
        String roleParam = request.getParameter("role_id");
        int roleId = roleParam != null
                     ? Integer.parseInt(roleParam)
                     : 100;

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try
        {

            if(userDAO.findPasswordByEmail(email) != null)
            {
                String error = URLEncoder.encode("Email already registered. Would you like to log in instead?",
                        StandardCharsets.UTF_8);
                String rememberedEmail = email != null
                                         ? URLEncoder.encode(email, StandardCharsets.UTF_8)
                                         : "";
                String rememberedRole = URLEncoder.encode(String.valueOf(roleId), StandardCharsets.UTF_8);
                String redirectUrl = request.getContextPath() + "/signUp?error=" + error;
                if(!rememberedEmail.isEmpty())
                {
                    redirectUrl += "&email=" + rememberedEmail;
                }
                if(!rememberedRole.isEmpty())
                {
                    redirectUrl += "&role=" + rememberedRole;
                }
                response.sendRedirect(redirectUrl);
                return;
            }
            // generate username based on the email input
            String userName = email.substring(0, email.indexOf('@'));
            //hashing password for secure the user password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            boolean success = userDAO.createUser(userName, email, hashedPassword, roleId);

            if(success)
            {
                response.sendRedirect("login");
            } else
            {
                out.println("<h3>Sign up failed unexpectedly.</h3>");
            }

        } catch(SQLException e)
        {
            LOGGER.log(Level.SEVERE, "Signup failed due to a database error for email: " + email, e);
            throw new ServletException("signup failed due to a database error.", e);
        }
    }
}
