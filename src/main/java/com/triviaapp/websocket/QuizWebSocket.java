package com.triviaapp.websocket;

import com.triviaapp.dao.*;
import com.triviaapp.dao.impl.*;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for handling real-time quiz interactions.
 * Handles joining lobbies, navigating questions, submitting answers,
 * and broadcasting updates such as questions and leaderboards.
 */
@ServerEndpoint("/quiz/webSocket")
public class QuizWebSocket {

    // --- DAO dependencies for DB interaction ---
    private static final SessionDAO sessionDAO = new SessionDAOImpl();              // Handles quiz session state
    private static final QuestionDAO questionDAO = new QuestionDAOImpl();           // Fetches question data
    private static final CategoryDAO categoryDAO = new CategoryDAOImpl();           // Retrieves category names
    private static final ModeratedAnswerDAO moderatedAnswerDAO = new ModeratedAnswerDAOImpl(); // Stores and validates answers

    // --- WebSocket session tracking structures ---
    private static final Map<Integer, Set<Session>> lobbySessions = new ConcurrentHashMap<>();
    // Maps lobby_id → all WebSocket sessions (clients) currently in that lobby

    private static final Map<Session, Integer> sessionLobbyMap = new ConcurrentHashMap<>();
    // Maps a WebSocket session → its associated lobby_id

    private static final Map<Session, Object[]> sessionUserMap = new ConcurrentHashMap<>();
    // Maps a WebSocket session → [user_id, username]

    private static final Map<Integer, Integer> lobbyCurrentQuestion = new ConcurrentHashMap<>();
    // Maps lobby_id → current question index in that session


