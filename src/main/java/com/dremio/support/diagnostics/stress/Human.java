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

import java.text.NumberFormat;

/**
 * Provides utility methods for making numbers and durations more human-readable by converting large
 * values (like bytes or milliseconds) into more appropriate units (KB, MB, GB, TB, or seconds,
 * minutes, hours, days).
 */
public class Human {

  /** A second measured in milliseconds. */
  private static final long SECOND = 1000;

  /** A minute measured in milliseconds. */
  private static final long MINUTE = 60 * SECOND;

  /** An hour measured in milliseconds. */
  private static final long HOUR = 60 * MINUTE;

  /** A day measured in milliseconds. */
  private static final long DAY = 24 * HOUR;

  private static final long kb = 1024L;
  private static final double kbd = 1024.0;

  /** Prevents instantiation of the utility class. */
  private Human() {}

  /**
   * Converts a duration given in nanoseconds to a human-readable string.
   *
   * @param durationNanos The duration in nanoseconds.
   * @return A human-readable string representation of the duration (e.g., "1.25 seconds", "5.50
   *     minutes").
   */
  public static String getHumanDurationFromNanos(final long durationNanos) {
    return getHumanDurationFromMillis(durationNanos / 1_000_000);
  }

  /**
   * Converts a duration given in milliseconds to a human-readable string. The output unit will be
   * days, hours, minutes, seconds, or milliseconds, whichever is most appropriate.
   *
   * @param durationMillis The duration in milliseconds.
   * @return A human-readable string representation of the duration (e.g., "1 day", "2.50 hours",
   *     "30 seconds").
   */
  public static String getHumanDurationFromMillis(final long durationMillis) {
    if (durationMillis == DAY) {
      return "1 day";
    }
    if (durationMillis > DAY) {
      return String.format("%.2f days", (double) durationMillis / DAY);
    }
    if (durationMillis == HOUR) {
      return "1 hour";
    }
    if (durationMillis > HOUR) {
      return String.format("%.2f hours", (double) durationMillis / HOUR);
    }
    if (durationMillis == MINUTE) {
      return "1 minute";
    }
    if (durationMillis > MINUTE) {
      return String.format("%.2f minutes", (double) durationMillis / MINUTE);
    }
    if (durationMillis == SECOND) {
      return "1 second";
    }
    if (durationMillis > SECOND) {
      return String.format("%.2f seconds", (double) durationMillis / SECOND);
    }
    long oneMillisecond = 1;
    if (durationMillis == oneMillisecond) {
      return "1 millisecond";
    }
    return String.format("%s milliseconds", durationMillis);
  }

  /**
   * Converts a byte count into a human-readable string using binary units (1024 bytes = 1 KB). The
   * output unit will be bytes, KB, MB, GB, or TB, whichever is most appropriate.
   *
   * @see <a href="https://wiki.ubuntu.com/UnitsPolicy">Ubuntu Units Policy</a>
   * @param bytes The number of bytes.
   * @return A human-readable string representation of the byte count (e.g., "1024 bytes", "1.50
   *     mb", "20.75 gb").
   */
  public static String getHumanBytes1024(final long bytes) {

    if (bytes > kb * kb * kb * kb) {
      return String.format("%.2f tb", bytes / (kb * kb * kb * kbd));
    }
    if (bytes > kb * kb * kb) {
      return String.format("%.2f gb", bytes / (kb * kb * kbd));
    }
    if (bytes > kb * kb) {
      return String.format("%.2f mb", bytes / (kb * kbd));
    }
    if (bytes > kb) {
      return String.format("%.2f kb", bytes / kbd);
    }
    return String.format("%s bytes", bytes);
  }

  /**
   * Formats a long integer as a human-readable string with locale-specific grouping separators.
   *
   * @param number The number to format.
   * @return A human-readable string representation of the number (e.g., "1,234,567").
   */
  public static String getHumanNumber(final long number) {
    return NumberFormat.getInstance().format(number);
  }

  /**
   * Formats a double as a human-readable string with locale-specific grouping separators and a
   * maximum of two decimal places.
   *
   * @param number The number to format.
   * @return A human-readable string representation of the number (e.g., "1,234.56").
   */
  public static String getHumanNumber(final double number) {
    final NumberFormat format = NumberFormat.getInstance();
    format.setMaximumFractionDigits(2);
    return format.format(number);
  }

  /**
   * Converts a byte count into a human-readable string using decimal units (1000 bytes = 1 KB). The
   * output unit will be bytes, KB, MB, GB, or TB, whichever is most appropriate.
   *
   * @param bytes The number of bytes.
   * @return A human-readable string representation of the byte count (e.g., "1000 bytes", "1.50
   *     kB", "20.75 gB").
   */
  public static String getHumanBytes1000(final long bytes) {
    long kb = 1000;
    double kbd = 1000.0;
    if (bytes > kb * kb * kb * kb) {
      return String.format("%.2f tb", bytes / (kb * kb * kb * kbd));
    }
    if (bytes > kb * kb * kb) {
      return String.format("%.2f gb", bytes / (kb * kb * kbd));
    }
    if (bytes > kb * kb) {
      return String.format("%.2f mb", bytes / (kb * kbd));
    }
    if (bytes > kb) {
      return String.format("%.2f kb", bytes / kbd);
    }
    return String.format("%s bytes", bytes);
  }
}
