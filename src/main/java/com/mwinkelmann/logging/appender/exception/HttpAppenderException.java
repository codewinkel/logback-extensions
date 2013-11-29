package com.mwinkelmann.logging.appender.exception;

/**
 * Specific http appender exception.
 * 
 * @author Mike Winkelmann
 *
 */
public class HttpAppenderException extends Exception {

  private static final long serialVersionUID = 14325894786L;

  public HttpAppenderException() {
    super();
  }

  public HttpAppenderException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public HttpAppenderException(final String message) {
    super(message);
  }

  public HttpAppenderException(final Throwable cause) {
    super(cause);
  }

}
