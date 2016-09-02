package com.mikewinkelmann.logging.appender.http;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.util.CloseUtil;
import com.google.common.base.Preconditions;
import com.mikewinkelmann.logging.appender.LoggingLevel;
import com.mikewinkelmann.logging.appender.http.exception.HttpAppenderException;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

/**
 * An abstract base for module specific {@code HttpAppender}
 * implementations in other logback modules.
 *
 * @author Mike Winkelmann
 */
public abstract class AbstractHttpAppender extends AppenderBase<ILoggingEvent> implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpAppender.class);

    private CloseableHttpClient httpClient;
    private BlockingQueue<ILoggingEvent> queue;
    private Future<?> task;

    // configuration params (required)
    private String requestUrl = null;

    // configuration params (optional because defaults are set)
    private boolean error = AbstractHttpAppenderConfig.DEFAULT_ERROR;
    private boolean warn = AbstractHttpAppenderConfig.DEFAULT_WARN;
    private boolean info = AbstractHttpAppenderConfig.DEFAULT_INFO;
    private boolean debug = AbstractHttpAppenderConfig.DEFAULT_DEBUG;
    private boolean trace = AbstractHttpAppenderConfig.DEFAULT_TRACE;
    private int successStatusCodeMin = AbstractHttpAppenderConfig.DEFAULT_SUCCESS_CODE_MIN;
    private int successStatusCodeMax = AbstractHttpAppenderConfig.DEFAULT_SUCCESS_CODE_MIN;
    private int queueSize = AbstractHttpAppenderConfig.DEFAULT_QUEUE_SIZE;

    protected AbstractHttpAppender() {
    }

    @Override
    public void start() {
        if (this.isStarted())
            return;
        Preconditions.checkNotNull(this.requestUrl, "RequestUrl must not be null");
        Preconditions.checkArgument(this.queueSize >= 0, "Queue size must be non negative");
        httpClient = createHttpClient();
        queue = createQueue();
        this.task = this.getContext().getExecutorService().submit(this);
        super.start();
    }

    @Override
    public void stop() {
        if (!this.isStarted())
            return;
        CloseUtil.closeQuietly(httpClient);
        this.task.cancel(true);
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event == null || !isStarted()) {
            return;
        }
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
                .setSocketTimeout(AbstractHttpAppenderConfig.HTTP_SOCKET_TIMEOUT)
                .setConnectTimeout(AbstractHttpAppenderConfig.HTTP_CONNECTION_TIMEOUT)
                .setConnectionRequestTimeout(AbstractHttpAppenderConfig.HTTP_CONNECTION_REQUEST_TIMEOUT)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(defaultRequestConfig)
                .build();
    }

    private void processQueue() throws InterruptedException {
        try {
            while (true) {
                final ILoggingEvent event = this.queue.take();
                switch (event.getLevel().levelInt) {
                    case Level.ERROR_INT:
                        if (error)
                            this.createAndExecuteRequest(event);
                        break;
                    case Level.WARN_INT:
                        if (warn)
                            this.createAndExecuteRequest(event);
                        break;
                    case Level.INFO_INT:
                        if (info)
                            this.createAndExecuteRequest(event);
                        break;
                    case Level.DEBUG_INT:
                        if (debug)
                            this.createAndExecuteRequest(event);
                        break;
                    case Level.TRACE_INT:
                        if (trace)
                            this.createAndExecuteRequest(event);
                        break;
                    default:
                        logger.error("Unknown logging level: " + event.getLevel().levelStr);
                        break;
                }
            }
        } catch (Exception e) {
            AbstractHttpAppender.logger.error("Exception caught:", e);
        } finally {
            this.httpClient = null;
            AbstractHttpAppender.logger.info("connection closed");
        }
    }

    private void createAndExecuteRequest(ILoggingEvent event) {
        try {
            HttpRequestBase createHttpRequest = this.createHttpRequest(event);
            if (createHttpRequest != null)
                this.executeHttpRequest(createHttpRequest);
        } catch (HttpAppenderException e) {
            logger.error("Appender error:", e);
        }
    }

    public abstract HttpRequestBase createHttpRequest(ILoggingEvent event) throws HttpAppenderException;

    public void executeHttpRequest(HttpRequestBase httpRequest) throws HttpAppenderException {
        try {
            HttpResponse proxyResponse = httpClient.execute(httpRequest);

            StatusLine statusLine = proxyResponse.getStatusLine();
            Integer statusCode = statusLine != null ? proxyResponse.getStatusLine().getStatusCode() : null;
            if (statusCode == null || (statusCode.intValue() < successStatusCodeMin
                    && statusCode.intValue() > successStatusCodeMax)) {
                throw new HttpAppenderException("Http request failed. Reason: statusCode="
                        + (statusCode != null ? statusCode : "no status code retrieved") + " reasonPhrase="
                        + (statusLine != null ? statusLine.getReasonPhrase() : "no reason retrieved!"));
            }

        } catch (Exception exception) {
            throw new HttpAppenderException("Exception caught due to execute http call: ", exception);
        } finally {
            httpRequest.releaseConnection();
        }
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestUrl() {
        return this.requestUrl;
    }

    public void setSuccessStatusCodeMin(int successStatusCodeMin) {
        this.successStatusCodeMin = successStatusCodeMin;
    }

    public void setSuccessStatusCodeMax(int successStatusCodeMax) {
        this.successStatusCodeMax = successStatusCodeMax;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void addLoggingLevel(String state) {
        if (state != null && state.length() > 0
                && LoggingLevel.valueOf(state.toUpperCase()) != null) {
            LoggingLevel logState = LoggingLevel.valueOf(state.toUpperCase());
            switch (logState) {
                case ERROR:
                    this.error = true;
                    break;
                case WARN:
                    this.warn = true;
                    break;
                case INFO:
                    this.info = true;
                    break;
                case DEBUG:
                    this.debug = true;
                    break;
                case TRACE:
                    this.trace = true;
                    break;
            }
        } else {
            throw new IllegalArgumentException("Null ,empty or not the right <LoggingLevel> property. States: "
                    + Arrays.toString(LoggingLevel.values()));
        }
    }
}
