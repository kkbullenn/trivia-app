package com.triviaapp.websocket;

import com.triviaapp.dao.*;
import com.triviaapp.dao.impl.*;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for handling real-time quiz interactions. Handles joining lobbies,
 * navigating questions, submitting answers, and broadcasting updates such as questions
 * and leaderboards.
 */
@ServerEndpoint("/quiz/webSocket")
public class QuizWebSocket {

    // --- DAO dependencies for DB interaction ---
    // Handles quiz session state
    private static final SessionDAO SESSION_DAO = new SessionDAOImpl();
    // Fetches question data
    private static final QuestionDAO QUESTION_DAO = new QuestionDAOImpl();
    // Retrieves category names
    private static final CategoryDAO CATEGORY_DAO = new CategoryDAOImpl();
    // Stores and validates answers
    private static final ModeratedAnswerDAO MODERATED_ANSWER_DAO =
            new ModeratedAnswerDAOImpl();

    // --- WebSocket session tracking structures ---
    // Maps lobby_id → all WebSocket sessions (clients) currently in that lobby
    private static final Map<Integer, Set<Session>> LOBBY_SESSIONS =
            new ConcurrentHashMap<>();

    // Maps a WebSocket session → its associated lobby_id
    private static final Map<Session, Integer> SESSION_LOBBY_MAP =
            new ConcurrentHashMap<>();

    // Maps a WebSocket session → [user_id, username]
    private static final Map<Session, Object[]> SESSION_USER_MAP =
            new ConcurrentHashMap<>();

    // Maps lobby_id → current question index in that session
    private static final Map<Integer, Integer> LOBBY_CURRENT_QUESTION =
            new ConcurrentHashMap<>();


