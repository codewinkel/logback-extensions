package com.mwinkelmann.logging.appender;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.mwinkelmann.logging.appender.exception.HttpAppenderException;

/**
 * An abstract base for module specific {@code HttpAppender}
 * implementations in other logback modeules.
 * 
 * @author Mike Winkelmann
 *
 */
public abstract class AbstractHttpAppender extends AppenderBase<ILoggingEvent> {

  private static final Logger logger = LoggerFactory.getLogger(AbstractHttpAppender.class);

  protected HttpClient httpClient;

  private static final int DEFAULT_SUCCESS_CODE_MAX = 299;
  private static final int DEFAULT_SUCCESS_CODE_MIN = 200;

  // configure params
  private boolean errorNotify = true;
  private boolean warnNotify = true;
  private boolean infoNotify = true;
  private boolean debugNotify = true;
  private boolean traceNotify = true;
  private int successStatusCodeMin = DEFAULT_SUCCESS_CODE_MIN;
  private int successStatusCodeMax = DEFAULT_SUCCESS_CODE_MAX;
  private String requestUrl = null;
  private Map<String, String> keyToParameterMap = null;

  protected AbstractHttpAppender() {}

  @Override
  public void start() {
    httpClient = createHttpClient();
    super.start();
  }

  @Override
  public void stop() {
    httpClient = null;
    super.stop();
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (event == null || !isStarted()) return;
    switch (event.getLevel().levelInt) {
      case Level.ERROR_INT:
        if (errorNotify)
          this.createAndExecuteRequest();
        break;
      case Level.WARN_INT:
        if (warnNotify)
          this.createAndExecuteRequest();
        break;
      case Level.INFO_INT:
        if (infoNotify)
          this.createAndExecuteRequest();
        break;
      case Level.DEBUG_INT:
        if (debugNotify)
          this.createAndExecuteRequest();
        break;
      case Level.TRACE_INT:
        if (traceNotify)
          this.createAndExecuteRequest();
        break;
      default:
        logger.error("Unknown logging level: " + event.getLevel().levelStr);
        break;
    }

  }

  private void createAndExecuteRequest() {
    try {
      HttpRequestBase createHttpRequest = this.createHttpRequest();
      this.executeHttpRequest(createHttpRequest);
    } catch (HttpAppenderException e) {
      logger.error("", e);
    }
  }

  public abstract HttpRequestBase createHttpRequest();

  public void executeHttpRequest(HttpRequestBase httpRequest) throws HttpAppenderException
  {
    try {
      HttpResponse proxyResponse = httpClient.execute(httpRequest);

      StatusLine statusLine = proxyResponse.getStatusLine();
      Integer statusCode = statusLine != null ? proxyResponse.getStatusLine().getStatusCode() : null;
      if (statusCode == null || (statusCode.intValue() < successStatusCodeMin
        && statusCode.intValue() > successStatusCodeMax))
      {
        throw new HttpAppenderException("Http request failed. Reason: statusCode="
          + (statusCode != null ? statusCode : "no status code retrieved") + " reasonPhrase="
          + (statusLine != null ? statusLine.getReasonPhrase() : "no reason retrieved!"));
      }

    } catch (Exception exception) {
      throw new HttpAppenderException("Exception caught due to execute http call: ", exception);
    } finally
    {
      httpRequest.releaseConnection();
    }
  }

  private HttpClient createHttpClient() {
    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
    poolingHttpClientConnectionManager.setMaxTotal(32);
    HttpClientBuilder httpClientBuilder =
      HttpClientBuilder
        .create()
        .setConnectionManager(poolingHttpClientConnectionManager);

    return httpClientBuilder.build();
  }

  public void setKeyToParameterMap(Map<String, String> keyToParameterMap) {
    this.keyToParameterMap = keyToParameterMap;
  }

  public Map<String, String> getKeyToParameterMap() {
    return this.keyToParameterMap;
  }

  public void setRequestUrl(String requestUrl) {
    this.requestUrl = requestUrl;
  }

  public String getRequestUrl() {
    return this.requestUrl;
  }

  public void setSuccessStatusCodeMax(int successStatusCodeMax) {
    this.successStatusCodeMax = successStatusCodeMax;
  }

  public int getSuccessStatusCodeMax() {
    return this.successStatusCodeMax;
  }

  public void setSuccessStatusCodeMin(int successStatusCodeMin) {
    this.successStatusCodeMin = successStatusCodeMin;
  }

  public int getSuccessStatusCodeMin() {
    return this.successStatusCodeMin;
  }

  public void setHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public boolean isErrorNotify() {
    return this.errorNotify;
  }

  public void setErrorNotify(boolean errorNotify) {
    this.errorNotify = errorNotify;
  }

  public boolean isWarnNotify() {
    return this.warnNotify;
  }

  public void setWarnNotify(boolean warnNotify) {
    this.warnNotify = warnNotify;
  }

  public boolean isInfoNotify() {
    return this.infoNotify;
  }

  public void setInfoNotify(boolean infoNotify) {
    this.infoNotify = infoNotify;
  }

  public boolean isDebugNotify() {
    return this.debugNotify;
  }

  public void setDebugNotify(boolean debugNotify) {
    this.debugNotify = debugNotify;
  }

  public boolean isTraceNotify() {
    return this.traceNotify;
  }

  public void setTraceNotify(boolean traceNotify) {
    this.traceNotify = traceNotify;
  }

}
