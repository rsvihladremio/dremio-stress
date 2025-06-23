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

/**
 * Defines the communication protocol to be used when interacting with Dremio. This enum specifies
 * whether to use the HTTP API, the standard JDBC driver, or the legacy JDBC driver.
 */
public enum Protocol {
  /** Represents communication via the Dremio HTTP API. */
  HTTP,
  /** Represents communication via the standard Dremio JDBC driver (Arrow Flight). */
  JDBC,
  /** Represents communication via the legacy Dremio JDBC driver. */
  LegacyJDBC;

  /**
   * Returns the string representation of the protocol.
   *
   * @return the protocol name as a string.
   */
  @Override
  public String toString() {
    // Using ordinal() is generally discouraged, but keeping existing logic
    final String protocolString;
    if (this.ordinal() == 0) {
      protocolString = "HTTP";
    } else if (this.ordinal() == 1) {
      protocolString = "JDBC";
    } else if (this.ordinal() == 2) {
      protocolString = "LegacyJDBC";
    } else {
      protocolString = null;
    }
    return protocolString;
  }
}
