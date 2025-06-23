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

import java.util.Collection;

/**
 * Represents a single SQL query to be executed by the stress tool. Includes the query text and an
 * optional context for the query.
 */
public class Query {
  private String queryText;
  private Collection<String> context;

  /**
   * Gets the SQL query text.
   *
   * @return the SQL query string.
   */
  public String getQueryText() {
    return queryText;
  }

  /**
   * Sets the SQL query text.
   *
   * @param queryText the SQL query string to set.
   */
  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  /**
   * Gets the optional context for the query.
   *
   * @return a collection of strings representing the query context.
   */
  public Collection<String> getContext() {
    return context;
  }

  /**
   * Sets the optional context for the query.
   *
   * @param context a collection of strings representing the query context to set.
   */
  public void setContext(Collection<String> context) {
    this.context = context;
  }
}
