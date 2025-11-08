package com.triviaapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.triviaapp.dao.CategoryDAO;
import com.triviaapp.dao.impl.CategoryDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class MainDataServlet extends HttpServlet {

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            // Not logged in, redirect to  login page
            response.sendRedirect("login");
            return;
        }

        // Create DAO object to grab available categories from database
        CategoryDAO categoryDAO = new CategoryDAOImpl();
        Map<Integer, String> categories;
        try {
            categories = categoryDAO.findAllCategories();
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        // Build JSON array for categories using org.json
        JSONArray categoriesArray = new JSONArray();
        for (Map.Entry<Integer, String> entry : categories.entrySet()) {
            JSONObject obj = new JSONObject();
            obj.put("category-id", entry.getKey());
            obj.put("category-name", entry.getValue());
            categoriesArray.put(obj);
        }
        
        // Return JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.write(categoriesArray.toString());
        out.flush();
    }
}
