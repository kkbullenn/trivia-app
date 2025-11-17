package com.triviaapp.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import com.triviaapp.externalapi.M2M100Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TranslatedQuizServlet
 *
 * This servlet is responsible for:
 *  1. Reading quiz questions from the DB for a given category
 *  2. Sending all question texts and answer options to the Python M2M100 translation service
 *  3. Rebuilding a translated JSON structure that the Front End (FE) can consume directly
 *
 * HOW FRONT END SHOULD CALL THIS :
 *
 *   Method: GET
 *   URL:   /trivia-app/quiz/translated
 *
 *   Required query parameter:
 *     - categoryId (int)  : which question category to load
 *
 *   Optional query parameter:
 *     - target (string)   : target language code, e.g. "tl", "es", "fr"
 *                           default = "tl" (Tagalog) if not provided or blank
 *
 *   Example calls:
 *     GET /trivia-app/quiz/translated?categoryId=1
 *     GET /trivia-app/quiz/translated?categoryId=1&target=tl
 *     GET /trivia-app/quiz/translated?categoryId=1&target=es
 *
 * 
 * RESPONSE SHAPE (USED BY FE) :
 *
 * On success (HTTP 200), the response is a JSON array of question objects:
 *
 * [
 *   {
 *     "question_id": 1,          // numeric DB id of the question
 *     "category_id": 1,          // numeric DB id of the category
 *     "text": "Translated question",     // translated question text (based on target=...)
 *     "original_text": "Original question", // original English text from DB
 *     "correct_key": "A",        // which option key is correct ("A", "B", ...)
 *     "youtube_url": "https://...", // YouTube URL if any, otherwise null/empty
 *     "points": 10,              // point value for the question
 *     "options": [
 *       {
 *         "key": "A",                         // option key
 *         "text": "Translated answer A",      // translated option text
 *         "original_text": "Original answer A"// original option text from DB
 *       },
 *       {
 *         "key": "B",
 *         "text": "Translated answer B",
 *         "original_text": "Original answer B"
 *       }
 *       // ...
 *     ]
 *   },
 *   // ... other questions
 * ]
 *
 * 
 * ERROR RESPONSES :
 *
 *   HTTP 400 (Bad Request):
 *     - Missing categoryId:
 *       { "error": "categoryId is required" }
 *     - Invalid categoryId (not an int):
 *       { "error": "categoryId must be integer" }
 *
 *   HTTP 500 (Server Error):
 *     - DB error:
 *       { "error": "DB error: <message>" }
 *     - Translation service error:
 *       { "error": "translation failed: <message>" }
 *
 * 
 * MANUAL TESTING NOTE :
 *   http://localhost:8081/trivia-app/quiz/translated?categoryId=1&target=tl
 *
 * @author Aira Bassig
 */
@WebServlet(name = "TranslatedQuizServlet", urlPatterns = {"/quiz/translated"})
public class TranslatedQuizServlet extends HttpServlet {

    // DAO used to fetch questions + options from the database
    private final QuestionDAO questionDAO = new QuestionDAOImpl();

    // Client wrapper for the Python M2M100 translation REST API
    private final M2M100Connection translator =
            new M2M100Connection("http://localhost:8892/m2m100/translate/questions-answers");

    /**
     * Helper class to carry all data needed for the translation request:
     * - questionsJson:                 array of question texts only
     * - answersJson:                   flattened list of all answer option texts
     * - optionCounts:                  how many options each question has
     * - originalOptionsPerQuestion:    raw options JSON per question, for reconstruction
     */
    private static class TranslationBatch {
        JSONArray questionsJson;                    // ["Q1 text", "Q2 text", ...]
        JSONArray answersJson;                      // ["A1 text", "A2 text", ...] across all questions
        List<Integer> optionCounts;                 // per-question option count, e.g. [4,4,4,...]
        List<JSONArray> originalOptionsPerQuestion; // per-question options as JSON arrays
    }

