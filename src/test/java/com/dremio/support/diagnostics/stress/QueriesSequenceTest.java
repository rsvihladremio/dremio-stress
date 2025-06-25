package com.dremio.support.diagnostics.stress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class QueriesSequenceTest {

  @Test
  public void testRandomToString() {
    assertEquals("RANDOM", QueriesSequence.RANDOM.toString());
  }

  @Test
  public void testSequentialToString() {
    assertEquals("SEQUENTIAL", QueriesSequence.SEQUENTIAL.toString());
  }
}
