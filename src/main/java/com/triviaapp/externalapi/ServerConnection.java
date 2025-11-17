package com.triviaapp.externalapi;

import java.net.URL;

/**
 * This class offers base functionality for external connections to other servers.
 *
 * @author Samarjit Bhogal
 */
abstract class ServerConnection {
    protected abstract URL getPostURL();
    protected abstract URL getGetURL();
}