    /**
     * Handles GET /quiz/translated
     *
     * Flow:
     *  1) Read categoryId, target language
     *  2) Query DB for questions in that category
     *  3) Build translation batch payload (questions and all options)
     *  4) Call Python translation service
     *  5) Rebuild FE-friendly JSON and send it back
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) Parse and validate categoryId
        Integer categoryId = parseCategoryId(req, resp);
        if (categoryId == null) {
            // parseCategoryId already sent 400 JSON error response
            return;
        }

        // 2) Determine target translation language (default "tl")
        String targetLang = resolveTargetLang(req);

        // 3) Fetch questions from the DB for this category
        List<Map<String, String>> questions;
        try {
            questions = questionDAO.findQuestionsByCategory(categoryId);
        } catch (SQLException e) {
            e.printStackTrace();
            sendJson(resp, 500, new JSONObject().put("error", "DB error: " + e.getMessage()));
            return;
        }

        // 4) If DB has no questions for this category, just test translation service with dummy data
        if (questions == null || questions.isEmpty()) {
            handleEmptyQuestions(resp, categoryId, targetLang);
            return;
        }

        // 5) Build translation batch payload from DB rows
        TranslationBatch batch = buildTranslationBatch(questions);

        // Payload sent to Python:
        // {
        //   "questions": ["Q1", "Q2", ...],
        //   "answers":   ["Q1A", "Q1B", "Q2A", ...],
        //   "source":    "en",
        //   "target":    "<targetLang>"
        // }
        JSONObject payload = new JSONObject();
        payload.put("questions", batch.questionsJson);
        payload.put("answers", batch.answersJson);
        payload.put("source", "en");
        payload.put("target", targetLang);

        // 6) Call Python M2M100 translation service
        JSONObject pyResp;
        try {
            pyResp = translator.translateBatch(payload);
        } catch (Exception e) {
            // Translation service failed (e.g., connection refused, timeout, etc.)
            sendJson(resp, 500, new JSONObject().put("error", "translation failed: " + e.getMessage()));
            return;
        }

        // 7) Rebuild final FE-friendly JSON from original data and translated texts
        JSONArray out = buildFinalResponseJson(
                questions,
                categoryId,
                batch.optionCounts,
                batch.originalOptionsPerQuestion,
                pyResp
        );

        // 8) Send back the JSON array to FE (or browser if manually testing)
        sendJson(resp, 200, out);
    }

    /**
     * Parse and validate "categoryId" from the query string.
     *
     * Example:
     *   /quiz/translated?categoryId=1
     *
     * If invalid, sends a 400 JSON error:
     *   { "error": "categoryId is required" }
     *   { "error": "categoryId must be integer" }
     *
     * @return Integer categoryId or null if invalid (and error already sent)
     */
    private Integer parseCategoryId(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String categoryIdParam = req.getParameter("categoryId");
        if (categoryIdParam == null) {
            sendJson(resp, 400, new JSONObject().put("error", "categoryId is required"));
            return null;
        }
        try {
            return Integer.parseInt(categoryIdParam);
        } catch (NumberFormatException e) {
            sendJson(resp, 400, new JSONObject().put("error", "categoryId must be integer"));
            return null;
        }
    }

    /**
     * Resolve "target" language from query param.
     *
     * If not provided or blank, default to Tagalog ("tl").
     *
     * Examples:
     *   ?target=tl  -> "tl"
     *   ?target=es  -> "es"
     *   (missing)   -> "tl"
     */
    private String resolveTargetLang(HttpServletRequest req) {
        String target = req.getParameter("target");
        return (target == null || target.isBlank()) ? "tl" : target;
    }

