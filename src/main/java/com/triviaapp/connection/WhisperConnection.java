package com.triviaapp.connection;

import io.github.cdimascio.dotenv.Dotenv;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * This class offers functionality for managing Whisper server's URLs/URIs.
 */
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
    public URL getPostURL() {
        try {
            return WHISPER_POST_URI.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(WHISPER_POST_URI + "is not a valid URL extension", e);
        }
    }

    @Override
    public URL getGetURL() {
        try {
            return WHISPER_GET_URI.toURL();
        } catch (final MalformedURLException e) {
            throw new RuntimeException(WHISPER_POST_URI + "is not a valid URL extension", e);
        }
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
