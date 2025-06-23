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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

public class QueryTest {

  @Test
  public void testDefaultState() {
    Query query = new Query();
    assertNull("Query text should be null by default", query.getQueryText());
    assertNull("Context should be null by default", query.getContext());
  }

  @Test
  public void testGettersAndSetters() {
    Query query = new Query();

    // Test query text
    String queryText = "SELECT 1";
    query.setQueryText(queryText);
    assertEquals("Query text should match the set value", queryText, query.getQueryText());
    assertSame("Query text reference should be the same", queryText, query.getQueryText());

    // Test context
    Collection<String> context = Arrays.asList("schema1", "schema2");
    query.setContext(context);
    assertSame("Context collection reference should be the same", context, query.getContext());
    assertEquals("Context collection size should match", context.size(), query.getContext().size());
    assertEquals("Context collection content should match", context, query.getContext());

    // Test setting context to null
    query.setContext(null);
    assertNull("Context should be null after setting to null", query.getContext());

    // Test setting empty context
    Collection<String> emptyContext = Collections.emptyList();
    query.setContext(emptyContext);
    assertSame("Empty context reference should be the same", emptyContext, query.getContext());
    assertEquals("Empty context collection size should be 0", 0, query.getContext().size());
  }
}
