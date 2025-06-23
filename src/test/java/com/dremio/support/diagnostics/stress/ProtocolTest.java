package com.dremio.support.diagnostics.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ProtocolTest {

  @Test
  public void testToString() {
    assertEquals("HTTP", Protocol.HTTP.toString());
    assertEquals("JDBC", Protocol.JDBC.toString());
    assertEquals("LegacyJDBC", Protocol.LegacyJDBC.toString());
  }
}
