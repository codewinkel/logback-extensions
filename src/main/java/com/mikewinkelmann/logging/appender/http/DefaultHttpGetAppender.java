package com.mikewinkelmann.logging.appender.http;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.mikewinkelmann.logging.appender.http.exception.HttpAppenderException;

/**
 * Sends http get request to the configured request url. 
 * 
 * @author Mike Winkelmann
 *
 */
public class DefaultHttpGetAppender extends AbstractHttpAppender {

  @Override
  public HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException {
    final HttpGet httpRequest = new HttpGet(this.getRequestUrl());
    httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    httpRequest.addHeader(HttpHeaders.ACCEPT, "application/json");
    return httpRequest;
  }

}
