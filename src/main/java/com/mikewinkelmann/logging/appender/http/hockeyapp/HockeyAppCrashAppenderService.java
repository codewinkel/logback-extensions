package com.mikewinkelmann.logging.appender.http.hockeyapp;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import com.google.common.base.Strings;
import com.mikewinkelmann.logging.appender.http.exception.HttpAppenderException;
import org.apache.http.entity.mime.content.FileBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Service to create all specific logs, formattings, ... for HockeyApp.
 *
 * @author Mike Winkelmann
 */
public class HockeyAppCrashAppenderService {

    private static final Logger logger = LoggerFactory.getLogger(HockeyAppCrashAppenderService.class);

    private SimpleDateFormat dateFormat = null;
    private String model, manufacturer, os, version, packageName;

    public HockeyAppCrashAppenderService(String model, String manufacturer, String os, String version, String packageName) {
        this.dateFormat = new SimpleDateFormat(HockeyAppCrashAppenderConfig.DATE_FORMAT);
        this.model = model;
        this.manufacturer = manufacturer;
        this.os = os;
        this.version = version;
        this.packageName = packageName;
    }

    FileBody createCrashFileBody(IThrowableProxy throwableProxy, long timeStamp)
            throws HttpAppenderException {
        logger.debug("Create crash log file body");
        File crashFile = this.createCrashLogFile(throwableProxy, timeStamp);
        return new FileBody(crashFile);
    }

    FileBody createDescriptionFileBody(String formattedMessage, long timeStamp)
            throws HttpAppenderException {
        logger.debug("Create description log file body");
        File crashFile = this.createDescriptionLogFile(formattedMessage, timeStamp);
        return new FileBody(crashFile);
    }

    private File createCrashLogFile(IThrowableProxy throwableProxy, long timestamp)
            throws HttpAppenderException {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        File tmpFile = null;
        boolean errorOnClose = false;
        try {
            StringBuffer content = new StringBuffer(HockeyAppCrashAppenderConfig.MAXIMUM_CRASH_FILE_SIZE_BYTES);
            content.append("Package: ").append(this.packageName).append("\n");
            content.append("Version: ").append(Strings.nullToEmpty(this.version)).append("\n");
            content.append("OS: ").append(Strings.nullToEmpty(this.os)).append("\n");
            content.append("Manufacturer: ").append(Strings.nullToEmpty(this.manufacturer)).append("\n");
            content.append("Model: ").append(Strings.nullToEmpty(this.model)).append("\n");
            content.append("Date: ").append(dateFormat.format(new Date(timestamp))).append("\n");
            content.append("\n");
            if (throwableProxy != null)
                content.append(this.parseStringArrayMessage(throwableProxy)).append("\n");
            content.trimToSize();

            tmpFile = File.createTempFile("exception", ".tmp.log");
            fileWriter = new FileWriter(tmpFile);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content.toString());
        } catch (Exception e) {
            throw new HttpAppenderException("Error due to create crash log file:", e);
        } finally {
            errorOnClose = closeBufferedWriter(bufferedWriter)
                    && closeFileWriter(fileWriter);
        }
        if (errorOnClose) {
            throw new HttpAppenderException("Error closing FileWriter or BufferedWriter");
        }
        logger.debug("Created crash log file content in tempFile: " + tmpFile.getAbsolutePath());
        return tmpFile;
    }

    private File createDescriptionLogFile(String formattedMessage, long timestamp)
            throws HttpAppenderException {
        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;
        File tmpFile = null;
        boolean errorOnClose = false;
        try {
            StringBuffer content = new StringBuffer(HockeyAppCrashAppenderConfig.MAXIMUM_DESCRIPTION_FILE_SIZE_BYTES);

            content.append("Description: ").append(formattedMessage).append("\n");
            content.append("Date: ").append(dateFormat.format(new Date(timestamp))).append("\n");
            content.append("\n");
            content.trimToSize();

            tmpFile = File.createTempFile("description", ".tmp.log");
            fileWriter = new FileWriter(tmpFile);
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(content.toString());
        } catch (Exception e) {
            throw new HttpAppenderException("Error due to create description log file:", e);
        } finally {
            errorOnClose = closeBufferedWriter(bufferedWriter)
                    && closeFileWriter(fileWriter);
        }
        if (errorOnClose) {
            throw new HttpAppenderException("Error closing FileWriter or BufferedWriter");
        }
        logger.debug("Created crash log file content in tempFile: " + tmpFile.getAbsolutePath());
        return tmpFile;
    }

    private boolean closeBufferedWriter(BufferedWriter bufferedWriter) {
        if (bufferedWriter == null) {
            return true;
        }
        boolean closed = true;
        try {
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            closed = false;
        }
        return closed;
    }

    private boolean closeFileWriter(FileWriter fileWriter) {
        if (fileWriter == null) {
            return true;
        }
        boolean closed = true;
        try {
            fileWriter.close();
        } catch (IOException e) {
            closed = false;
        }
        return closed;
    }

    private String parseStringArrayMessage(IThrowableProxy throwableProxy) {
        String throwableClassName = throwableProxy.getClassName();
        String throwableMessage = throwableProxy.getMessage();
        StackTraceElementProxy[] stackTraceElementProxyArray = throwableProxy.getStackTraceElementProxyArray();
        logger.debug("Parse exception message with"
                + stackTraceElementProxyArray.length + "elements to create the correct crash log file.");
        StringBuffer buffer = new StringBuffer();
        createFirstLine(throwableClassName, throwableMessage, buffer);

        for (StackTraceElementProxy stackColumn : stackTraceElementProxyArray) {
            buffer.append("\t").append(stackColumn.getSTEAsString()).append("\n");
        }
        return buffer.toString();
    }

    private void createFirstLine(String throwableClassName, String throwableMessage, StringBuffer buffer) {
        if (throwableClassName != null)
            buffer.append(throwableClassName);
        if (throwableMessage != null)
            buffer.append(" ").append(throwableMessage);
        if (buffer.length() > 0)
            buffer.append("\n");
    }

}
