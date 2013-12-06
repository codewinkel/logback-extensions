package com.mwinkelmann.logging.appender;

/**
 * Default configuration for {@link AbstractHttpAppender}
 * 
 * @author Mike Winkelmann
 *
 */
public class AbstractHttpAppenderConfig {

  public static final int DEFAULT_SUCCESS_CODE_MAX = 299;
  public static final int DEFAULT_SUCCESS_CODE_MIN = 200;
  public static final int DEFAULT_QUEUE_SIZE = 0;

  public static final boolean DEFAULT_WARN_NOTIFY = false;
  public static final boolean DEFAULT_ERROR_NOTIFY = false;
  public static final boolean DEFAULT_INFO_NOTIFY = false;
  public static final boolean DEFAULT_DEBUG_NOTIFY = false;
  public static final boolean DEFAULT_TRACE_NOTIFY = false;

}
