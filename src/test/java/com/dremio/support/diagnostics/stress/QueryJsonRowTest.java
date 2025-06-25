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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Tests for the {@link QueryJsonRow} class. */
public class QueryJsonRowTest {

  /** Tests that a new QueryJsonRow object is initialized correctly with null values. */
  @Test
  public void testDefaultState() {
    QueryJsonRow row = new QueryJsonRow();
    assertNull(row.getQueryText(), "getQueryText should return null by default");
    assertNull(row.getOutcome(), "getOutcome should return null by default");
    assertNull(row.getContext(), "getContext should return null by default");
    assertNull(row.getUsername(), "getUsername should return null by default");
    assertNull(row.getQueryId(), "getQueryId should return null by default");
  }

  /** Tests the getter and setter methods for all fields. */
  @Test
  public void testGettersAndSetters() {
    QueryJsonRow row = new QueryJsonRow();

    String queryText = "SELECT * FROM table";
    String outcome = "SUCCESS";
    String context = "context.schema";
    String username = "test_user";
    String queryId = "abc-123-def-456";

    // Set values using setters
    row.setQueryText(queryText);
    row.setOutcome(outcome);
    row.setContext(context);
    row.setUsername(username);
    row.setQueryId(queryId);

    // Verify values using getters
    assertEquals(
        queryText,
        row.getQueryText(),
        "setQueryText should set and getQueryText should return the correct value");
    assertEquals(
        outcome,
        row.getOutcome(),
        "setOutcome should set and getOutcome should return the correct value");
    assertEquals(
        context,
        row.getContext(),
        "setContext should set and getContext should return the correct value");
    assertEquals(
        username,
        row.getUsername(),
        "setUsername should set and getUsername should return the correct value");
    assertEquals(
        queryId,
        row.getQueryId(),
        "setQueryId should set and getQueryId should return the correct value");
  }
}
