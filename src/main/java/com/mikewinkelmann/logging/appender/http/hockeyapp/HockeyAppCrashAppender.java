package com.mikewinkelmann.logging.appender.http.hockeyapp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

import com.google.common.base.Preconditions;
import com.mikewinkelmann.logging.appender.http.AbstractHttpAppender;
import com.mikewinkelmann.logging.appender.http.exception.HttpAppenderException;

/**
 * Sends the special {@link ILoggingEvent} object to the hockeyapp backend. 
 * So Hockeyapp can visualize the exceptions, opens a bug in bugtracker, informs you if necessary and so on.
 * 
 * @author Mike Winkelmann
 *
 */
public class HockeyAppCrashAppender extends AbstractHttpAppender {

  private static final Logger logger = LoggerFactory.getLogger(HockeyAppCrashAppender.class);

  private HockeyAppCrashAppenderService service;

  // configuration
  private String userId, contact, model, manufacturer, os, version, packageName, apiToken, appId, requestUrl;


  public HockeyAppCrashAppender() {
    this.setRequestUrl(HockeyAppCrashAppenderConfig.HOCKEYAPP_CRASH_API_URL);
  }

  @Override
  public void start() {
    super.start();
    Preconditions.checkNotNull(this.apiToken, "ApiToken must not be null");
    Preconditions.checkNotNull(this.appId, "AppId must not be null");
    Preconditions.checkNotNull(this.packageName, "PackageName must not be null");
    requestUrl =
      this.getRequestUrl().replace(HockeyAppCrashAppenderConfig.HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER, this.appId);
    this.service = new HockeyAppCrashAppenderService(model, manufacturer, os, version, packageName);
  }

  @Override
  public HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException {

    logger.debug("Create HttpRequest for HockeyApp call against crash api Event: " + event.getLevel().levelStr);
    final HttpPost httpRequest = new HttpPost(requestUrl);
    httpRequest.addHeader("X-HockeyAppToken", this.apiToken);

    IThrowableProxy throwableProxy = event.getThrowableProxy();
    long timeStamp = event.getTimeStamp();
    String formattedMessage = event.getFormattedMessage();

    FileBody crashFileBody = this.service.createCrashFileBody(throwableProxy, timeStamp);
    FileBody descriptionFileBody = this.service.createDescriptionFileBody(formattedMessage, timeStamp);
    MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create()
      .addPart("log", crashFileBody)
      .addPart("description", descriptionFileBody);
    //  TODO implement .addPart("attachment", this.createAttatchmentFile(event))
    if (this.userId != null)
      multipartEntityBuilder.addTextBody("userID", this.userId);
    if (this.contact != null)
      multipartEntityBuilder.addTextBody("contact", this.contact);

    HttpEntity multipartEntity = multipartEntityBuilder.build();

    httpRequest.setEntity(multipartEntity);

    return httpRequest;
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
