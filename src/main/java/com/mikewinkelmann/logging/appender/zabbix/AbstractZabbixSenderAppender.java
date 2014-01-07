package com.mikewinkelmann.logging.appender.zabbix;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.google.common.base.Preconditions;
import com.mikewinkelmann.logging.appender.http.AbstractHttpAppender;
import com.mikewinkelmann.logging.appender.http.AbstractHttpAppenderConfig;

/**
 * Sends an error message to the zabbix backend. 
 * So Zabbix can visualize the exceptions and inform you if necessary.
 * 
 * @author Mike Winkelmann
 *
 */
public abstract class AbstractZabbixSenderAppender extends AppenderBase<ILoggingEvent> implements Runnable {

  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractHttpAppender.class);

  private BlockingQueue<ILoggingEvent> queue;
  private Future<?> task;

  // configuration params (required)
  private String zabbixHostName;
  private String zabbixServerAddress;
  private int zabbixServerPort;

  // configuration params (optional because defaults are set)
  private boolean error = AbstractHttpAppenderConfig.DEFAULT_ERROR;
  private boolean warn = AbstractHttpAppenderConfig.DEFAULT_WARN;
  private boolean info = AbstractHttpAppenderConfig.DEFAULT_INFO;
  private boolean debug = AbstractHttpAppenderConfig.DEFAULT_DEBUG;
  private boolean trace = AbstractHttpAppenderConfig.DEFAULT_TRACE;
  private int queueSize = AbstractHttpAppenderConfig.DEFAULT_QUEUE_SIZE;

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
              this.execute(event);
            break;
          case Level.WARN_INT:
            if (warn)
              this.execute(event);
            break;
          case Level.INFO_INT:
            if (info)
              this.execute(event);
            break;
          case Level.DEBUG_INT:
            if (debug)
              this.execute(event);
            break;
          case Level.TRACE_INT:
            if (trace)
              this.execute(event);
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

  private void execute(ILoggingEvent event) {
    //    ZABBIX_SENDER
    //
    //    Section: Maintenance Commands (8)
    //    Updated: 5 July 2011
    //    Index Return to Main Contents
    //     
    //    NAME
    //
    //    zabbix_sender - Zabbix sender utility.  
    //    SYNOPSIS
    //
    //    zabbix_sender [-hpzvIV] {-kso | [-T] -i <inputfile>} [-c <config-file>]  
    //    DESCRIPTION
    //
    //    zabbix_sender is a command line utility for sending data to a remote Zabbix server. On the Zabbix server an item of type Zabbix trapper should be created with corresponding key. Note that incoming values will only be accepted from hosts specified in Allowed hosts field for this item.
    //     
    //
    //    Options
    //
    //    -c, --config <config-file>
    //    Specify agent configuration file for reading server details.
    //    -z, --zabbix-server <server>
    //    Hostname or IP address of Zabbix server.
    //    -p, --port <port>
    //    Specify port number of server trapper running on the server. Default is 10051.
    //    -s, --host <host>
    //    Specify host name as registered in Zabbix front-end. Host IP address and DNS name will not work.
    //    -I, --source-address <IP>
    //    Specify source IP address.
    //    -k, --key <key>
    //    Specify item key to send value to.
    //    -o, --value <value>
    //    Specify value.
    //    -i, --input-file <inputfile>
    //    Load values from input file. Specify - for standard input. Each line of file contains whitespace delimited: <hostname> <key> <value>. Specify - in <hostname> to use hostname from configuration file or --host argument.
    //    -T, --with-timestamps
    //    Each line of file contains whitespace delimited: <hostname> <key> <timestamp> <value>. This can be used with --input-file option. Timestamp should be specified in Unix timestamp format.
    //    -r, --real-time
    //    Send values one by one as soon as they are received. This can be used when reading from standard input.
    //    -v, --verbose
    //    Verbose mode, -vv for more details.
    //    -h, --help
    //    Display this help and exit.
    //    -V, --version
    //    Output version information and exit.
    //     
    //    EXAMPLES
    //
    //    zabbix_sender -c /etc/zabbix/zabbix_agentd.conf -s Monitored Host -k mysql.queries -o 342.45
    //    Send 342.45 as the value for mysql.queries key in Monitored Host host using Zabbix server defined in agent daemon configuration file.
    //
    //    zabbix_sender -z 192.168.1.113 -i data_values.txt
    //
    //    Send values from file data_values.txt to server with IP 192.168.1.113. Host names and keys are defined in the file.
    //
    //    echo - hw.serial.number 1287872261 SQ4321ASDF | zabbix_sender -c /etc/zabbix/zabbix_agentd.conf -T -i -
    //
    //    Send a timestamped value from the commandline to Zabbix server, specified in the agent daemon configuration file. Dash in the input data indicates that hostname also should be used from the same configuration file.
    //
    //     
  }

}
