package com.mwinkelmann.logging.appender;

import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

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
    StringBuilder sb = new StringBuilder();
    sb.append("{\"jsonrpc\":\"2.0\"").
      append(",\"params\":{").
      append("\"user\":\"").append(username).
      append("\",\"password\":\"").append(password).
      append("\"},").
      append("\"method\":\"user.authenticate\",").
      append("\"id\":\"2\"}");

    HttpPost httpPost = null;
    try {
      httpPost = new HttpPost(this.requestUrl);
      httpPost.setEntity(new StringEntity(sb.toString()));
      httpPost.addHeader("Content-Type", "application/json-rpc");
    } catch (UnsupportedEncodingException e) {
      throw new HttpAppenderException("Error in creating body for Zabbix request:", e);
    }

    return httpPost;
  }
}
