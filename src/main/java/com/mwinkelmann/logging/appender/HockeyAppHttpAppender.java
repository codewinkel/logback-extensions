package com.mwinkelmann.logging.appender;

import org.apache.http.client.methods.HttpRequestBase;

import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Sends {@link ILoggingEvent} objects to the hockey app backend. 
 * So HockeyApp can visualize the exceptions and inform you if necessary.
 * 
 * @author Mike Winkelmann
 *
 */
public class HockeyAppHttpAppender extends AbstractHttpAppender {

  @Override
  public HttpRequestBase createHttpRequest() {
    // TODO
    return null;
  }

}
