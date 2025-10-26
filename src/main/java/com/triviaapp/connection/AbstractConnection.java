package main.java.com.triviaapp.connection;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * This class offers base functionality for external connections to other servers.
 */
abstract class AbstractConnection {
    protected abstract URL getBaseURL() throws MalformedURLException;
    protected abstract URI getBaseURI();
    protected abstract URI makeURI(final String uri);
    protected abstract URL makeURL(final String url) throws MalformedURLException;
}
