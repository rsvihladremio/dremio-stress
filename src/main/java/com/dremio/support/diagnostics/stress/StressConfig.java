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

import java.util.List;

/**
 * Configuration class for the stress test application. This class is used to deserialize the stress
 * test configuration from a JSON file. It contains lists of individual queries and query groups to
 * be executed.
 */
public class StressConfig {

  /**
   * A list of individual query configurations. Each {@link QueryConfig} defines a single query, its
   * parameters, context, and frequency.
   */
  private List<QueryConfig> queries;

  /**
   * A list of query groups. Each {@link QueryGroup} defines a named collection of SQL queries that
   * can be referenced by {@link QueryConfig} instances.
   */
  private List<QueryGroup> queryGroups;

  /**
   * Gets the list of individual query configurations.
   *
   * @return the list of {@link QueryConfig} objects.
   */
  public List<QueryConfig> getQueries() {
    return queries;
  }

  /**
   * Sets the list of individual query configurations.
   *
   * @param queries the list of {@link QueryConfig} objects to set.
   */
  public void setQueries(List<QueryConfig> queries) {
    this.queries = queries;
  }

  /**
   * Gets the list of query groups.
   *
   * @return the list of {@link QueryGroup} objects.
   */
  public List<QueryGroup> getQueryGroups() {
    return queryGroups;
  }

  /**
   * Sets the list of query groups.
   *
   * @param queryGroups the list of {@link QueryGroup} objects to set.
   */
  public void setQueryGroups(List<QueryGroup> queryGroups) {
    this.queryGroups = queryGroups;
  }
}
