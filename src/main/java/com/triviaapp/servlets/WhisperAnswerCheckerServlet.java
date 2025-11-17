package com.triviaapp.servlets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;
import com.triviaapp.externalapi.WhisperConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * Utilizes Whisper transcription to evaluate spoken quiz answers against stored solutions.
 *
 * @author Samarjit Bhogal
 */
@MultipartConfig
public class WhisperAnswerCheckerServlet extends HttpServlet {
    private static final WhisperConnection CONNECTION = new WhisperConnection();
    private static final double MATCH_RESULT_EPSILON = 0.001;

    /**
     * Transcribes and checks if the answer is correct for the given question.
     *
     * @param request  multipart form-data containing the file and the question_id of the question to check answers for.
     * @param response returns a json body with a message of "YES" or "NO" to simply say if the answer was correct or not.
     *                 // Success:
     *                 {
     *                 status: "success",
     *                 playerAnswerKey: "A", (1-1 with db)
     *                 playerAnswer: "test answer here.", (1-1 with db)
     *                 transcribedAnswer: "From Whisper Service Directly.",
     *                 actualAnswerKey: "B",
     *                 actualAnswer: "Blah Blah..."
     *                 }
     *                 <p>
     *                 Fail:
     *                 {
     *                 status: "fail",
     *                 message: "Could not detect your answer. Please try again."
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

        // get question info from db
        final String answerKey = question.get("answers_key"); // actual answer key
        final String answersOptionStr = question.get("answers_option"); // array of json object with: key & text

        if (answersOptionStr == null) {
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

        final JSONArray optionsArray = new JSONArray(answersOptionStr); // array of json object with: key & text
        final List<String> answerTexts = new ArrayList<>();
        final List<String> answerKeys = new ArrayList<>();
        String actualAnswer = "";

        for (final Object options : optionsArray) {
            final JSONObject option = (JSONObject) options;
            final String text = option.get("text").toString();
            final String key = option.get("key").toString();

            answerTexts.add(text);
            answerKeys.add(key);

            if (key.equalsIgnoreCase(answerKey)) {
                actualAnswer = text;
            }
        }

        answerTexts.addAll(answerKeys);

        // get translated text (could be option key, could be answer text, could be both)
        final String transcribedText = whisperJson.get("translated_text").asText();

        // send to a model to depict if the user is any amount of correct (YES or NO).
        final JSONArray result = WhisperConnection.getMatchResult(transcribedText, answerTexts);

        // figure out with result which db answer/key is the closest
        // percentages are organized in order of answerTexts + {A, B, C, D}
        final List<Double> probs = new ArrayList<>();

        for(int i = 0; i < result.length(); i++) {
            final double p = result.getDouble(i);
            probs.add(p);
        }

        final double maxProb = probs.stream().max(Double::compareTo).orElseThrow();

        if (maxProb + MATCH_RESULT_EPSILON < 0.50) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            final String data = "{"
                    + "\"status\": \"error\","
                    + "\"message\": \"Could not detect your answer. Please try again.\""
                    + "}";
            resWriter.write(data);
            return;
        }

        final int probIndex = probs.indexOf(maxProb);

        final String playerAnswerKey;
        final String playerAnswer;

        if (probIndex >= 4) {
            // a key, find answer at index of key
            playerAnswerKey = answerKeys.get(probIndex % 4);
            playerAnswer = answerTexts.get(answerKeys.indexOf(playerAnswerKey));
        } else {
            // a text, find key and index of answer
            playerAnswer = answerTexts.get(probIndex % 4);
            playerAnswerKey = answerKeys.get(answerTexts.indexOf(playerAnswer));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final String data = "{"
                + "\"status\": \"success\","
                + "\"playerAnswerKey\": \"" + playerAnswerKey + "\","
                + "\"playerAnswer\": \"" + playerAnswer + "\","
                + "\"transcribedAnswer\": \"" + transcribedText + "\","
                + "\"actualAnswerKey\": \"" + answerKey + "\","
                + "\"actualAnswer\": \"" + actualAnswer + "\""
                + "}";
        resWriter.write(data);
    }
}
