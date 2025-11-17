package com.triviaapp.externalapi;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Connection helper to talk to the local M2M100 Python service.
 * Example endpoint:
 *      http://localhost:8892/m2m100/translate/questions-answers
 *
 * @author Aira Bassig
 */
public class M2M100Connection {

    private final String endpoint;

    public M2M100Connection(String endpoint) {
        this.endpoint = endpoint;
    }

    public JSONObject translateBatch(JSONObject payload) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(180000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        // send JSON to python
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes());
        }

        int status = conn.getResponseCode();

        BufferedReader reader;
        if (status >= 200 && status < 300) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        conn.disconnect();

        if (status < 200 || status >= 300) {
            throw new RuntimeException("M2M100 returned " + status + ": " + sb);
        }

        return new JSONObject(sb.toString());
    }
}
