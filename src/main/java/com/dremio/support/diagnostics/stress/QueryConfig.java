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

public class QueryConfig {

  private String query;
  private String queryGroup;
  private int frequency;
  private Map<String, Object[]> parameters;
  private List<String> sqlContext;

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getQueryGroup() {
    return queryGroup;
  }

  public void setQueryGroup(String queryGroup) {
    this.queryGroup = queryGroup;
  }

  public int getFrequency() {
    return frequency;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

  public Map<String, Object[]> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object[]> parameters) {
    this.parameters = parameters;
  }

  public List<String> getSqlContext() {
    return sqlContext;
  }

  public void setSqlContext(List<String> sqlContext) {
    this.sqlContext = sqlContext;
  }
}
