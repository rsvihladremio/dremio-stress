package com.dremio.support.diagnostics.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HumanTest {

  @Test
  public void testGetHumanDurationFromNanos() {
    assertEquals("1 millisecond", Human.getHumanDurationFromNanos(1_000_000));
    assertEquals("1 second", Human.getHumanDurationFromNanos(1_000_000_000));
    assertEquals("1.50 seconds", Human.getHumanDurationFromNanos(1_500_000_000));
  }

  @Test
  public void testGetHumanDurationFromMillis() {
    assertEquals("1 millisecond", Human.getHumanDurationFromMillis(1));
    assertEquals("1 second", Human.getHumanDurationFromMillis(1000));
    assertEquals("1.50 seconds", Human.getHumanDurationFromMillis(1500));
    assertEquals("1 minute", Human.getHumanDurationFromMillis(60000));
    assertEquals("1.50 minutes", Human.getHumanDurationFromMillis(90000));
    assertEquals("1 hour", Human.getHumanDurationFromMillis(3600000));
    assertEquals("1.50 hours", Human.getHumanDurationFromMillis(5400000));
    assertEquals("1 day", Human.getHumanDurationFromMillis(86400000));
    assertEquals("1.50 days", Human.getHumanDurationFromMillis(129600000));
  }

  @Test
  public void testGetHumanBytes1024() {
    assertEquals("10 bytes", Human.getHumanBytes1024(10));
    assertEquals("1024 bytes", Human.getHumanBytes1024(1024));
    assertEquals("1.50 kb", Human.getHumanBytes1024(1536));
    assertEquals("1024.00 kb", Human.getHumanBytes1024(1024L * 1024));
    assertEquals("1024.00 mb", Human.getHumanBytes1024(1024L * 1024 * 1024));
    assertEquals("1024.00 gb", Human.getHumanBytes1024(1024L * 1024 * 1024 * 1024));
  }

  @Test
  public void testGetHumanNumberLong() {
    assertEquals("1,000", Human.getHumanNumber(1000L));
    assertEquals("1,234,567", Human.getHumanNumber(1234567L));
  }

  @Test
  public void testGetHumanNumberDouble() {
    assertEquals("1,000", Human.getHumanNumber(1000.0));
    assertEquals("1,234.57", Human.getHumanNumber(1234.567));
  }

  @Test
  public void testGetHumanBytes1000() {
    assertEquals("10 bytes", Human.getHumanBytes1000(10));
    assertEquals("1000 bytes", Human.getHumanBytes1000(1000));
    assertEquals("1.50 kb", Human.getHumanBytes1000(1500));
    assertEquals("1000.00 kb", Human.getHumanBytes1000(1000L * 1000));
    assertEquals("1000.00 mb", Human.getHumanBytes1000(1000L * 1000 * 1000));
    assertEquals("1000.00 gb", Human.getHumanBytes1000(1000L * 1000 * 1000 * 1000));
  }
}
