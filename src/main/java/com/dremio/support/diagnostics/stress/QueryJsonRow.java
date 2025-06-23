/**
 * Copyright 2022 Dremio
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Represents a single row of query information parsed from a JSON file. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryJsonRow {
  /** The text of the SQL query. */
  private String queryText;
  /** The outcome of the query (e.g., "SUCCESS", "FAILED"). */
  private String outcome;
  /** The context in which the query was run. */
  private String context;
  /** The username who executed the query. */
  private String username;
  /** The unique identifier for the query. */
  private String queryId;

  /**
   * Gets the SQL query text.
   *
   * @return the query text.
   */
  public String getQueryText() {
    return queryText;
  }

  /**
   * Sets the SQL query text.
   *
   * @param queryText the query text.
   */
  public void setQueryText(String queryText) {
    this.queryText = queryText;
  }

  /**
   * Gets the outcome of the query.
   *
   * @return the outcome.
   */
  public String getOutcome() {
    return outcome;
  }

  /**
   * Sets the outcome of the query.
   *
   * @param outcome the outcome.
   */
  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  /**
   * Gets the context of the query.
   *
   * @return the context.
   */
  public String getContext() {
    return context;
  }

  /**
   * Sets the context of the query.
   *
   * @param context the context.
   */
  public void setContext(String context) {
    this.context = context;
  }

  /**
   * Gets the username who executed the query.
   *
   * @return the username.
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username who executed the query.
   *
   * @param userName the username.
   */
  public void setUsername(String userName) {
    this.username = userName;
  }

  /**
   * Gets the unique identifier for the query.
   *
   * @return the query ID.
   */
  public String getQueryId() {
    return queryId;
  }

  /**
   * Sets the unique identifier for the query.
   *
   * @param queryId the query ID.
   */
  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }
}
