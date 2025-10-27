import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;
import java.util.*;

@WebServlet("/main")
public class MainServlet extends HttpServlet {

    private static final String DB_URL = String.format(
    "jdbc:mysql://%s:%s/%s",
    System.getenv("MYSQLHOST"),
    System.getenv("MYSQLPORT"),
    System.getenv("MYSQLDATABASE")
    );

    private static final String DB_USER = System.getenv("MYSQLUSER");
    private static final String DB_PASSWORD = System.getenv("MYSQLPASSWORD");
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {
        
        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("login");
            return;
        }

        // Grab available categories from database
        List<Map<String, String>> categories = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String sql = "SELECT category_id, name FROM categories ORDER BY display_order ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    Map<String, String> category = new HashMap<>();
                    category.put("id", rs.getString("category_id"));
                    category.put("name", rs.getString("name"));
                    categories.add(category);
                }
                stmt.close();
            }
            
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
            return;
        }

        RequestDispatcher dispatcher = request.getRequestDispatcher("/select-quiz.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
    }
}
