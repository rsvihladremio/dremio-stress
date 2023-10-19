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
 * Provides utility methods for making more easily understandable the output of big numbers
 * typically bytes and time units
 */
public class Human {

  /** a second measured in milliseconds */
  private static final long SECOND = 1000;

  /** a minute measured in milliseconds */
  private static final long MINUTE = 60 * SECOND;

  /** an hour measured in milliseconds */
  private static final long HOUR = 60 * MINUTE;

  /** a day measured in milliseconds */
  private static final long DAY = 24 * HOUR;

  private static final long kb = 1024L;
  private static final double kbd = 1024.0;

  /** prevent instantiation */
  private Human() {}

  public static String getHumanDurationFromNanos(final long durationNanos) {
    return getHumanDurationFromMillis(durationNanos / 1_000_000);
  }

  /**
   * Convert into a string the appropriate time unit for a time span measured in milliseconds. The
   * maximum unit of measure is days and the minimum is in milliseconds.
   *
   * @param durationMillis duration or time span that is measured in milliseconds
   * @return a string formatted in an easily readable string, the max unit of measure is days out in
   *     days
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
   * Convert bytes into a string measurement of terabytes, gigabytes, megabytes, kilobytes or bytes.
   * Appropriate for RAM, but some would argue not for disk size or network bandwidth. For file size
   * this is largely a matter of preference
   *
   * @see <a href="https://wiki.ubuntu.com/UnitsPolicy">Ubuntu Units Policy</a>
   * @param bytes number of bytes
   * @return formatted string with decimal precision of 2, the largest unit of measure will be
   *     terabytes
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

  public static String getHumanNumber(final long number) {
    return NumberFormat.getInstance().format(number);
  }

  public static String getHumanNumber(final double number) {
    final NumberFormat format = NumberFormat.getInstance();
    format.setMaximumFractionDigits(2);
    return format.format(number);
  }

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
