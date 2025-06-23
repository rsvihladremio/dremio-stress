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

/**
 * Implementation of the ConnectApi interface for establishing connections to Dremio APIs. This
 * class provides concrete implementations for connecting to Dremio using different protocols.
 */
public class ConnectDremioApi implements ConnectApi {

  /**
   * Establishes a connection to a Dremio API using the specified parameters.
   *
   * @param username The username for authentication
   * @param password The password for authentication
   * @param host The host address of the Dremio server
   * @param timeoutSeconds Connection timeout in seconds
   * @param protocol The protocol to use for the connection (HTTP, JDBC, or LegacyJDBC)
   * @param ignoreSSL Whether to ignore SSL certificate validation
   * @return A DremioApi instance for interacting with the Dremio server
   * @throws IOException If a connection cannot be established
   */
  @Override
  public DremioApi connect(
      String username,
      String password,
      String host,
      Integer timeoutSeconds,
      Protocol protocol,
      boolean ignoreSSL)
      throws IOException {
    final UsernamePasswordAuth auth = new UsernamePasswordAuth(username, password);
    if (protocol.equals(Protocol.HTTP)) {
      HttpApiCall apiCall = new HttpApiCall(ignoreSSL);
      return new DremioV3Api(apiCall, auth, host, timeoutSeconds);
    } else if (protocol.equals(Protocol.LegacyJDBC)) {
      return new DremioLegacyJDBCDriver(host);
    }
    return new DremioArrowFlightJDBCDriver(host);
  }
}
