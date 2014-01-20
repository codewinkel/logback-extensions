package com.mikewinkelmann.logging.appender.http.hockeyapp;

/**
 * Configuration class for HockeyAppCrashAppender.
 * 
 * @author Mike Winkelman
 *
 */
public class HockeyAppCrashAppenderConfig {

  public static final int MAXIMUM_CRASH_FILE_SIZE_BYTES = 204800; // 200KiloBytes
  public static final int MAXIMUM_DESCRIPTION_FILE_SIZE_BYTES = 204800; // 200KiloBytes
  public static final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss Z";
  public static final String HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER = "{APPID}";
  public static final String HOCKEYAPP_CRASH_API_URL = "https://rink.hockeyapp.net/api/2/apps/"
    + HOCKEYAPP_CRASH_API_URL_APPID_PLACEHOLDER + "/crashes/upload";

}
