package com.triviaapp.servlets;

import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 * Handles password update requests for authenticated users.
 *
 * @author Jerry Xing
 */
public class UserPasswordServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("user_id") == null)
        {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not logged in");
            return;
        }

        JSONObject payload = RequestUtils.readJsonPayload(request, response);
        if(payload == null)
        {
            return;
        }

        String oldPassword = payload.optString("old_password", "").trim();
        String newPassword = payload.optString("new_password", "").trim();
        if(oldPassword.isEmpty() || newPassword.isEmpty())
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required fields");
            return;
        }
        if(oldPassword.equals(newPassword))
        {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "New password must differ from current password");
            return;
        }

        UserDAO userDAO = new UserDAOImpl();
        int userId = (Integer) session.getAttribute("user_id");

        try
        {
            String currentHash = userDAO.findPasswordHashById(userId);
            if(currentHash == null || !BCrypt.checkpw(oldPassword, currentHash))
            {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                JSONObject error = new JSONObject();
                error.put("success", false);
                error.put("message", "Current password is incorrect.");
                writeJson(response, error);
                return;
            }

            String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            boolean updated = userDAO.updatePassword(userId, newHash);
            if(!updated)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to update password");
                return;
            }

            JSONObject success = new JSONObject();
            success.put("success", true);
            writeJson(response, success);
        } catch(SQLException ex)
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    private void writeJson(HttpServletResponse response, JSONObject json) throws IOException
    {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try(PrintWriter out = response.getWriter())
        {
            out.write(json.toString());
        }
    }
}
