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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Abstract base class for Dremio JDBC driver implementations. This class handles the common logic
 * for establishing a JDBC connection and executing SQL queries, including context management.
 */
public abstract class AbstractDremioJDBCDriver implements DremioApi {

  private final Connection connection;
  /** Lock for synchronizing access to currentContext. */
  private final Object currentContextLock = new Object();
  /** Stores the current SQL context (e.g., "schema.table"). */
  private String currentContext = "";

  /**
   * Gets the specific JDBC driver class name.
   *
   * @return The fully qualified name of the JDBC driver class.
   */
  protected abstract String getDriverClass();

  /**
   * Gets the logger instance for the specific driver implementation.
   *
   * @return The logger instance.
   */
  protected abstract Logger getLogger();

  /**
   * Constructs an AbstractDremioJDBCDriver and establishes a connection.
   *
   * @param url The JDBC connection URL.
   * @throws RuntimeException if the JDBC driver class is not found or if a connection cannot be
   *     established.
   */
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
   * Runs a SQL statement over JDBC. This method handles setting the SQL context if it has changed
   * since the last execution.
   *
   * @param sql The SQL string to submit to Dremio.
   * @param table A collection of strings representing the path to the table (e.g., schema, table
   *     name). If null or empty, no context is set.
   * @return A {@link DremioApiResponse} indicating the success or failure of the SQL execution.
   * @throws IOException This exception is part of the {@link DremioApi} interface. In this
   *     JDBC-based implementation, actual {@link SQLException}s are typically wrapped in a {@link
   *     RuntimeException}.
   * @throws RuntimeException if a {@link SQLException} occurs during JDBC operations, or if an
   *     unexpected state is encountered.
   */
  @Override
  public DremioApiResponse runSQL(String sql, Collection<String> table) throws IOException {
    final String context;
    if (table == null || table.isEmpty()) {
      context = "";
    } else {
      context = String.join(".", table);
    }
    // Synchronize to ensure thread-safe context switching
    synchronized (currentContextLock) {
      if (!currentContext.equals(context)) {
        currentContext = context;
        getLogger().info(() -> String.format("Changing SQL context to: %s", context));
        try {
          // Execute USE statement to change context if it's not empty
          if (!context.isEmpty()) {
            if (!runSqlQueryAndConsumeResult("USE " + context)) {
              // The execute method returns false if the result is an update count or there are no
              // results.
              // For a successful USE statement, it might return true or false depending on the
              // driver.
              // We'll rely on absence of SQLException to indicate success for USE.
              // However, if it explicitly returns false and it's unexpected, it could be an issue.
              // For simplicity, we assume if no SQLException, USE was successful.
              getLogger().fine(() -> String.format("USE %s statement executed.", context));
            }
          }
          // After attempting to set context (or if no context change was needed), execute the main
          // SQL
          boolean success = runSqlQueryAndConsumeResult(sql);
          // The 'execute' method returns true if the first result is a ResultSet object;
          // false if it is an update count or there are no results.
          // For many DML/DDL, it might be false. For SELECT, it's true.
          // We consider it successful if no exception is thrown.
          // A more robust check might involve checking update counts or specific result types.
          if (!success && sql.toLowerCase().trim().startsWith("select")) {
            // This case might be an issue if a SELECT query returns false without an exception.
            // However, typically, a SELECT that returns no rows would still return true from
            // execute().
            getLogger()
                .warning(
                    () ->
                        String.format(
                            "SQL statement '%s' executed but returned false, which might be unexpected for a SELECT.",
                            sql));
          }
          final DremioApiResponse response = new DremioApiResponse();
          response.setSuccessful(true);
          return response;
        } catch (SQLException ex) {
          getLogger()
              .warning(
                  () ->
                      String.format(
                          "Failed to set context or execute SQL '%s': %s", sql, ex.getMessage()));
          final DremioApiResponse response = new DremioApiResponse();
          response.setSuccessful(false);
          response.setErrorMessage("Failed to set context or execute SQL: " + ex.getMessage());
          return response;
        }
      }
    }
    // If context did not need to be changed, execute SQL directly
    try {
      runSqlQueryAndConsumeResult(sql);
      // Ensure any update count is consumed
    } catch (SQLException e) {
      getLogger()
          .warning(() -> String.format("Failed to execute SQL '%s': %s", sql, e.getMessage()));
      final DremioApiResponse response = new DremioApiResponse();
      response.setSuccessful(false);
      response.setErrorMessage("Failed to execute SQL: " + e.getMessage());
      return response;
    }

    final DremioApiResponse response = new DremioApiResponse();
    response.setSuccessful(true);
    return response;
  }

  private boolean runSqlQueryAndConsumeResult(String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      // See notes above about the return value of 'execute()'
      final boolean success = statement.execute(sql);
      if (!success && sql.toLowerCase().trim().startsWith("select")) {
        getLogger()
            .warning(
                () ->
                    String.format(
                        "SQL statement '%s' executed but returned false, which might be unexpected for a SELECT.",
                        sql));
      }
      // Consume the result to prevent cancellation
      if (success) {
        try (ResultSet rs = statement.getResultSet()) {
          while (rs.next()) {
            // Consume all rows
          }
        }
      }

      while (statement.getMoreResults() || statement.getUpdateCount() != -1) {
        // Continue consuming results
      }
      // Ensure any update count is consumed
      return success;
    }
  }

  /**
   * Gets the JDBC connection URL for the Dremio server. Note: This specific implementation
   * currently returns an empty string. A concrete implementation should return the actual
   * connection URL if it's meaningful.
   *
   * @return The JDBC connection URL used by this driver instance.
   */
  @Override
  public String getUrl() {
    return "";
  }
}
