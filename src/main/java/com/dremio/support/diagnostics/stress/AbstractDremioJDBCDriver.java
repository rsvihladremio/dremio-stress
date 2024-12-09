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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Logger;

public abstract class AbstractDremioJDBCDriver implements DremioApi {
  private final Connection connection;
  private final Object currentContextLock = new Object();
  private String currentContext = "";

  protected abstract String getDriverClass();

  protected abstract Logger getLogger();

  protected AbstractDremioJDBCDriver(String url) {
    try {
      Class.forName(this.getDriverClass());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    try {
      connection = DriverManager.getConnection(url);
      // use con here
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * runs a sql statement over jdbc
   *
   * @param sql sql string to submit to dremio
   * @param table
   * @return the result of the job
   * @throws IOException occurs when the underlying apiCall does, typically a problem with handling
   *     of the body
   */
  @Override
  public DremioApiResponse runSQL(String sql, Collection<String> table) throws IOException {
    final String context;
    if (table == null) {
      context = "";
    } else {
      context = String.join(".", table);
    }
    synchronized (currentContextLock) {
      if (!currentContext.equals(context)) {
        currentContext = context;
        getLogger().info(() -> String.format("changing context %s", context));
        try {
          if (!connection.createStatement().execute("USE " + context)) {
            throw new RuntimeException("failed using USE");
          }
          final boolean success = connection.createStatement().execute(sql);
          if (!success) {
            throw new RuntimeException("unhandled exception executing sql");
          }
          final DremioApiResponse response = new DremioApiResponse();
          response.setSuccessful(true);
          return response;
        } catch (SQLException ex) {
          throw new RuntimeException(ex);
        }
      }
    }
    try {
      if (connection.createStatement().execute(sql)) {
        final DremioApiResponse response = new DremioApiResponse();
        response.setSuccessful(true);
        return response;
      }
      throw new RuntimeException("unhandled exception");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The http URL for the dremio server
   *
   * @return return the url used to access Dremio
   */
  @Override
  public String getUrl() {
    return "";
  }
}
