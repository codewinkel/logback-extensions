package com.mikewinkelmann.logging.appender.http;

/**
 * Default configuration for {@link AbstractHttpAppender}.
 *
 * @author Mike Winkelmann
 */
public class AbstractHttpAppenderConfig {

    // general
    public static final int DEFAULT_SUCCESS_CODE_MAX = 299;
    public static final int DEFAULT_SUCCESS_CODE_MIN = 200;
    public static final int DEFAULT_QUEUE_SIZE = 10;

    // notify levels
    public static final boolean DEFAULT_WARN = false;
    public static final boolean DEFAULT_ERROR = false;
    public static final boolean DEFAULT_INFO = false;
    public static final boolean DEFAULT_DEBUG = false;
    public static final boolean DEFAULT_TRACE = false;

    // connection
    public static final int HTTP_CONNECTION_REQUEST_TIMEOUT = 5000;
    public static final int HTTP_CONNECTION_TIMEOUT = 5000;
    public static final int HTTP_SOCKET_TIMEOUT = 5000;

}
