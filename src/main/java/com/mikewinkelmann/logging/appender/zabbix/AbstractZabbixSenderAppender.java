package com.mikewinkelmann.logging.appender.zabbix;

import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.google.common.base.Preconditions;
import com.mikewinkelmann.logging.appender.LoggingLevel;
import com.mikewinkelmann.logging.appender.http.AbstractHttpAppender;

/**
 * Sends an error message to the zabbix backend. 
 * So Zabbix can visualize the exceptions and inform you if necessary.
 * 
 * @author Mike Winkelmann
 *
 */
public abstract class AbstractZabbixSenderAppender extends AppenderBase<ILoggingEvent> implements Runnable {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractHttpAppender.class);
  private static final boolean DEFAULT_WARN = false;
  private static final boolean DEFAULT_ERROR = false;
  private static final boolean DEFAULT_INFO = false;
  private static final boolean DEFAULT_DEBUG = false;
  private static final boolean DEFAULT_TRACE = false;

  private BlockingQueue<ILoggingEvent> queue;
  private Future<?> task;

  // configuration params (required)
  private String zabbixHostName;
  private String zabbixServerAddress;
  private int zabbixServerPort;

  // configuration params (optional because defaults are set)
  private boolean error = DEFAULT_ERROR;
  private boolean warn = DEFAULT_WARN;
  private boolean info = DEFAULT_INFO;
  private boolean debug = DEFAULT_DEBUG;
  private boolean trace = DEFAULT_TRACE;
  private int queueSize = 0;

  protected AbstractZabbixSenderAppender()
  {}

  @Override
  public void start() {
    try {
      if (this.isStarted())
        return;
      Preconditions.checkNotNull(this.zabbixHostName, "ZabbixHostName must be not null");
      Preconditions.checkNotNull(this.zabbixServerAddress, "ZabbixServerAddress must be not null");
      Preconditions.checkArgument(this.zabbixServerPort > 0, "ZabbixServerPort must be non negative");
      Preconditions.checkArgument(this.queueSize >= 0, "Queue size must be non negative");
      queue = createQueue();
      task = this.getContext().getExecutorService().submit(this);
      super.start();
    } catch (Exception e) {
      logger.error("Error due to initializing AbstractZabbixSenderAppender: ", e);
    }
  }

  @Override
  public void stop() {
    if (!this.isStarted())
      return;
    this.task.cancel(true);
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

  private void processQueue() throws InterruptedException
  {
    try {
      while (true)
      {
        final ILoggingEvent event = this.queue.take();
        switch (event.getLevel().levelInt) {
          case Level.ERROR_INT:
            if (error)
              this.executeError(event);
            break;
          case Level.WARN_INT:
            if (warn)
              this.executeWarn(event);
            break;
          case Level.INFO_INT:
            if (info)
              this.executeInfo(event);
            break;
          case Level.DEBUG_INT:
            if (debug)
              this.executeDebug(event);
            break;
          case Level.TRACE_INT:
            if (trace)
              this.executeTrace(event);
            break;
          default:
            logger.error("Unknown logging level: " + event.getLevel().levelStr);
            break;
        }
      }
    } catch (Exception e) {
      AbstractZabbixSenderAppender.logger.error("Exception caught:", e);
    } finally
    {
      AbstractZabbixSenderAppender.logger.info("connection closed");
    }
  }

  private void executeTrace(ILoggingEvent event) {
    // TODO Auto-generated method stub

  }

  private void executeDebug(ILoggingEvent event) {
    // TODO Auto-generated method stub

  }

  private void executeInfo(ILoggingEvent event) {
    // TODO Auto-generated method stub

  }

  private void executeWarn(ILoggingEvent event) {
    // TODO Auto-generated method stub

  }

  private void executeError(ILoggingEvent event) {
    // TODO Auto-generated method stub

  }

  public void setZabbixHostName(String zabbixHostName) {
    this.zabbixHostName = zabbixHostName;
  }

  public void setZabbixServerAddress(String zabbixServerAddress) {
    this.zabbixServerAddress = zabbixServerAddress;
  }

  public void setZabbixServerPort(int zabbixServerPort) {
    this.zabbixServerPort = zabbixServerPort;
  }

  public void setQueueSize(int queueSize) {
    this.queueSize = queueSize;
  }

  public void addLoggingLevel(String state) {
    if (state != null && state.length() > 0
      && LoggingLevel.valueOf(state.toUpperCase()) != null)
    {
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
