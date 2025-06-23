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

import java.io.IOException;
import java.util.Collection;

/**
 * Interface defining the operations available on a Dremio API. This interface abstracts the details
 * of communication with Dremio, allowing for different implementations (REST API, JDBC, etc.) while
 * providing a consistent interface for client code.
 */
public interface DremioApi {

  /**
   * Runs a SQL statement against the Dremio API.
   *
   * @param sql SQL string to submit to Dremio
   * @param table Collection of strings representing the context (schema/table path) to use with the
   *     query
   * @return The result of the job execution encapsulated in a DremioApiResponse
   * @throws IOException Occurs when the underlying API call fails, typically a problem with
   *     handling of the request or response body
   */
  DremioApiResponse runSQL(String sql, Collection<String> table) throws IOException;

  /**
   * Returns the URL used to access the Dremio server.
   *
   * @return The URL string used to access Dremio
   */
  String getUrl();
}