    /**
     * If there are no questions for the given category in the DB,
     * we still want to confirm that the translation pipeline works.
     *
     * So we send a small dummy payload to the Python service and return the
     * translated result directly to the caller.
     */
    private void handleEmptyQuestions(HttpServletResponse resp, int categoryId, String targetLang)
            throws IOException {

        System.out.println("No questions for category " + categoryId + ", using test payload.");

        // Dummy payload: just a single question and two answers
        JSONObject payload = new JSONObject();
        payload.put("questions", new JSONArray().put("Hello World"));
        payload.put("answers", new JSONArray().put("Red").put("Blue"));
        payload.put("source", "en");
        payload.put("target", targetLang);

        try {
            JSONObject pyResp = translator.translateBatch(payload);
            // Directly return Python's response so we can inspect it
            sendJson(resp, 200, pyResp);
        } catch (Exception e) {
            sendJson(resp, 500, new JSONObject().put("error", "translation failed: " + e.getMessage()));
        }
    }

    /**
     * Build the batch payload that will be sent to the Python translation service.
     *
     * For each DB row (question), we:
     *   - Add question_text to questionsJson
     *   - Parse answers_option JSON to extract options
     *   - Add each option text to answersJson (flattened)
     *   - Store metadata so we can reconstruct per-question options later
     *
     * The DB can store answers_option in formats like:
     *   1) [ { "key": "A", "text": "..." }, ... ]
     *   2) { "options": [ { "key": "...", "text": "..." }, ... ] }
     *   3) { "A": "1", "B": "2", ... }  (we convert this to the array format)
     */
    private TranslationBatch buildTranslationBatch(List<Map<String, String>> questions) {

        TranslationBatch batch = new TranslationBatch();
        batch.questionsJson = new JSONArray();
        batch.answersJson = new JSONArray();
        batch.optionCounts = new ArrayList<>();
        batch.originalOptionsPerQuestion = new ArrayList<>();

        for (Map<String, String> qMap : questions) {
            // 1) Question text
            String questionText = qMap.get("question_text");
            batch.questionsJson.put(questionText != null ? questionText : "");

            // 2) Answer options JSON string from DB
            String answersOptionJson = qMap.get("answers_option");

            // If no options stored, treat as zero options
            if (answersOptionJson == null || answersOptionJson.isBlank()) {
                batch.optionCounts.add(0);
                batch.originalOptionsPerQuestion.add(new JSONArray());
                continue;
            }

            String trimmed = answersOptionJson.trim();

            try {
                JSONArray optsJson;

                if (trimmed.startsWith("[")) {
                    // Case 1: Already an array of objects: [ { "key": "A", "text": "..." }, ... ]
                    optsJson = new JSONArray(trimmed);

                } else if (trimmed.startsWith("{")) {
                    // Case 2: Maybe one of:
                    //   (a) { "options": [ { "key": "...", "text": "..." }, ... ] }
                    //   (b) { "A": "1", "B": "2", ... } - key : text map
                    JSONObject obj = new JSONObject(trimmed);

                    if (obj.has("options")) {
                        // (a) "options" array is already available
                        optsJson = obj.getJSONArray("options");
                    } else {
                        // (b) Convert { "A": "1", "B": "2" } to [ {key: "A", text:"1"}, ... ]
                        System.out.println("!!! Converting key-value object to options array, question_id="
                                + qMap.get("question_id") + ": " + trimmed);

                        optsJson = new JSONArray();
                        for (String key : obj.keySet()) {
                            JSONObject optObj = new JSONObject();
                            optObj.put("key", key);
                            optObj.put("text", obj.getString(key));
                            optsJson.put(optObj);
                        }
                    }

                } else {
                    // Unexpected format (e.g., plain text, XML, etc.)
                    System.out.println("!!! Non-JSON answers_option for question_id="
                            + qMap.get("question_id") + ": " + trimmed);
                    batch.optionCounts.add(0);
                    batch.originalOptionsPerQuestion.add(new JSONArray());
                    continue;
                }

                // If we reach here, optsJson is a valid array of options
                batch.originalOptionsPerQuestion.add(optsJson);
                batch.optionCounts.add(optsJson.length());

                // For translation: we only send the "text" values of each option
                for (int i = 0; i < optsJson.length(); i++) {
                    JSONObject optObj = optsJson.getJSONObject(i);
                    batch.answersJson.put(optObj.getString("text"));
                }

            } catch (Exception ex) {
                // Parsing failed for this question's options; log it and treat as no options
                System.out.println("XXX Failed to parse answers_option for question_id="
                        + qMap.get("question_id") + ": " + trimmed + " (" + ex.getMessage() + ")");
                batch.optionCounts.add(0);
                batch.originalOptionsPerQuestion.add(new JSONArray());
            }
        }

        return batch;
    }

