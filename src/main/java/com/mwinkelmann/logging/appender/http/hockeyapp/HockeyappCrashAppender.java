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

  private static final String HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER = "{APPID}";
  private static final String HOCKEYAPP_CRASH_API_URL = "https://rink.hockeyapp.net/api/2/apps/"
    + HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER + "/crashes/upload";

  // TODO configurable
  private SimpleDateFormat dateFormat = null;

  // configuration
  private String userId, email, model, manufacturer, os, version, packageName, apiToken, appId, requestUrl;

  public HockeyappCrashAppender() {
    this.setRequestUrl(HOCKEYAPP_CRASH_API_URL);
    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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

    HttpEntity multipartEntity = MultipartEntityBuilder.create()
      .addPart("log", crashFileBody)
      //  TODO implement .addBinaryBody("description", this.createDescriptionLogFile(event))
      //  TODO implement .addBinaryBody("attachment", this.createAttatchmentFile(event))
      .addTextBody("userID", this.userId)
      .addTextBody("contact", this.email)
      .setStrictMode()
      .build();

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
      content.append("Version: ").append(this.version).append("\n");
      content.append("OS: ").append(this.os).append("\n");
      content.append("Manufacturer: ").append(this.manufacturer).append("\n");
      content.append("Model: ").append(this.model).append("\n");
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

  public String getUserId() {
    return this.userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getEmail() {
    return this.email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getModel() {
    return this.model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getManufacturer() {
    return this.manufacturer;
  }

  public void setManufacturer(String manufacturer) {
    this.manufacturer = manufacturer;
  }

  public String getOs() {
    return this.os;
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getVersion() {
    return this.version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getPackageName() {
    return this.packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getApiToken() {
    return this.apiToken;
  }

  public void setApiToken(String apiToken) {
    this.apiToken = apiToken;
  }

  public String getAppId() {
    return this.appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

}
