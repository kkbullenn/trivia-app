package com.triviaapp.connection;

import io.github.cdimascio.dotenv.Dotenv;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public final class WhisperConnection extends AbstractConnection {
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
    }

    @Override
    public URL getBaseURL() throws MalformedURLException {
        return WHISPER_URI.toURL();
    }

    @Override
    public URI getBaseURI() {
        return WHISPER_URI;
    }

    @Override
    public URI makeURI(final String uri) {
        return URI.create(WHISPER_URI + uri);
    }

    @Override
    public URL makeURL(final String url) throws MalformedURLException {
        return URI.create(WHISPER_URI + url).toURL();
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