    /** Called when a new client connects to the WebSocket */
    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        System.out.println("WebSocket connected: " + session.getId());
    }

    /** Called when a message is received from a client */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        JSONObject msg = new JSONObject(message);
        String type = msg.getString("type");
        Integer lobbyId = msg.has("lobbyId") ? msg.getInt("lobbyId") : sessionLobbyMap.get(session);
        String username = msg.optString("username", "unknown");
        Integer userId = msg.optInt("user_id", -1);

        // Handle different message types
        switch (type) {
            case "join":   // Player joins a lobby
                lobbySessions.putIfAbsent(lobbyId, ConcurrentHashMap.newKeySet());
                lobbySessions.get(lobbyId).add(session);
                sessionLobbyMap.put(session, lobbyId);
                sessionUserMap.put(session, new Object[]{userId, username});
                broadcastLobbyInfo(lobbyId);  // Update others that a new player joined
                break;

            case "next":   // Moderator or player moves to next question
                handleNextQuestion(lobbyId);
                break;

            case "prev":   // Move to previous question
                handlePrevQuestion(lobbyId);
                break;

            case "answer": // Player submits an answer
                String answer = msg.getString("answer");
                handlePlayerAnswer(session, lobbyId, answer);
                break;

            default:
                System.out.println("Unknown message type: " + type);
        }
    }

    /** Called when a client disconnects */
    @OnClose
    public void onClose(Session session) {
        Integer lobbyId = sessionLobbyMap.get(session);
        Object[] userInfo = sessionUserMap.get(session);
        if (userInfo == null) return;

        int participantId = (int) userInfo[0];

        if (lobbyId != null) {
            // Remove session from the lobby set
            lobbySessions.getOrDefault(lobbyId, Set.of()).remove(session);
            try {
                // Remove user from DB session table
                sessionDAO.leaveSession(lobbyId, participantId);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // Notify others in the lobby that this user left
            broadcastToLobby(lobbyId, "User " + participantId + " left the lobby.");
        }

        // Clean up all maps
        sessionLobbyMap.remove(session);
        sessionUserMap.remove(session);
        System.out.println("WebSocket closed: " + session.getId());
    }

    /** Called when an error occurs in the WebSocket */
    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }

    /** Move to the next question in the lobby and broadcast it */
    private void handleNextQuestion(Integer lobbyId) {
        try {
            Integer newIndex = sessionDAO.incrementAndGetCurrentIndex(lobbyId);
            if (newIndex != null) {
                sendQuestionToLobby(lobbyId, newIndex);
                lobbyCurrentQuestion.put(lobbyId, newIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Move to the previous question in the lobby and broadcast it */
    private void handlePrevQuestion(Integer lobbyId) {
        try {
            Integer newIndex = sessionDAO.decrementAndGetCurrentIndex(lobbyId);
            if (newIndex != null) {
                sendQuestionToLobby(lobbyId, newIndex);
                lobbyCurrentQuestion.put(lobbyId, newIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Process and store a player's submitted answer */
    private void handlePlayerAnswer(Session session, Integer lobbyId, String answer) {
        Object[] userInfo = sessionUserMap.get(session);
        if (userInfo == null) return;

        int participantId = (int) userInfo[0];
        String username = (String) userInfo[1];

        System.out.println("Player " + username + " answered: " + answer + " in lobby " + lobbyId);

        try {
            // Identify the question currently being answered
            int currentQuestionId = getCurrentQuestionId(lobbyId);

            // Check correctness and calculate score
            boolean isCorrect = moderatedAnswerDAO.isAnswerCorrect(currentQuestionId, answer);
            Map<String, String> questionData = questionDAO.findQuestionById(currentQuestionId);
            int questionPoints = Integer.parseInt(questionData.get("points"));
            int score = isCorrect ? questionPoints : 0;

            // Store the moderated answer in DB
            moderatedAnswerDAO.createModeratedAnswer(lobbyId, currentQuestionId, participantId, answer, isCorrect, score);

            // Get updated leaderboard and broadcast it to all clients
            List<Map<String, String>> leaderboard = moderatedAnswerDAO.getSessionLeaderboard(lobbyId);
            broadcastLeaderboard(lobbyId, leaderboard);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Sends a question payload to all players in the given lobby */
    private void sendQuestionToLobby(Integer lobbyId, int questionIndex) {
        try {
            List<Integer> questionIds = sessionDAO.findQuestionIdsForSession(lobbyId);
            if (questionIndex < 0 || questionIndex >= questionIds.size()) return;

            int questionId = questionIds.get(questionIndex);
            Map<String, String> qData = questionDAO.findQuestionById(questionId);
            if (qData == null) return;

            JSONObject payload = new JSONObject();
            payload.put("type", "question");
            payload.put("index", questionIndex);
            payload.put("category_name",
                    categoryDAO.findCategoryNameById(Integer.parseInt(qData.get("category_id"))));
            payload.put("question_text", qData.get("question_text"));
            payload.put("answer_option", qData.get("answer_option"));
            payload.put("points", qData.get("points"));
            payload.put("youtube_url", qData.get("youtube_url"));

            broadcastToLobby(lobbyId, payload.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Retrieves the current question ID for a given lobby */
    private int getCurrentQuestionId(Integer lobbyId) throws SQLException {
        Integer currentIndex = sessionDAO.getCurrentIndex(lobbyId);
        List<Integer> questionIds = sessionDAO.findQuestionIdsForSession(lobbyId);
        if (currentIndex == null || questionIds.isEmpty()) return -1;
        return questionIds.get(currentIndex);
    }

    /** Broadcasts player count and lobby info to everyone in the lobby */
    private void broadcastLobbyInfo(Integer lobbyId) {
        JSONObject payload = new JSONObject();
        payload.put("type", "lobbyInfo");
        payload.put("playerCount", lobbySessions.getOrDefault(lobbyId, Set.of()).size());
        broadcastToLobby(lobbyId, payload.toString());
    }

    /** Broadcasts the current leaderboard to all players in the lobby */
    private void broadcastLeaderboard(Integer lobbyId, List<Map<String, String>> leaderboard) {
        JSONObject payload = new JSONObject();
        payload.put("type", "leaderboard");
        payload.put("lobbyId", lobbyId);
        payload.put("leaderboard", leaderboard);
        broadcastToLobby(lobbyId, payload.toString());
    }

    /** Sends a message to all connected sessions in a lobby */
    private void broadcastToLobby(Integer lobbyId, String message) {
        Set<Session> sessions = lobbySessions.get(lobbyId);
        if (sessions != null) {
            for (Session s : sessions) {
                try {
                    s.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
