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
 * Implementation of the DremioApi interface using the Arrow Flight JDBC driver. This class provides
 * connectivity to Dremio using the high-performance Arrow Flight protocol via JDBC.
 */
public class DremioArrowFlightJDBCDriver extends AbstractDremioJDBCDriver {

  /** Logger for this class. */
  private static final Logger logger =
      Logger.getLogger(DremioArrowFlightJDBCDriver.class.getName());

  /**
   * Returns the fully qualified class name of the Arrow Flight JDBC driver.
   *
   * @return The Arrow Flight JDBC driver class name
   */
  @Override
  protected String getDriverClass() {
    return "org.apache.arrow.driver.jdbc.ArrowFlightJdbcDriver";
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
   * Constructs a DremioArrowFlightJDBCDriver with the specified connection string.
   *
   * @param connectionString The JDBC connection URL for connecting to Dremio
   */
  public DremioArrowFlightJDBCDriver(String connectionString) {
    super(connectionString);
  }
}
