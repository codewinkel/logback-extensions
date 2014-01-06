package com.mwinkelmann.logging.appender.http.hockeyapp;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;

import ch.qos.logback.classic.spi.ILoggingEvent;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mwinkelmann.logging.appender.http.AbstractHttpAppender;
import com.mwinkelmann.logging.appender.http.exception.HttpAppenderException;

/**
 * Sends the special {@link ILoggingEvent} object to the hockeyapp backend. 
 * So Hockeyapp can visualize the exceptions, opens a bug in bugtracker, informs you if necessary and so on.
 * 
 * @author Mike Winkelmann
 *
 */
public class HockeyappCrashAppender extends AbstractHttpAppender {
  // @TODO implememt checks for filesize
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private static final String HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER = "{APPID}";
  private static final String HOCKEYAPP_CRASH_API_URL = "https://rink.hockeyapp.net/api/2/apps/"
    + HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER + "/crashes/upload";

  // configuration
  private SimpleDateFormat dateFormat = null;
  private String userId, contact, model, manufacturer, os, version, packageName, apiToken, appId, requestUrl;

  public HockeyappCrashAppender() {
    this.setRequestUrl(HOCKEYAPP_CRASH_API_URL);
    dateFormat = new SimpleDateFormat(DATE_FORMAT);
  }

  @Override
  public void start() {
    super.start();
    Preconditions.checkNotNull(this.apiToken, "ApiToken must not be null");
    Preconditions.checkNotNull(this.appId, "AppId must not be null");
    Preconditions.checkNotNull(this.packageName, "PackageName must not be null");
    requestUrl = this.getRequestUrl().replace(HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER, this.appId);
  }

  @Override
  public HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException {

    final HttpPost httpRequest = new HttpPost(requestUrl);
    httpRequest.addHeader("X-HockeyAppToken", this.apiToken);

    File crashFile = this.createCrashLogFile(event);
    FileBody crashFileBody = new FileBody(crashFile);

    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
      .addPart("log", crashFileBody);
    //  TODO implement .addBinaryBody("description", this.createDescriptionLogFile(event))
    //  TODO implement .addBinaryBody("attachment", this.createAttatchmentFile(event))
    if (this.userId != null)
      multipartEntityBuilder.addTextBody("userID", this.userId);
    if (this.contact != null)
      multipartEntityBuilder.addTextBody("contact", this.contact);

    HttpEntity multipartEntity = multipartEntityBuilder.build();

    httpRequest.setEntity(multipartEntity);

    return httpRequest;
  }

  private File createCrashLogFile(ILoggingEvent event) throws HttpAppenderException {
    FileWriter fileWriter = null;
    File tmpFile = null;
    try {
      StringBuffer content = new StringBuffer();
      tmpFile = File.createTempFile("exception", ".exc.log");

      content.append("Package: ").append(this.packageName).append("\n");
      content.append("Version: ").append(Strings.nullToEmpty(this.version)).append("\n");
      content.append("OS: ").append(Strings.nullToEmpty(this.os)).append("\n");
      content.append("Manufacturer: ").append(Strings.nullToEmpty(this.manufacturer)).append("\n");
      content.append("Model: ").append(Strings.nullToEmpty(this.model)).append("\n");
      content.append("Date: ").append(dateFormat.format(new Date(event.getTimeStamp()))).append("\n");
      content.append("\n");
      content.append(event.getFormattedMessage()).append("\n");

      fileWriter = new FileWriter(tmpFile);
      fileWriter.write(content.toString());
    } catch (Exception e) {
      throw new HttpAppenderException("Error due to create crash log file:", e);
    } finally
    {
      if (fileWriter != null)
        try {
          fileWriter.close();
        } catch (Exception e) {
          throw new HttpAppenderException("Error due to close crash log file writer:", e);
        }
    }
    return tmpFile;
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
