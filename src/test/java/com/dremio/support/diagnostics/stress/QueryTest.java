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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class QueryTest {

  @Test
  public void testDefaultState() {
    Query query = new Query();
    assertNull(query.getQueryText(), "Query text should be null by default");
    assertNull(query.getContext(), "Context should be null by default");
  }

  @Test
  public void testGettersAndSetters() {
    Query query = new Query();

    // Test query text
    String queryText = "SELECT 1";
    query.setQueryText(queryText);
    assertEquals(queryText, query.getQueryText(), "Query text should match the set value");
    assertSame(queryText, query.getQueryText(), "Query text reference should be the same");

    // Test context
    Collection<String> context = Arrays.asList("schema1", "schema2");
    query.setContext(context);
    assertSame(context, query.getContext(), "Context collection reference should be the same");
    assertEquals(context.size(), query.getContext().size(), "Context collection size should match");
    assertEquals(context, query.getContext(), "Context collection content should match");

    // Test setting context to null
    query.setContext(null);
    assertNull(query.getContext(), "Context should be null after setting to null");

    // Test setting empty context
    Collection<String> emptyContext = Collections.emptyList();
    query.setContext(emptyContext);
    assertSame(emptyContext, query.getContext(), "Empty context reference should be the same");
    assertEquals(0, query.getContext().size(), "Empty context collection size should be 0");
  }
}
