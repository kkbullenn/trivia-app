package com.triviaapp.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import com.triviaapp.externalapi.WhisperConnection;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;

/**
 * Utilizes Whisper transcription to evaluate spoken quiz answers against stored solutions.
 *
 * @author Samarjit Bhogal
 */
@MultipartConfig
public class WhisperAnswerCheckerServlet extends HttpServlet {
    private static final WhisperConnection CONNECTION = new WhisperConnection();

    /**
     * Transcribes and checks if the answer is correct for the given question.
     *
     * @param request  multipart form-data containing the file and the question_id of the question to check answers for.
     * @param response returns a json body with a message of "YES" or "NO" to simply say if the answer was correct or not.
     *                 {
     *                 "status": "success",
     *                 "message": "YES"
     *                 }
     */
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.sendRedirect("/trivia-app/login");
            return;
        }

        final ObjectMapper mapper = new ObjectMapper();
        final PrintWriter resWriter = response.getWriter();

        // get all parts + request verification
        final String fileName;
        final int questionId;
        final byte[] audioBytes;

        try {
            final Part questionIdPart = request.getPart("question_id");
            final Part filePart = request.getPart("file");

            if (questionIdPart == null || filePart == null) {
                throw new ServletException();
            }

            fileName = filePart.getSubmittedFileName();
            questionId = Integer.parseInt(new String(questionIdPart.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
            audioBytes = filePart.getInputStream().readAllBytes();
        } catch (final ServletException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            final String data = "{"
                    + "\"status\": \"error\","
                    + "\"message\": \"Expected 'question_id' and 'file' in multipart/form-data.\""
                    + "}";
            resWriter.write(data);
            return;
        }

        // transcribe audio
        final HttpURLConnection connection = (HttpURLConnection) CONNECTION.getPostURL().openConnection();
        final InputStream whisperInputStream = WhisperConnection.getTranscription(audioBytes, fileName, connection);
        final JsonNode whisperJson = mapper.readTree(whisperInputStream);

        // Example expected Whisper JSON:
        // {
        //   "detected_language": "en",
        //   "duration_sec": 8.96,
        //   "translated_text": "Hello world"
        // }

        // get question and answers from database of this quiz (specifically answers_option & answers_key)
        final Map<String, String> question;

        try {
            final QuestionDAO questionDAO = new QuestionDAOImpl();
            question = questionDAO.findQuestionById(questionId);

            if (question == null) {
                throw new SQLException();
            }
        } catch (final SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            final String data = "{"
                    + "\"status\": \"error\","
                    + "\"message\": \"Question could not be transcribed as it does not exist.\""
                    + "}";
            resWriter.write(data);
            return;
        }

        // get question info
        final String answerKey = question.get("answers_key");
        String answerText = null;

        final String answersOptionStr = question.get("answers_option");
        final JsonNode answerArray = mapper.readTree(answersOptionStr);

        if (answerArray.isArray()) {
            for (final JsonNode obj : answerArray) {
                if (obj != null) {
                    final String key = obj.get("key").asText();

                    if (answerKey.equalsIgnoreCase(key)) {
                        final String text = obj.get("text").asText();

                        if (text != null) {
                            answerText = text;
                        }
                    }
                }
            }
        }

        if (answerText == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            final String data = "{"
                    + "\"status\": \"error\","
                    + "\"message\": \"Question didn't have a legible answer.\""
                    + "}";
            resWriter.write(data);
            return;
        }


        // get translated text
        final String translatedText = whisperJson.get("translated_text").asText();

        // send to a model to depict if the user is any amount of correct (YES or NO).
        final String result = WhisperConnection.getMatchResult(translatedText, answerText);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final String data = "{"
                + "\"status\": \"success\","
                + "\"message\": \"" + result + "\""
                + "}";
        resWriter.write(data);
    }
}
