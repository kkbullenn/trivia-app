import java.io.IOException;

import com.triviaapp.dao.ModeratedAnswerDAO;
import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.impl.ModeratedAnswerDAOImpl;
import com.triviaapp.dao.impl.QuestionDAOImpl;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class QuizDataServlet extends HttpServlet {
    
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException, ServletException {

        // Check if user went through proper channels
        String ajaxHeader = request.getHeader("AJAX-Requested-With");
        if (ajaxHeader == null || !ajaxHeader.equals("fetch")) {
            response.sendRedirect("/quiz");
            return;
        }
        
        HttpSession session = request.getSession(false);
        int lobbyId = (Integer) session.getAttribute("lobby_id");

        // Create DAOs to fetch questions and answers
        QuestionDAO questionDAO = new QuestionDAOImpl();
        ModeratedAnswerDAO answerDAO = new ModeratedAnswerDAOImpl();

    }
}
