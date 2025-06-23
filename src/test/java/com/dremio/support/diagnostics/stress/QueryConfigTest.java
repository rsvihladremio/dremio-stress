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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Tests for the {@link QueryConfig} class. */
public class QueryConfigTest {

  /** Tests that a new QueryConfig object is initialized correctly with default values. */
  @Test
  public void testDefaultState() {
    QueryConfig config = new QueryConfig();
    assertNull("getQuery should return null by default", config.getQuery());
    assertNull("getQueryGroup should return null by default", config.getQueryGroup());
    assertEquals("getFrequency should return 0 by default", 0, config.getFrequency());
    assertNull("getParameters should return null by default", config.getParameters());
    assertNull("getSequence should return null by default", config.getSequence());
    assertNull("getSqlContext should return null by default", config.getSqlContext());
  }

  /** Tests the getter and setter methods for all fields. */
  @Test
  public void testGettersAndSetters() {
    QueryConfig config = new QueryConfig();

    String query = "SELECT * FROM some_table WHERE id = ${sequence.value}";
    String queryGroup = "test_group";
    int frequency = 5;
    Map<String, List<Object>> parameters = new HashMap<>();
    parameters.put("param1", Collections.singletonList("value1")); // Example with one value
    parameters.put("param2", Arrays.asList("valueA", 123, true)); // Example with multiple types
    Sequence sequence = new Sequence();
    sequence.setName("sequence");
    sequence.setStart(1);
    sequence.setEnd(10);
    sequence.setStep(1);
    List<String> sqlContext = new ArrayList<>();
    sqlContext.add("catalog.schema");

    // Set values using setters
    config.setQuery(query);
    config.setQueryGroup(queryGroup);
    config.setFrequency(frequency);
    config.setParameters(parameters);
    config.setSequence(sequence);
    config.setSqlContext(sqlContext);

    // Verify values using getters
    assertEquals(
        "setQuery should set and getQuery should return the correct value",
        query,
        config.getQuery());
    assertEquals(
        "setQueryGroup should set and getQueryGroup should return the correct value",
        queryGroup,
        config.getQueryGroup());
    assertEquals(
        "setFrequency should set and getFrequency should return the correct value",
        frequency,
        config.getFrequency());
    assertSame(
        "setParameters should set and getParameters should return the same map instance",
        parameters,
        config.getParameters());
    assertSame(
        "setSequence should set and getSequence should return the same sequence instance",
        sequence,
        config.getSequence());
    assertSame(
        "setSqlContext should set and getSqlContext should return the same list instance",
        sqlContext,
        config.getSqlContext());
  }
}
