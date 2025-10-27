package com.triviaapp.connection;

import io.github.cdimascio.dotenv.Dotenv;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public final class WhisperConnection extends ServerConnection {
    public static final URI WHISPER_GET_URI;
    public static final URI WHISPER_POST_URI;

    private static final Dotenv ENV;
    private static final String ENV_MODE;

    private static final String DEV_MODE = "dev";
    private static final int WHISPER_LOCAL_PORT = 8888;

    private static final String HOST;
    private static final int PORT;
    private static final URI WHISPER_URI;

    static {
        ENV = Dotenv.load();
        ENV_MODE = getMode(ENV.get("WHISPER_MODE"));
        HOST = ENV_MODE.equals(DEV_MODE) ? "localhost" : ENV.get("WHISPER_HOST");
        PORT = ENV_MODE.equals(DEV_MODE) ? WHISPER_LOCAL_PORT : Integer.parseInt(ENV_MODE);
        WHISPER_URI = createURI();
        WHISPER_GET_URI = URI.create(WHISPER_URI + "/whisper");
        WHISPER_POST_URI = URI.create(WHISPER_URI + "/transcribe");
    }

    @Override
    public URL getPostURL() throws MalformedURLException {
        return WHISPER_POST_URI.toURL();
    }

    @Override
    public URL getGetURL() throws MalformedURLException {
        return WHISPER_GET_URI.toURL();
    }

    private static String getMode(final String mode) {
        if (mode == null || mode.isEmpty()) {
            return DEV_MODE;
        }

        return mode;
    }

    private static URI createURI() {
        final String http = DEV_MODE.equals(ENV_MODE) ? "http" : "https";

        return URI.create(String.format("%s://%s:%s", http, HOST, PORT));
    }
}
