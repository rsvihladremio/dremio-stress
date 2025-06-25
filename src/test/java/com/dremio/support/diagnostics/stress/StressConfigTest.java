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

/** Tests for the {@link StressConfig} class. */
public class StressConfigTest {

  /** Tests that a new StressConfig object is initialized correctly with null lists. */
  @Test
  public void testDefaultState() {
    StressConfig config = new StressConfig();
    assertNull(config.getQueries(), "getQueries should return null by default");
    assertNull(config.getQueryGroups(), "getQueryGroups should return null by default");
  }

  /** Tests the getter and setter methods for queries and query groups. */
  @Test
  public void testGettersAndSetters() {
    StressConfig config = new StressConfig();

    // Create sample lists
    List<QueryConfig> queries = new ArrayList<>();
    queries.add(new QueryConfig()); // Add a dummy QueryConfig
    queries.add(new QueryConfig()); // Add another dummy QueryConfig

    List<QueryGroup> queryGroups = new ArrayList<>();
    queryGroups.add(new QueryGroup()); // Add a dummy QueryGroup

    // Set the lists
    config.setQueries(queries);
    config.setQueryGroups(queryGroups);

    // Verify using assertSame to check if the exact list references are returned
    assertSame(
        queries,
        config.getQueries(),
        "setQueries should set and getQueries should return the same list instance");
    assertSame(
        queryGroups,
        config.getQueryGroups(),
        "setQueryGroups should set and getQueryGroups should return the same list instance");

    // Also verify content/size using assertEquals for double-check
    assertEquals(
        2, config.getQueries().size(), "getQueries list size should match the set list size");
    assertEquals(
        1,
        config.getQueryGroups().size(),
        "getQueryGroups list size should match the set list size");
  }
}
