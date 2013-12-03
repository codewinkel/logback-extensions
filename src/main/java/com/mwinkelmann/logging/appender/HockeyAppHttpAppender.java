package com.mwinkelmann.logging.appender;

import java.io.File;
import java.io.FileWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.mwinkelmann.logging.appender.exception.HttpAppenderException;

/**
 * Sends {@link ILoggingEvent} objects to the hockey app backend. 
 * So HockeyApp can visualize the exceptions and inform you if necessary.
 * 
 * @author Mike Winkelmann
 *
 */
public class HockeyAppHttpAppender extends AbstractHttpAppender {

  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  // configuration
  private String userId;
  private String email;
  private String model;
  private String manufacturer;
  private String os;
  private String version;
  private String packageName;
  private String apiToken;

  @Override
  public HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException {

    final HttpPost httpRequest = new HttpPost(this.requestUrl);
    httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    httpRequest.addHeader(HttpHeaders.ACCEPT, "application/json");
    httpRequest.addHeader("X-HockeyAppToken", this.apiToken);

    List<BasicNameValuePair> parameters = new ArrayList<BasicNameValuePair>();

    parameters.add(new BasicNameValuePair("log", this.createCrashLogFile(event)));
    // parameters.add(new BasicNameValuePair("description", null));
    // parameters.add(new BasicNameValuePair("attachment", null));
    parameters.add(new BasicNameValuePair("userId", this.userId));
    parameters.add(new BasicNameValuePair("contact", this.email));

    UrlEncodedFormEntity entity;
    try {
      entity = new UrlEncodedFormEntity(parameters);
      httpRequest.setEntity(entity);
    } catch (UnsupportedEncodingException e) {
      throw new HttpAppenderException("Error due to http call from log appender:", e);
    }

    return httpRequest;
  }

  private String createCrashLogFile(ILoggingEvent event) throws HttpAppenderException {
    String filePath = null;
    FileWriter fileWriter = null;
    try {
      StringBuffer content = new StringBuffer();
      File tmpFile = File.createTempFile("exception", ".exc.log");

      content.append("Package: ").append(this.packageName).append("\n");
      content.append("Version: ").append(this.version).append("\n");
      content.append("OS: ").append(this.os).append("\n");
      content.append("Manufacturer: ").append(this.manufacturer).append("\n");
      content.append("Model: ").append(this.model).append("\n");
      content.append("Date: ").append(sdf.format(new Date(event.getTimeStamp()))).append("\n");
      content.append("\n");
      content.append(event.getFormattedMessage()).append("\n");

      fileWriter = new FileWriter(tmpFile);
      fileWriter.write(content.toString());
      filePath = tmpFile.getAbsolutePath();
    } catch (Exception e) {
      throw new HttpAppenderException("Error due to create crash log file:", e);
    } finally
    {
      if (fileWriter != null) try {
        fileWriter.close();
      } catch (Exception e) {
        throw new HttpAppenderException("Error due to close crash log file writer:", e);
      }
    }
    return filePath;
  }

}
