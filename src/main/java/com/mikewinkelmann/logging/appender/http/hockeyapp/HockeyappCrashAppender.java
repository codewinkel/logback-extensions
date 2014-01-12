package com.mikewinkelmann.logging.appender.http.hockeyapp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mikewinkelmann.logging.appender.http.AbstractHttpAppender;
import com.mikewinkelmann.logging.appender.http.exception.HttpAppenderException;

/**
 * Sends the special {@link ILoggingEvent} object to the hockeyapp backend. 
 * So Hockeyapp can visualize the exceptions, opens a bug in bugtracker, informs you if necessary and so on.
 * 
 * @author Mike Winkelmann
 *
 */
public class HockeyappCrashAppender extends AbstractHttpAppender {


  private static final Logger logger = LoggerFactory.getLogger(HockeyappCrashAppender.class);

  // configuration
  private SimpleDateFormat dateFormat = null;
  private String userId, contact, model, manufacturer, os, version, packageName, apiToken, appId, requestUrl;

  public HockeyappCrashAppender() {
    this.setRequestUrl(HockeyAppCrashAppenderConfig.HOCKEYAPP_CRASH_API_URL);
    dateFormat = new SimpleDateFormat(HockeyAppCrashAppenderConfig.DATE_FORMAT);
  }

  @Override
  public void start() {
    super.start();
    Preconditions.checkNotNull(this.apiToken, "ApiToken must not be null");
    Preconditions.checkNotNull(this.appId, "AppId must not be null");
    Preconditions.checkNotNull(this.packageName, "PackageName must not be null");
    requestUrl =
      this.getRequestUrl().replace(HockeyAppCrashAppenderConfig.HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER, this.appId);
  }

  @Override
  public HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException {

    logger.debug("Create HttpRequest for HockeyApp call against crash api Event: " + event.getLevel().levelStr);
    final HttpPost httpRequest = new HttpPost(requestUrl);
    httpRequest.addHeader("X-HockeyAppToken", this.apiToken);

    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
      .addPart("log", createCrashFileBody(event));
    //  TODO implement .addPart("description", this.createDescriptionLogFile(event))
    //  TODO implement .addPart("attachment", this.createAttatchmentFile(event))
    if (this.userId != null)
      multipartEntityBuilder.addTextBody("userID", this.userId);
    if (this.contact != null)
      multipartEntityBuilder.addTextBody("contact", this.contact);

    HttpEntity multipartEntity = multipartEntityBuilder.build();

    httpRequest.setEntity(multipartEntity);

    return httpRequest;
  }

  private FileBody createCrashFileBody(ILoggingEvent event) throws HttpAppenderException {
    String[] splittedMessage = event.getFormattedMessage().split("\n");
    File crashFile = this.createCrashLogFile(splittedMessage, event.getTimeStamp(), 0);
    FileBody crashFileBody = new FileBody(crashFile);
    return crashFileBody;
  }

  private File createCrashLogFile(String[] splittedMessage, long timestamp, int rowsToDelete)
    throws HttpAppenderException {
    FileWriter fileWriter = null;
    BufferedWriter bufferedWriter = null;
    File tmpFile = null;
    try {
      StringBuffer content = new StringBuffer();
      tmpFile = File.createTempFile("exception", ".exc.log");

      content.append("Package: ").append(this.packageName).append("\n");
      content.append("Version: ").append(Strings.nullToEmpty(this.version)).append("\n");
      content.append("OS: ").append(Strings.nullToEmpty(this.os)).append("\n");
      content.append("Manufacturer: ").append(Strings.nullToEmpty(this.manufacturer)).append("\n");
      content.append("Model: ").append(Strings.nullToEmpty(this.model)).append("\n");
      content.append("Date: ").append(dateFormat.format(new Date(timestamp))).append("\n");
      content.append("\n");
      content.append(this.parseStringArrayMessage(splittedMessage, rowsToDelete)).append("\n");

      fileWriter = new FileWriter(tmpFile);
      bufferedWriter = new BufferedWriter(fileWriter);
      bufferedWriter.write(content.toString());

      double bytes = tmpFile.length();
      double kilobytes = (bytes / 1024);

      if (kilobytes > HockeyAppCrashAppenderConfig.MAXIMUM_CRASH_FILE_SIZE_KILOBYTES)
      {
        logger.debug("Created crash file size to big for HockeyApp API Size: "
          + kilobytes + " MaxSize: " + HockeyAppCrashAppenderConfig.MAXIMUM_CRASH_FILE_SIZE_KILOBYTES);
        tmpFile.delete();
        tmpFile = createCrashLogFile(splittedMessage, timestamp, rowsToDelete++);
      }
    } catch (Exception e) {
      throw new HttpAppenderException("Error due to create crash log file:", e);
    } finally
    {
      if (bufferedWriter != null)
      {
        try {
          bufferedWriter.flush();
          bufferedWriter.close();
        } catch (IOException e) {
          throw new HttpAppenderException("Error due to close crash log buffered file writer:", e);
        }
      }
      if (fileWriter != null)
        try {
          fileWriter.close();
        } catch (Exception e) {
          throw new HttpAppenderException("Error due to close crash log file writer:", e);
        }
    }
    return tmpFile;
  }

  private String parseStringArrayMessage(String[] splittedMessage, int rowsToDelete) {
    logger.debug("Parse exception message and remove "
      + rowsToDelete + " rows to create the correct crash log file size.");
    StringBuffer buffer = new StringBuffer();
    int messageLenght = splittedMessage.length;
    int columnIndex = 0;
    for (String messageColumn : splittedMessage) {
      buffer.append(messageColumn).append("\n");
      columnIndex++;
      if (columnIndex == (messageLenght - rowsToDelete))
      {
        break;
      }
    }
    return buffer.toString();
  }

  public void setUserId(String userId) {
    if (userId != null && userId.length() <= 255)
      this.userId = userId;
  }

  public void setContact(String contact) {
    if (contact != null && contact.length() <= 255)
      this.contact = contact;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

}