    /**
     * Rebuild the final response as a JSON array of questions, ready for FE.
     *
     * We combine:
     *   - original DB data (ids, original text, correct_key, youtube, points)
     *   - per-question options metadata (optionCounts, originalOptsPerQ)
     *   - translated questions and answers returned by Python
     *
     * Python response is expected to have:
     *   - questions_translated: array of translated question texts
     *   - answers_translated:   flat array of translated option texts
     */
    private JSONArray buildFinalResponseJson(
            List<Map<String, String>> questions,
            int defaultCategoryId,
            List<Integer> optionCounts,
            List<JSONArray> originalOptsPerQ,
            JSONObject pyResp
    ) {
        // Translated questions/answers returned by Python
        JSONArray qTranslated = pyResp.getJSONArray("questions_translated");
        JSONArray aTranslated = pyResp.getJSONArray("answers_translated");

        JSONArray out = new JSONArray();
        int ansIdx = 0; // index into flattened translated answers

        for (int qIdx = 0; qIdx < questions.size(); qIdx++) {
            Map<String, String> qMap = questions.get(qIdx);

            JSONObject qJson = new JSONObject();

            // Raw DB fields (strings)
            String qIdStr = qMap.get("question_id");
            String catIdStr = qMap.get("category_id");
            String originalText = qMap.get("question_text");
            String answersKey = qMap.get("answers_key");
            String youtubeUrl = qMap.get("youtube_url");
            String pointsStr = qMap.get("points");

            // Convert to numeric where needed, with safe defaults
            int qId = (qIdStr != null && !qIdStr.isEmpty()) ? Integer.parseInt(qIdStr) : 0;
            int catId = (catIdStr != null && !catIdStr.isEmpty()) ? Integer.parseInt(catIdStr) : defaultCategoryId;
            int points = (pointsStr != null && !pointsStr.isEmpty()) ? Integer.parseInt(pointsStr) : 0;

            // Core question object for FE
            qJson.put("question_id", qId);
            qJson.put("category_id", catId);
            qJson.put("text", qTranslated.getString(qIdx)); // translated question text
            qJson.put("original_text", originalText);       // original English question
            qJson.put("correct_key", answersKey);
            qJson.put("youtube_url", youtubeUrl);
            qJson.put("points", points);

            // Options reconstruction
            int optCount = optionCounts.get(qIdx);
            JSONArray originalOpts = originalOptsPerQ.get(qIdx);
            JSONArray rebuiltOpts = new JSONArray();

            for (int i = 0; i < optCount; i++) {
                JSONObject origOpt = originalOpts.getJSONObject(i);
                String key = origOpt.getString("key");
                String translatedText = aTranslated.getString(ansIdx++); // next translated option

                JSONObject newOpt = new JSONObject();
                newOpt.put("key", key);
                newOpt.put("text", translatedText);                  // translated option text
                newOpt.put("original_text", origOpt.optString("text", "")); // original option text

                rebuiltOpts.put(newOpt);
            }

            // Attach options array to the question
            qJson.put("options", rebuiltOpts);

            // Add question object to output array
            out.put(qJson);
        }

        return out;
    }

    /**
     * Utility: send a JSON object as HTTP response.
     *
     * Example:
     *   sendJson(resp, 400, new JSONObject().put("error", "something wrong"));
     */
    private void sendJson(HttpServletResponse resp, int status, JSONObject obj) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.getWriter().write(obj.toString());
    }

    /**
     * Utility: send a JSON array as HTTP response.
     *
     * Used mainly for the success case where we return an array of questions.
     */
    private void sendJson(HttpServletResponse resp, int status, JSONArray arr) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");
        resp.getWriter().write(arr.toString());
    }
}
