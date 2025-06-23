/**
 * Copyright 2023 Dremio
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.support.diagnostics.stress;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.time.Instant;
import java.util.Arrays;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

/**
 * Custom formatter for log messages in the stress testing tool. This formatter provides detailed
 * and structured log output including: - Standardized log level names (ERROR, WARN, DEBUG, etc.) -
 * Thread ID information - Timestamp in ISO 8601 format - Source class and method names - Detailed
 * exception information including stack traces
 */
public class CustomLogFormatter extends Formatter {

  /** Represents an empty logger name. */
  private static final String emptyLogger = "";

  /**
   * Translates Java logging levels to more standardized logging level names.
   *
   * @param loggingLevel The Java logging level
   * @return A standardized string representation of the logging level
   */
  String getLogLevel(final Level loggingLevel) {
    if (FINEST.equals(loggingLevel)) {
      return "DIAG";
    } else if (FINER.equals(loggingLevel)) {
      return "TRACE";
    } else if (FINE.equals(loggingLevel)) {
      return "DEBUG";
    } else if (WARNING.equals(loggingLevel)) {
      return "WARN";
    } else if (SEVERE.equals(loggingLevel)) {
      return "ERROR";
    } else {
      return loggingLevel.getName();
    }
  }

  /**
   * Formats stack trace elements for consistent display.
   *
   * @param element Stream of stack trace elements
   * @return Stream of formatted stack trace strings
   */
  private Stream<String> stackTraceString(final Stream<StackTraceElement> element) {
    return element.map(x -> String.format("\tat %s", x));
  }

  /**
   * Converts a stream of strings to an array.
   *
   * @param stream The stream to convert
   * @return Array of strings
   */
  private String[] toArray(final Stream<String> stream) {
    return stream.toArray(String[]::new);
  }

  /**
   * Extracts the message from a throwable.
   *
   * @param cause The throwable to extract the message from
   * @return The message from the throwable
   */
  private String getMessage(final Throwable cause) {
    return cause.getMessage();
  }

  /**
   * Formats a log record according to the custom format. The format includes level, logger name,
   * thread ID, timestamp, class name, method name, and message. For records with exceptions, stack
   * traces are also included.
   *
   * @param record The log record to format
   * @return The formatted log message
   */
  @Override
  public String format(final LogRecord record) {
    final String level = getLogLevel(record.getLevel());
    final String loggerName;
    if (!emptyLogger.equals(record.getLoggerName())) {
      loggerName = record.getLoggerName();
    } else {
      loggerName = "root";
    }
    final Throwable thrown = record.getThrown();
    final int threadId = record.getThreadID();
    if (thrown == null) {
      return String.format(
          "%s [%s-%d] %s %s:%s - %s%n",
          level,
          loggerName,
          threadId,
          Instant.ofEpochMilli(record.getMillis()),
          record.getSourceClassName(),
          record.getSourceMethodName(),
          record.getMessage());
    } else {
      final String thrownMessage = thrown.getMessage();
      final Throwable cause = thrown.getCause();
      final StackTraceElement[] stackTrace = thrown.getStackTrace();
      final Stream<StackTraceElement> streamOfStackTraces = Arrays.stream(stackTrace);
      final Stream<String> streamOfMessages = this.stackTraceString(streamOfStackTraces);
      final String[] messages = toArray(streamOfMessages);
      final String stackTraceAsString = String.join("\n", messages);
      if (cause == null) {
        return String.format(
            "%s [%s-%d] %s %s:%s - %s - %s%n%s%n",
            level,
            loggerName,
            threadId,
            Instant.ofEpochMilli(record.getMillis()),
            record.getSourceClassName(),
            record.getSourceMethodName(),
            record.getMessage(),
            thrownMessage,
            stackTraceAsString);
      } else {
        final String causeMessage = getMessage(cause);
        return String.format(
            "%s [%s-%d] %s %s:%s - %s - %s%n%s%n%s%n%s%n",
            level,
            loggerName,
            threadId,
            Instant.ofEpochMilli(record.getMillis()),
            record.getSourceClassName(),
            record.getSourceMethodName(),
            record.getMessage(),
            thrownMessage,
            stackTraceAsString,
            causeMessage,
            stackTraceAsString);
      }
    }
  }
}