    /** Called when a new client connects to the WebSocket */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config)
    {
        System.out.println("WebSocket connected: " + session.getId());
    }

    /** Called when a message is received from a client */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException
    {
        JSONObject msg = new JSONObject(message);
        String type = msg.getString("type");
        Integer lobbyId = msg.has("lobbyId")
                          ? msg.getInt("lobbyId")
                          : SESSION_LOBBY_MAP.get(session);
        String username = msg.optString("username", "unknown");
        int userId = msg.optInt("user_id", -1);

        // Handle different message types
        switch(type)
        {
            // Player joins a lobby
            case "join":
                LOBBY_SESSIONS.putIfAbsent(lobbyId, ConcurrentHashMap.newKeySet());
                LOBBY_SESSIONS.get(lobbyId).add(session);
                SESSION_LOBBY_MAP.put(session, lobbyId);
                SESSION_USER_MAP.put(session, new Object[]{userId, username});
                // Update others that a new player joined
                broadcastLobbyInfo(lobbyId);
                break;

            // Moderator or player moves to next question
            case "next":
                handleNextQuestion(lobbyId);
                break;

            // Move to previous question
            case "prev":
                handlePrevQuestion(lobbyId);
                break;

            // Player submits an answer
            case "answer":
                String answer = msg.getString("answer");
                handlePlayerAnswer(session, lobbyId, answer);
                break;

            default:
                System.out.println("Unknown message type: " + type);
        }
    }

    /** Called when a client disconnects */
    @OnClose
    public void onClose(Session session)
    {
        Integer lobbyId = SESSION_LOBBY_MAP.get(session);
        Object[] userInfo = SESSION_USER_MAP.get(session);
        if(userInfo == null)
        {
            return;
        }

        int participantId = (int) userInfo[0];

        if(lobbyId != null)
        {
            // Remove session from the lobby set
            LOBBY_SESSIONS.getOrDefault(lobbyId, Set.of()).remove(session);
            try
            {
                // Remove user from DB session table
                SESSION_DAO.leaveSession(lobbyId, participantId);
            } catch(SQLException e)
            {
                e.printStackTrace();
            }

            // Notify others in the lobby that this user left
            broadcastToLobby(lobbyId, "User " + participantId + " left the lobby.");
        }

        // Clean up all maps
        SESSION_LOBBY_MAP.remove(session);
        SESSION_USER_MAP.remove(session);
        System.out.println("WebSocket closed: " + session.getId());
    }

    /** Called when an error occurs in the WebSocket */
    @OnError
    public void onError(Session session, Throwable throwable)
    {
        throwable.printStackTrace();
    }

    /** Move to the next question in the lobby and broadcast it */
    private void handleNextQuestion(Integer lobbyId)
    {
        try
        {
            Integer newIndex = SESSION_DAO.incrementAndGetCurrentIndex(lobbyId);
            if(newIndex != null)
            {
                sendQuestionToLobby(lobbyId, newIndex);
                LOBBY_CURRENT_QUESTION.put(lobbyId, newIndex);
            }
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /** Move to the previous question in the lobby and broadcast it */
    private void handlePrevQuestion(Integer lobbyId)
    {
        try
        {
            Integer newIndex = SESSION_DAO.decrementAndGetCurrentIndex(lobbyId);
            if(newIndex != null)
            {
                sendQuestionToLobby(lobbyId, newIndex);
                LOBBY_CURRENT_QUESTION.put(lobbyId, newIndex);
            }
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /** Process and store a player's submitted answer */
    private void handlePlayerAnswer(Session session, Integer lobbyId, String answer)
    {
        Object[] userInfo = SESSION_USER_MAP.get(session);
        if(userInfo == null || lobbyId == null)
        {
            return;
        }

        int participantId = (int) userInfo[0];
        String username = (String) userInfo[1];

        System.out.println(
                "Player " + username + " answered: " + answer + " in lobby " + lobbyId);

        try
        {
            Integer currentIndexObj = SESSION_DAO.getCurrentIndex(lobbyId);
            List<Integer> questionIds = SESSION_DAO.findQuestionIdsForSession(lobbyId);
            if(currentIndexObj == null || questionIds == null || questionIds.isEmpty())
            {
                return;
            }

            int currentIndex = currentIndexObj;
            if(currentIndex < 0 || currentIndex >= questionIds.size())
            {
                return;
            }

            int currentQuestionId = questionIds.get(currentIndex);
            Map<String, String> questionData = QUESTION_DAO.findQuestionById(currentQuestionId);
            if(questionData == null)
            {
                return;
            }

            String correctAnswerKey = questionData.get("answers_key");
            String categoryName = null;
            try
            {
                categoryName = CATEGORY_DAO.findCategoryNameById(
                        Integer.parseInt(questionData.get("category_id")));
            }
            catch(NumberFormatException ignored)
            {
            }

            Map<String, String> existingAnswer =
                    MODERATED_ANSWER_DAO.findAnswerForParticipantAndQuestion(lobbyId,
                            currentQuestionId, participantId);

            int totalQuestions = questionIds.size();
            int answeredCountBefore = MODERATED_ANSWER_DAO
                    .countAnswersForParticipantInSession(lobbyId, participantId);

            if(existingAnswer != null)
            {
                sendAnswerResult(session, lobbyId, currentQuestionId, currentIndex,
                        existingAnswer.get("selected_answer"), correctAnswerKey,
                        Boolean.parseBoolean(existingAnswer.get("is_correct")), true,
                        parseScore(existingAnswer.get("score")), answeredCountBefore,
                        totalQuestions, null, participantId);
                return;
            }

            boolean isCorrect = MODERATED_ANSWER_DAO.isAnswerCorrect(currentQuestionId, answer);
            int questionPoints = parseScore(questionData.get("points"));
            int score = isCorrect ? questionPoints : 0;

            MODERATED_ANSWER_DAO.createModeratedAnswer(lobbyId, currentQuestionId,
                    participantId, answer, isCorrect, score);

            List<Map<String, String>> leaderboard =
                    MODERATED_ANSWER_DAO.getSessionLeaderboard(lobbyId);
            broadcastLeaderboard(lobbyId, leaderboard);

            int answeredCountAfter = MODERATED_ANSWER_DAO
                    .countAnswersForParticipantInSession(lobbyId, participantId);

            sendAnswerResult(session, lobbyId, currentQuestionId, currentIndex, answer,
                    correctAnswerKey, isCorrect, false, score, answeredCountAfter,
                    totalQuestions, leaderboard, participantId);

            if(answeredCountAfter >= totalQuestions)
            {
                int totalScore = findParticipantScore(leaderboard, participantId);
                sendCompletion(session, lobbyId, totalQuestions, answeredCountAfter,
                        totalScore, leaderboard, categoryName);
            }

        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /** Sends a question payload to all players in the given lobby */
    private void sendQuestionToLobby(Integer lobbyId, int questionIndex)
    {
        try
        {
            List<Integer> questionIds = SESSION_DAO.findQuestionIdsForSession(lobbyId);
            if(questionIndex < 0 || questionIndex >= questionIds.size())
            {
                return;
            }

            int questionId = questionIds.get(questionIndex);
            Map<String, String> qData = QUESTION_DAO.findQuestionById(questionId);
            if(qData == null)
            {
                return;
            }

            JSONObject payload = new JSONObject();
            payload.put("type", "question");
            payload.put("index", questionIndex);
            payload.put("category_name",
                    CATEGORY_DAO.findCategoryNameById(
                            Integer.parseInt(qData.get("category_id"))));
            payload.put("question_text", qData.get("question_text"));
            payload.put("answers_option", qData.get("answers_option"));
            payload.put("answers_key", qData.get("answers_key"));
            payload.put("points", qData.get("points"));
            payload.put("youtube_url", qData.get("youtube_url"));
            payload.put("total_questions", questionIds.size());

            broadcastToLobby(lobbyId, payload.toString());

        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /** Retrieves the current question ID for a given lobby */
    private int getCurrentQuestionId(Integer lobbyId) throws SQLException
    {
        Integer currentIndex = SESSION_DAO.getCurrentIndex(lobbyId);
        List<Integer> questionIds = SESSION_DAO.findQuestionIdsForSession(lobbyId);
        if(currentIndex == null || questionIds.isEmpty())
        {
            return -1;
        }
        return questionIds.get(currentIndex);
    }

    /** Broadcasts player count and lobby info to everyone in the lobby */
    private void broadcastLobbyInfo(Integer lobbyId)
    {
        JSONObject payload = new JSONObject();
        payload.put("type", "lobbyInfo");
        payload.put("playerCount", LOBBY_SESSIONS.getOrDefault(lobbyId, Set.of()).size());
        broadcastToLobby(lobbyId, payload.toString());
    }

    /** Broadcasts the current leaderboard to all players in the lobby */
    private void broadcastLeaderboard(Integer lobbyId,
                                      List<Map<String, String>> leaderboard)
    {
        JSONObject payload = new JSONObject();
        payload.put("type", "leaderboard");
        payload.put("lobbyId", lobbyId);
        payload.put("leaderboard", toLeaderboardJson(leaderboard));
        broadcastToLobby(lobbyId, payload.toString());
    }

    /** Sends a message to all connected sessions in a lobby */
    private void broadcastToLobby(Integer lobbyId, String message)
    {
        Set<Session> sessions = LOBBY_SESSIONS.get(lobbyId);
        if(sessions != null)
        {
            for(Session s : sessions)
            {
                try
                {
                    s.getBasicRemote().sendText(message);
                } catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private void sendAnswerResult(Session session, Integer lobbyId, int questionId,
                                  int questionIndex, String selectedAnswer,
                                  String correctAnswer, boolean isCorrect,
                                  boolean alreadyAnswered, int scoreAwarded,
                                  int answeredCount, int totalQuestions,
                                  List<Map<String, String>> leaderboard,
                                  int participantId) throws IOException
    {
        JSONObject payload = new JSONObject();
        payload.put("type", "answerResult");
        payload.put("lobbyId", lobbyId);
        payload.put("question_id", questionId);
        payload.put("question_index", questionIndex);
        payload.put("selected_answer", selectedAnswer);
        if(correctAnswer != null)
        {
            payload.put("correct_answer", correctAnswer);
        }
        payload.put("is_correct", isCorrect);
        payload.put("already_answered", alreadyAnswered);
        payload.put("score_awarded", scoreAwarded);
        payload.put("answered_count", answeredCount);
        payload.put("total_questions", totalQuestions);
        payload.put("participant_id", participantId);
        if(leaderboard != null)
        {
            payload.put("leaderboard", toLeaderboardJson(leaderboard));
        }
        sendToSession(session, payload);
    }

    private void sendCompletion(Session session, Integer lobbyId, int totalQuestions,
                                int answeredCount, int totalScore,
                                List<Map<String, String>> leaderboard,
                                String categoryName) throws IOException
    {
        JSONObject payload = new JSONObject();
        payload.put("type", "quizComplete");
        payload.put("lobbyId", lobbyId);
        payload.put("total_questions", totalQuestions);
        payload.put("answered_count", answeredCount);
        payload.put("total_score", totalScore);
        payload.put("leaderboard", toLeaderboardJson(leaderboard));
        if(categoryName != null)
        {
            payload.put("category_name", categoryName);
        }
        sendToSession(session, payload);
    }

    private void sendToSession(Session session, JSONObject payload) throws IOException
    {
        if(session != null && session.isOpen())
        {
            session.getBasicRemote().sendText(payload.toString());
        }
    }

    private JSONArray toLeaderboardJson(List<Map<String, String>> leaderboard)
    {
        JSONArray arr = new JSONArray();
        if(leaderboard != null)
        {
            for(Map<String, String> entry : leaderboard)
            {
                arr.put(new JSONObject(entry));
            }
        }
        return arr;
    }

    private int findParticipantScore(List<Map<String, String>> leaderboard, int participantId)
    {
        if(leaderboard == null)
        {
            return 0;
        }
        for(Map<String, String> entry : leaderboard)
        {
            if(entry == null)
            {
                continue;
            }
            String pid = entry.get("participant_id");
            if(pid != null)
            {
                try
                {
                    if(Integer.parseInt(pid) == participantId)
                    {
                        return parseScore(entry.get("total_score"));
                    }
                }
                catch(NumberFormatException ignored)
                {
                }
            }
        }
        return 0;
    }

    private int parseScore(String value)
    {
        if(value == null)
        {
            return 0;
        }
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch(NumberFormatException ex)
        {
            return 0;
        }
    }
}
