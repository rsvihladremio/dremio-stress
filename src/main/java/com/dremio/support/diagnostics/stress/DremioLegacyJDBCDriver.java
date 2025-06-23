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

import java.util.logging.Logger;

/**
 * Implementation of the DremioApi interface using the legacy Dremio JDBC driver. This class
 * provides connectivity to Dremio via the older JDBC driver.
 */
public class DremioLegacyJDBCDriver extends AbstractDremioJDBCDriver {
  /** Logger for this class. */
  private static final Logger logger = Logger.getLogger(DremioLegacyJDBCDriver.class.getName());

  /**
   * Returns the fully qualified class name of the legacy Dremio JDBC driver.
   *
   * @return The legacy Dremio JDBC driver class name
   */
  @Override
  protected String getDriverClass() {
    return "com.dremio.jdbc.Driver";
  }

  /**
   * Returns the logger for this class.
   *
   * @return The logger instance
   */
  @Override
  protected Logger getLogger() {
    return logger;
  }

  /**
   * Constructs a DremioLegacyJDBCDriver with the specified connection string.
   *
   * @param connectionString The JDBC connection URL for connecting to Dremio
   */
  public DremioLegacyJDBCDriver(final String connectionString) {
    super(connectionString);
  }
}
