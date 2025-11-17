package com.triviaapp.util;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

    /**
     * Obtain a JDBC Connection using DriverManager.
     * Configuration must be provided via .env (JDBC_URL/JDBC_USER/JDBC_PASS)
     * or system environment variables.
     *
     * @author Haven Zhang
     */
public class DBConnectionManager {

    public static Connection getConnection() throws SQLException {
        // Load .env values
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        } catch (Throwable ignored) {
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
            }
        }

        if (url == null || url.isEmpty())
        {
            url = System.getenv("JDBC_URL");
        }
        if (user == null || user.isEmpty())
        {
            user = System.getenv("JDBC_USER");
        }
        if (pass == null || pass.isEmpty())
        {
            pass = System.getenv("JDBC_PASS");
        }

        if (url == null || url.isEmpty() || user == null || user.isEmpty() || pass == null || pass.isEmpty()) {
            throw new SQLException("Missing database configuration: JDBC_URL, JDBC_USER and JDBC_PASS must be set in .env or environment variables");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {}

        return DriverManager.getConnection(url, user, pass);
    }
}
