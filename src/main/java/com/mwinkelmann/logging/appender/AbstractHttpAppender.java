package com.mwinkelmann.logging.appender;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.util.CloseUtil;

import com.google.common.base.Preconditions;
import com.mwinkelmann.logging.appender.exception.HttpAppenderException;

/**
 * An abstract base for module specific {@code HttpAppender}
 * implementations in other logback modeules.
 * 
 * @author Mike Winkelmann
 *
 */
public abstract class AbstractHttpAppender extends AppenderBase<ILoggingEvent> implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(AbstractHttpAppender.class);
  private CloseableHttpClient httpClient;
  private BlockingQueue<ILoggingEvent> queue;

  // default params
  private static final int DEFAULT_SUCCESS_CODE_MAX = 299;
  private static final int DEFAULT_SUCCESS_CODE_MIN = 200;
  public static final int DEFAULT_QUEUE_SIZE = 0;

  // configure params (required)
  protected String requestUrl = null;

  // configure params (optional)
  private boolean errorNotify = true;
  private boolean warnNotify = true;
  private boolean infoNotify = true;
  private boolean debugNotify = true;
  private boolean traceNotify = true;
  private int successStatusCodeMin = DEFAULT_SUCCESS_CODE_MIN;
  private int successStatusCodeMax = DEFAULT_SUCCESS_CODE_MAX;
  private Map<String, String> keyToParameterMap = null;
  private int queueSize = DEFAULT_QUEUE_SIZE;

  protected AbstractHttpAppender()
  {}

  @Override
  public void start() {
    if (this.isStarted())
      return;
    Preconditions.checkNotNull(this.requestUrl, "RequestUrl must not be null");
    Preconditions.checkArgument(this.queueSize < 0, "Queue size must be non negative");
    httpClient = createHttpClient();
    queue = createQueue();
    super.start();
  }

  @Override
  public void stop() {
    if (!this.isStarted())
      return;
    CloseUtil.closeQuietly(httpClient);
    httpClient = null;
    super.stop();
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (event == null || !isStarted()) return;
    queue.offer(event);
  }

  @Override
  public final void run() {
    try {
      while (!Thread.currentThread().isInterrupted()) {

        if (this.httpClient == null)
          break;

        processQueue();
      }
    } catch (InterruptedException e) {
      // nothing to do, because we will exit now
    }
  }

  private BlockingQueue<ILoggingEvent> createQueue() {
    return queueSize <= 0
      ? new SynchronousQueue<ILoggingEvent>()
      : new ArrayBlockingQueue<ILoggingEvent>(queueSize);
  }

  private CloseableHttpClient createHttpClient() {
    RequestConfig defaultRequestConfig = RequestConfig.custom()
      .setSocketTimeout(5000)
      .setConnectTimeout(5000)
      .setConnectionRequestTimeout(5000)
      .build();

    return HttpClients.custom()
      .setDefaultRequestConfig(defaultRequestConfig)
      .build();
  }

  private void processQueue() throws InterruptedException
  {
    try {
      while (true)
      {
        final ILoggingEvent event = this.queue.take();
        switch (event.getLevel().levelInt) {
          case Level.ERROR_INT:
            if (errorNotify)
              this.createAndExecuteRequest(event);
            break;
          case Level.WARN_INT:
            if (warnNotify)
              this.createAndExecuteRequest(event);
            break;
          case Level.INFO_INT:
            if (infoNotify)
              this.createAndExecuteRequest(event);
            break;
          case Level.DEBUG_INT:
            if (debugNotify)
              this.createAndExecuteRequest(event);
            break;
          case Level.TRACE_INT:
            if (traceNotify)
              this.createAndExecuteRequest(event);
            break;
          default:
            logger.error("Unknown logging level: " + event.getLevel().levelStr);
            break;
        }
      }
    } catch (Exception e) {
      AbstractHttpAppender.logger.error("Exception caught:", e);
    } finally
    {
      this.httpClient = null;
      AbstractHttpAppender.logger.info("connection closed");
    }
  }

  private void createAndExecuteRequest(ILoggingEvent event) {
    try {
      HttpRequestBase createHttpRequest = this.createHttpRequest(event);
      this.executeHttpRequest(createHttpRequest);
    } catch (HttpAppenderException e) {
      logger.error("Appender error:", e);
    }
  }

  public abstract HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException;

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
