package com.mikewinkelmann.logging.appender.http.hockeyapp;

/**
 * Configuration class for HockeyAppCrashAppender.
 * 
 * @author Mike Winkelman
 *
 */
public class HockeyAppCrashAppenderConfig {

  public static final double MAXIMUM_CRASH_FILE_SIZE_KILOBYTES = 200;
  public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static final String HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER = "{APPID}";
  public static final String HOCKEYAPP_CRASH_API_URL = "https://rink.hockeyapp.net/api/2/apps/"
    + HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER + "/crashes/upload";

}
