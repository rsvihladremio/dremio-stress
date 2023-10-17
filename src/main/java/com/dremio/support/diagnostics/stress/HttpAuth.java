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

/** HttpAuth wraps the username and password */
public class HttpAuth {
  private final String username;
  private final String password;

  /** generates json string for the rest api authentication */
  @Override
  public String toString() {
    return String.format("{\"userName\":\"%s\",\"password\":\"%s\"}", username, password);
  }

  /**
   * HttpAuth wraps the username and password for the rest api
   *
   * @param username username with rights to dremio rest api
   * @param password password for the username
   */
  public HttpAuth(String username, String password) {
    this.username = username;
    this.password = password;
  }
}
