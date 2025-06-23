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
import java.util.Map;

/** Represents the configuration for a single query in the stress test. */
public class QueryConfig {

  /** The SQL query string to be executed. */
  private String query;

  /** The name of the query group this query belongs to. */
  private String queryGroup;

  /** The frequency at which this query should be executed relative to others. */
  private int frequency;

  /**
   * Parameters for the query, where the key is the parameter name and the value is a list of
   * possible values.
   */
  private Map<String, List<Object>> parameters;

  /** The sequence configuration for generating parameters, if applicable. */
  private Sequence sequence;

  /** The SQL context (e.g., path to home space) for the query. */
  private List<String> sqlContext;

  /**
   * Gets the SQL query string.
   *
   * @return the query string.
   */
  public String getQuery() {
    return query;
  }

  /**
   * Sets the SQL query string.
   *
   * @param query the query string.
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * Gets the name of the query group.
   *
   * @return the query group name.
   */
  public String getQueryGroup() {
    return queryGroup;
  }

  /**
   * Sets the name of the query group.
   *
   * @param queryGroup the query group name.
   */
  public void setQueryGroup(String queryGroup) {
    this.queryGroup = queryGroup;
  }

  /**
   * Gets the execution frequency.
   *
   * @return the frequency.
   */
  public int getFrequency() {
    return frequency;
  }

  /**
   * Sets the execution frequency.
   *
   * @param frequency the frequency.
   */
  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

  /**
   * Gets the query parameters.
   *
   * @return a map of parameter names to lists of values.
   */
  public Map<String, List<Object>> getParameters() {
    return parameters;
  }

  /**
   * Sets the query parameters.
   *
   * @param parameters a map of parameter names to lists of values.
   */
  public void setParameters(Map<String, List<Object>> parameters) {
    this.parameters = parameters;
  }

  /**
   * Gets the sequence configuration.
   *
   * @return the sequence configuration.
   */
  public Sequence getSequence() {
    return sequence;
  }

  /**
   * Sets the sequence configuration.
   *
   * @param sequence the sequence configuration.
   */
  public void setSequence(Sequence sequence) {
    this.sequence = sequence;
  }

  /**
   * Gets the SQL context.
   *
   * @return a list of context paths.
   */
  public List<String> getSqlContext() {
    return sqlContext;
  }

  /**
   * Sets the SQL context.
   *
   * @param sqlContext a list of context paths.
   */
  public void setSqlContext(List<String> sqlContext) {
    this.sqlContext = sqlContext;
  }
}
