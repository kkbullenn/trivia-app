import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale.Category;
import java.util.Map;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/main")
public class MainServlet extends HttpServlet {

    // private static final String DB_URL = String.format(
    // "jdbc:mysql://%s:%s/%s",
    // System.getenv("MYSQLHOST"),
    // System.getenv("MYSQLPORT"),
    // System.getenv("MYSQLDATABASE")
    // );
    // private static final String DB_USER = System.getenv("MYSQLUSER");
    // private static final String DB_PASSWORD = System.getenv("MYSQLPASSWORD");
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
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

        // Build JSON array using org.json
        JSONArray jsonArray = new JSONArray();
        for (Map.Entry<Integer, String> entry : categories.entrySet()) {
            JSONObject obj = new JSONObject();
            obj.put("id", entry.getKey());
            obj.put("name", entry.getValue());
            jsonArray.put(obj);
        }
        
        // Return JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonArray.toString());

        RequestDispatcher dispatcher = request.getRequestDispatcher("/select-quiz.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        // Will implement later if needed
    }
}
