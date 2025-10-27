
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;

@WebServlet("/signup")
public class SignUpServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://shuttle.proxy.rlwy.net:24339/trivia_app";
    private static final String DB_USER = "backend_team";
    private static final String DB_PASSWORD = "BackTeam!123";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/signup.html");
        dispatcher.forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        String userId = request.getParameter("user_id");
        String password = request.getParameter("password");

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String checkSql = "SELECT COUNT(*) FROM users WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, userId);
                ResultSet rs = checkStmt.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count > 0) {
                    out.println("<h3>Email already registered!</h3>");
                    out.println("<a href='signup'>Try again</a>");
                    return;
                }
            }

            String insertSql = "INSERT INTO users (email, password) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                stmt.setString(1, userId);
                stmt.setString(2, password);
                stmt.executeUpdate();
            }
            response.sendRedirect("login");

        } catch (SQLException e) {
            e.printStackTrace();
            out.println("<h3>Database error:</h3>");
            out.println("<pre>" + e.getMessage() + "</pre>");
        }
    }
}

