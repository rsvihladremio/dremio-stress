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

import static org.junit.Assert.*;

import org.junit.Test;

/** Tests for the {@link Sequence} class. */
public class SequenceTest {

  /** Tests that a new Sequence object is initialized correctly with default values. */
  @Test
  public void testDefaultState() {
    Sequence sequence = new Sequence();
    assertNull("getName should return null by default", sequence.getName());
    assertEquals("getStart should return 0 by default", 0, sequence.getStart());
    assertEquals("getEnd should return 0 by default", 0, sequence.getEnd());
    assertEquals("getStep should return 0 by default", 0, sequence.getStep());
  }

  /** Tests the getter and setter methods for all fields. */
  @Test
  public void testGettersAndSetters() {
    Sequence sequence = new Sequence();

    String name = "test_sequence";
    int start = 10;
    int end = 100;
    int step = 5;

    // Set values using setters
    sequence.setName(name);
    sequence.setStart(start);
    sequence.setEnd(end);
    sequence.setStep(step);

    // Verify values using getters
    assertEquals(
        "setName should set and getName should return the correct value", name, sequence.getName());
    assertEquals(
        "setStart should set and getStart should return the correct value",
        start,
        sequence.getStart());
    assertEquals(
        "setEnd should set and getEnd should return the correct value", end, sequence.getEnd());
    assertEquals(
        "setStep should set and getStep should return the correct value", step, sequence.getStep());
  }
}
