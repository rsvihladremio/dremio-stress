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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for the {@link QueryGroup} class. */
public class QueryGroupTest {

  /** Tests that a new QueryGroup object is initialized correctly with null values. */
  @Test
  public void testDefaultState() {
    QueryGroup queryGroup = new QueryGroup();
    assertNull(queryGroup.getName(), "getName should return null by default");
    assertNull(queryGroup.getQueries(), "getQueries should return null by default");
  }

  /** Tests the getter and setter methods for all fields. */
  @Test
  public void testGettersAndSetters() {
    QueryGroup queryGroup = new QueryGroup();

    String name = "test_group";
    List<String> queries = new ArrayList<>();
    queries.add("SELECT 1");
    queries.add("SELECT 2");

    // Set values using setters
    queryGroup.setName(name);
    queryGroup.setQueries(queries);

    // Verify values using getters
    assertEquals(
        name,
        queryGroup.getName(),
        "setName should set and getName should return the correct value");
    assertSame(
        queries,
        queryGroup.getQueries(),
        "setQueries should set and getQueries should return the same list instance");
    assertEquals(
        2, queryGroup.getQueries().size(), "getQueries list size should match the set list size");
  }
}
