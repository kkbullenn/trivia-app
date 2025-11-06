package com.triviaapp.servlets;

import com.triviaapp.util.DBConnectionManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.Map;
import com.triviaapp.dao.CategoryDAO;
import com.triviaapp.dao.impl.CategoryDAOImpl;
import com.triviaapp.dao.RoleDAO;
import com.triviaapp.dao.impl.RoleDAOImpl;
import com.triviaapp.dao.UserDAO;
import com.triviaapp.dao.impl.UserDAOImpl;
import com.triviaapp.dao.QuestionDAO;
import com.triviaapp.dao.impl.QuestionDAOImpl;
import com.triviaapp.dao.IndividualAnswerDAO;
import com.triviaapp.dao.impl.IndividualAnswerDAOImpl;
import com.triviaapp.dao.SessionDAO;
import com.triviaapp.dao.impl.SessionDAOImpl;
import java.sql.Timestamp;
import com.triviaapp.dao.ModeratedAnswerDAO;
import com.triviaapp.dao.impl.ModeratedAnswerDAOImpl;

/**
 * Simple servlet to test database connectivity using DBConnectionManager.
 * Visit /test-db after deploying to verify connection works.
 */
public class TestDbServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain; charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            out.println("Testing DB connection...");
            try (Connection c = DBConnectionManager.getConnection()) {
                if (c != null && !c.isClosed()) {
                    out.println("OK: got connection");
                    out.println("DB Product: " + c.getMetaData().getDatabaseProductName() + " "
                            + c.getMetaData().getDatabaseProductVersion());
                } else {
                    out.println("FAIL: connection is null or closed");
                }
            } catch (Exception e) {
                out.println("ERROR: failed to get connection:");
                e.printStackTrace(out);
            }

            // Run DAO tests
            out.println();
            testRoleDAO(out);
            out.println();
            testCategoryDAO(out);
            out.println();
            testUserDAO(out);
            out.println();
            testQuestionDAO(out);
            out.println();
            testIndividualAnswerDAO(out);
            out.println();
            testSessionDAO(out);
            out.println();
            testModeratedAnswerDAO(out);
        }
    }

    // --- DAO test helpers -------------------------------------------------
    private void testRoleDAO(PrintWriter out) {
        out.println("Testing RoleDAO...");
        try {
            RoleDAO roleDao = new RoleDAOImpl();
            Map<Integer, String> roles = roleDao.findAllRoles();
            out.println("Roles count: " + roles.size());
            for (Map.Entry<Integer, String> en : roles.entrySet()) {
                out.println(en.getKey() + " -> " + en.getValue());
            }
            if (!roles.isEmpty()) {
                Integer firstId = roles.keySet().iterator().next();
                String name = roleDao.findRoleNameById(firstId);
                out.println("Lookup by id " + firstId + " -> " + name);
            }
        } catch (Exception e) {
            out.println("ERROR: RoleDAO test failed:");
            e.printStackTrace(out);
        }
    }

    private void testUserDAO(PrintWriter out) {
        out.println("Testing UserDAO...");
        try {
            UserDAO userDao = new UserDAOImpl();
            String ts = String.valueOf(System.currentTimeMillis());
            String username = "testuser_" + ts;
            String email = "test+" + ts + "@example.com";
            String password = "pw" + ts;

            int roleId = 100;
            boolean createdRole = false;
            int createdRoleId = -1;

            // Use DAO to create user (tests createUser implementation)
            boolean created = userDao.createUser(username, email, password, roleId);
            out.println("userDao.createUser returned: " + created + " (roleId=" + roleId + ")");

            // Verify via DAO read method; avoid printing plaintext password in logs
            String pwFromDb = userDao.findPasswordByEmail(email);
            out.println("Password found: " + (pwFromDb != null));
            out.println("Password matches provided: " + (pwFromDb != null && pwFromDb.equals(password)));

            // cleanup test user (direct SQL cleanup is OK for tests)
            try (Connection conn = DBConnectionManager.getConnection();
                    java.sql.PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE email = ?")) {
                ps.setString(1, email);
                int del = ps.executeUpdate();
                out.println("Deleted rows: " + del);
            }

        } catch (Exception e) {
            out.println("ERROR: UserDAO test failed:");
            e.printStackTrace(out);
        }
    }

    private void testCategoryDAO(PrintWriter out) {
        out.println("Testing CategoryDAO...");
        try {
            CategoryDAO catDao = new CategoryDAOImpl();
            Map<Integer, String> cats = catDao.findAllCategories();
            out.println("Categories count: " + cats.size());
            for (Map.Entry<Integer, String> en : cats.entrySet()) {
                out.println(en.getKey() + " -> " + en.getValue());
            }
            if (!cats.isEmpty()) {
                Integer firstId = cats.keySet().iterator().next();
                String name = catDao.findCategoryNameById(firstId);
                out.println("Lookup by id " + firstId + " -> " + name);
            }
        } catch (Exception e) {
            out.println("ERROR: CategoryDAO test failed:");
            e.printStackTrace(out);
        }
    }

    private void testQuestionDAO(PrintWriter out) {
        out.println("Testing QuestionDAO...");
        boolean createdCategory = false;
        int createdCategoryId = -1;
        boolean createdUser = false;
        int createdUserId = -1;
        try {
            QuestionDAO qDao = new QuestionDAOImpl();
            CategoryDAO catDao = new CategoryDAOImpl();

            int catId = 1;
            int uploadedBy = 1;
            out.println("Using seeded category id=" + catId + " and uploaded_by=" + uploadedBy);

            // 1) Test createQuestion (DAO)
            String ts = String.valueOf(System.currentTimeMillis());
            String questionText = "test question " + ts;
            String xmlQuestion = "<q>test</q>";
            String youtubeUrl = "https://example.com/watch?v=test";
            String answersJson = "{\"A\":\"one\",\"B\":\"two\",\"C\":\"three\",\"D\":\"four\"}";
            String answersKey = "A";
            int points = 5;

            boolean created = qDao.createQuestion(catId, xmlQuestion, youtubeUrl, questionText, answersJson, answersKey,
                    points, uploadedBy);
            out.println("createQuestion returned: " + created);

            // 2) locate the created question id by scanning questions in the category
            Integer foundQid = null;
            java.util.List<Map<String, String>> qlist = qDao.findQuestionsByCategory(catId);
            for (Map<String, String> row : qlist) {
                if (questionText.equals(row.get("question_text"))) {
                    foundQid = Integer.valueOf(row.get("question_id"));
                    break;
                }
            }
            if (foundQid == null) {
                out.println("Failed to locate created question by text; attempting id-scan");
                java.util.List<Integer> ids = qDao.findQuestionIdsByCategory(catId);
                for (Integer id : ids) {
                    Map<String, String> r = qDao.findQuestionById(id);
                    if (r != null && questionText.equals(r.get("question_text"))) {
                        foundQid = id;
                        break;
                    }
                }
            }

            if (foundQid == null) {
                out.println(
                        "ERROR: could not find question created by createQuestion; aborting remaining question tests");
            } else {
                out.println("Located created question id=" + foundQid);

                // 3) findQuestionById
                Map<String, String> one = qDao.findQuestionById(foundQid);
                out.println("findQuestionById returned: question_text="
                        + (one == null ? "<null>" : one.get("question_text")));

                // 4) findQuestionIdsByCategory
                java.util.List<Integer> ids2 = qDao.findQuestionIdsByCategory(catId);
                out.println("findQuestionIdsByCategory count=" + ids2.size() + ", contains created id="
                        + ids2.contains(foundQid));

                // 5) updateQuestion
                String updatedText = questionText + " - updated";
                boolean updated = qDao.updateQuestion(foundQid, catId, xmlQuestion, youtubeUrl, updatedText,
                        answersJson, answersKey, points + 1);
                out.println("updateQuestion returned: " + updated);
                Map<String, String> after = qDao.findQuestionById(foundQid);
                out.println("After update: question_text=" + (after == null ? "<null>" : after.get("question_text"))
                        + ", points=" + (after == null ? "?" : after.get("points")));

                // 6) deleteQuestion
                boolean deleted = qDao.deleteQuestion(foundQid);
                out.println("deleteQuestion returned: " + deleted);
                Map<String, String> afterDel = qDao.findQuestionById(foundQid);
                out.println(
                        "findQuestionById after delete => " + (afterDel == null ? "null (deleted)" : "still exists"));
            }
        } catch (Exception e) {
            out.println("ERROR: QuestionDAO test failed:");
            e.printStackTrace(out);
        } finally {
            // cleanup temporary user/category if we created them
            try {
                if (createdUser && createdUserId != -1) {
                    try (java.sql.Connection conn = DBConnectionManager.getConnection();
                            java.sql.PreparedStatement ps = conn
                                    .prepareStatement("DELETE FROM users WHERE user_id = ?")) {
                        ps.setInt(1, createdUserId);
                        int d = ps.executeUpdate();
                        out.println("Deleted temporary user " + createdUserId + ": " + d);
                    }
                }
            } catch (Exception ex) {
                out.println("WARN: failed to cleanup temporary user");
                ex.printStackTrace(out);
            }
            try {
                if (createdCategory && createdCategoryId != -1) {
                    try (java.sql.Connection conn = DBConnectionManager.getConnection();
                            java.sql.PreparedStatement ps = conn
                                    .prepareStatement("DELETE FROM categories WHERE category_id = ?")) {
                        ps.setInt(1, createdCategoryId);
                        int d = ps.executeUpdate();
                        out.println("Deleted temporary category " + createdCategoryId + ": " + d);
                    }
                }
            } catch (Exception ex) {
                out.println("WARN: failed to cleanup temporary category");
                ex.printStackTrace(out);
            }
        }
    }

    private void testIndividualAnswerDAO(PrintWriter out) {
        out.println("Testing IndividualAnswerDAO...");
        try {
            IndividualAnswerDAO iaDao = new IndividualAnswerDAOImpl();
            // Use seeded ids from seed.sql: question_id=1, user_id=1, category_id=1
            int questionId = 1;
            int userId = 1;
            String selected = "B";
            boolean isCorrect = false;
            int score = 0;

            boolean created = iaDao.createAnswer(questionId, userId, selected, isCorrect, score);
            out.println("createAnswer returned: " + created);

            java.util.List<Map<String, String>> answers = iaDao.findAnswersByUser(userId);
            out.println("findAnswersByUser count: " + answers.size());
            if (!answers.isEmpty()) {
                Map<String, String> first = answers.get(0);
                out.println("Latest answer id=" + first.get("answer_id") + ", question_id=" + first.get("question_id")
                        + ", selected=" + first.get("selected_answer") + ", score=" + first.get("score"));

                // record id for cleanup
                int answerId = Integer.parseInt(first.get("answer_id"));

                int total = iaDao.getTotalScoreForIndividualMode(userId, 1);
                out.println("getTotalScoreForIndividualMode(user=1,cat=1) returned: " + total);

                // cleanup the created answer
                try (java.sql.Connection conn = DBConnectionManager.getConnection();
                        java.sql.PreparedStatement ps = conn
                                .prepareStatement("DELETE FROM individual_answers WHERE answer_id = ?")) {
                    ps.setInt(1, answerId);
                    int del = ps.executeUpdate();
                    out.println("Deleted individual_answers rows: " + del);
                }
            }
        } catch (Exception e) {
            out.println("ERROR: IndividualAnswerDAO test failed:");
            e.printStackTrace(out);
        }
    }

    private void testSessionDAO(PrintWriter out) {
        out.println("Testing SessionDAO...");
        try {
            SessionDAO sDao = new SessionDAOImpl();
            // Use seeded host user id
            int hostUserId = 1;
            String ts = String.valueOf(System.currentTimeMillis());
            String sessionName = "test_session_" + ts;

            boolean created = sDao.createSession(hostUserId, sessionName, 10, "active", null, null);
            out.println("createSession returned: " + created);

            // find sessions by host and locate the one we created
            java.util.List<Map<String, String>> sessions = sDao.findSessionsByHost(hostUserId);
            out.println("findSessionsByHost count: " + sessions.size());
            Integer foundId = null;
            for (Map<String, String> row : sessions) {
                if (sessionName.equals(row.get("session_name"))) {
                    foundId = Integer.valueOf(row.get("session_id"));
                    break;
                }
            }

            // list active sessions summary
            java.util.List<Map<String, String>> summary = sDao.listActiveSessionsSummary();
            out.println("listActiveSessionsSummary count: " + summary.size());

            if (foundId == null) {
                out.println("Could not locate created session by name; skipping participant tests");
            } else {
                out.println("Located session id=" + foundId);
                Map<String, String> one = sDao.findSessionById(foundId);
                out.println("findSessionById status=" + (one == null ? "<null>" : one.get("status")));

                // join as participant (use user 1)
                boolean joined = sDao.joinSession(foundId, hostUserId);
                out.println("joinSession returned: " + joined);

                java.util.List<Map<String, String>> parts = sDao.findParticipantsBySession(foundId);
                out.println("findParticipantsBySession count: " + parts.size());

                // leave
                boolean left = sDao.leaveSession(foundId, hostUserId);
                out.println("leaveSession returned: " + left);

                // end session now
                boolean ended = sDao.endSessionNow(foundId);
                out.println("endSessionNow returned: " + ended);

                // delete session
                boolean deleted = sDao.deleteSession(foundId);
                out.println("deleteSession returned: " + deleted);

                // verify deletion
                Map<String, String> after = sDao.findSessionById(foundId);
                out.println("findSessionById after delete => " + (after == null ? "null (deleted)" : "still exists"));
            }

        } catch (Exception e) {
            out.println("ERROR: SessionDAO test failed:");
            e.printStackTrace(out);
        }
    }

    private void testModeratedAnswerDAO(PrintWriter out) {
        out.println("Testing ModeratedAnswerDAO...");
        try {
            ModeratedAnswerDAO mDao = new ModeratedAnswerDAOImpl();
            SessionDAO sDao = new SessionDAOImpl();

            int hostUserId = 1;
            String ts = String.valueOf(System.currentTimeMillis());
            String sessionName = "test_modans_session_" + ts;

            // create session to attach moderated answers
            boolean created = sDao.createSession(hostUserId, sessionName, 20, "active", null, null);
            out.println("createSession for moderated answers returned: " + created);

            // locate session id
            Integer sessionId = null;
            java.util.List<Map<String, String>> sessions = sDao.findSessionsByHost(hostUserId);
            for (Map<String, String> r : sessions) {
                if (sessionName.equals(r.get("session_name"))) {
                    sessionId = Integer.valueOf(r.get("session_id"));
                    break;
                }
            }

            if (sessionId == null) {
                out.println("Failed to locate session for moderated answers; aborting test");
                return;
            }
            out.println("Using session_id=" + sessionId);

            // use seeded question_id=1 and participant user_id=1
            int questionId = 1;
            int participantId = 1;
            String selected = "A";
            boolean isCorrect = true;
            int score = 5;

            boolean createdAns = mDao.createModeratedAnswer(sessionId, questionId, participantId, selected, isCorrect,
                    score);
            out.println("createModeratedAnswer returned: " + createdAns);

            java.util.List<Map<String, String>> bySession = mDao.findAnswersBySession(sessionId);
            out.println("findAnswersBySession count: " + bySession.size());
            if (!bySession.isEmpty()) {
                Map<String, String> a = bySession.get(0);
                out.println("Latest moderated answer id=" + a.get("answer_id") + ", participant="
                        + a.get("participant_id") + ", score=" + a.get("score"));
            }

            java.util.List<Map<String, String>> byParticipant = mDao.findAnswersByParticipant(participantId, sessionId);
            out.println("findAnswersByParticipant count: " + byParticipant.size());

            java.util.List<Map<String, String>> leaderboard = mDao.getSessionLeaderboard(sessionId);
            out.println("getSessionLeaderboard count: " + leaderboard.size());
            if (!leaderboard.isEmpty()) {
                Map<String, String> top = leaderboard.get(0);
                out.println("Top participant=" + top.get("participant_id") + ", username=" + top.get("username")
                        + ", total_score=" + top.get("total_score"));
            }

            // cleanup moderated answers for this session
            try (java.sql.Connection conn = DBConnectionManager.getConnection();
                    java.sql.PreparedStatement ps = conn
                            .prepareStatement("DELETE FROM moderated_answers WHERE session_id = ?")) {
                ps.setInt(1, sessionId);
                int del = ps.executeUpdate();
                out.println("Deleted moderated_answers rows: " + del);
            }

            // delete the created session
            boolean deleted = sDao.deleteSession(sessionId);
            out.println("deleteSession (for moderated answers) returned: " + deleted);

        } catch (Exception e) {
            out.println("ERROR: ModeratedAnswerDAO test failed:");
            e.printStackTrace(out);
        }
    }

}
