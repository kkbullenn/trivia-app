import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.WebServlet;
import java.io.*;
import java.sql.*;


public class CreateQuizServlet extends HttpServlet {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/quizapp";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "password"; // change this when server is set up

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        RequestDispatcher dispatcher = request.getRequestDispatcher("/admin_landing.html");
        dispatcher.forward(request, response);
    }

@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
    }

}
