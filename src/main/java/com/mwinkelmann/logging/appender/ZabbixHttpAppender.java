package com.mwinkelmann.logging.appender;

import org.apache.http.client.methods.HttpRequestBase;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.mwinkelmann.logging.appender.exception.HttpAppenderException;

/**
 * Sends an error message to the zabbix backend. 
 * So Zabbix can visualize the exceptions and inform you if necessary.
 * 
 * @author Mike Winkelmann
 *
 */
public class ZabbixHttpAppender extends AbstractHttpAppender {

  private String username;
  private String password;

  @Override
  public HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException {
    // TODO
    return null;
  }
}
