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
package com.dremio.support.diagnostics;

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

public class CustomLogFormatter extends Formatter {

  private static final String emptyLogger = "";

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

  private Stream<String> stackTraceString(final Stream<StackTraceElement> element) {
    return element.map(x -> String.format("\tat %s", x));
  }

  private String[] toArray(final Stream<String> stream) {
    return stream.toArray(String[]::new);
  }

  private String getMessage(final Throwable cause) {
    return cause.getMessage();
  }

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
