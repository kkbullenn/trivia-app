package com.triviaapp.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple utility to obtain a JDBC Connection using DriverManager.
 * Priority for configuration values:
 * 1) .env (loaded via java-dotenv)
 * 2) System environment variables (JDBC_URL, JDBC_USER, JDBC_PASS)
 * 3) hardcoded defaults (for local dev only)
 */
public class DBConnectionManager {
    private static final String DEFAULT_URL = "jdbc:mysql://shuttle.proxy.rlwy.net:24339/trivia_app?useSSL=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "haven";
    private static final String DEFAULT_PASS = "haven!123";

    public static Connection getConnection() throws SQLException {
        // Try to load .env values first (if present)
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        } catch (Throwable ignored) {
            // ignore dotenv loading problems and fall back to env
        }

        String url = null;
        String user = null;
        String pass = null;

        if (dotenv != null) {
            try {
                url = dotenv.get("JDBC_URL");
                user = dotenv.get("JDBC_USER");
                pass = dotenv.get("JDBC_PASS");
            } catch (Exception ignored) {
                // ignore
            }
        }

        if (url == null || url.isEmpty()) url = System.getenv("JDBC_URL");
        if (user == null || user.isEmpty()) user = System.getenv("JDBC_USER");
        if (pass == null || pass.isEmpty()) pass = System.getenv("JDBC_PASS");

        if (url == null || url.isEmpty()) url = DEFAULT_URL;
        if (user == null || user.isEmpty()) user = DEFAULT_USER;
        if (pass == null || pass.isEmpty()) pass = DEFAULT_PASS;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}

        return DriverManager.getConnection(url, user, pass);
    }
}
