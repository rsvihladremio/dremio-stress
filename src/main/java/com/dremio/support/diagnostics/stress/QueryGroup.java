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

/** Represents a group of queries that can be run together. */
public class QueryGroup {
  /** The name of the query group. */
  private String name;
  /** The list of query definitions that belong to this group. */
  private List<String> queries;

  /**
   * Gets the name of the query group.
   *
   * @return the group name.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the query group.
   *
   * @param name the group name.
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the list of queries in the group.
   *
   * @return the list of queries.
   */
  public List<String> getQueries() {
    return queries;
  }

  /**
   * Sets the list of queries in the group.
   *
   * @param queries the list of queries.
   */
  public void setQueries(List<String> queries) {
    this.queries = queries;
  }
}
