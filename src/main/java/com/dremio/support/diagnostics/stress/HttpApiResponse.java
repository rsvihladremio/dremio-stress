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

import java.util.Map;

public class HttpApiResponse {
  private int responseCode;
  private String message;
  private Map<String, Object> response;

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Map<String, Object> getResponse() {
    return response;
  }

  public void setResponse(Map<String, Object> response) {
    this.response = response;
  }

  @Override
  public String toString() {
    return "HttpApiResponse{"
        + "responseCode="
        + responseCode
        + ", message='"
        + message
        + '\''
        + ", response="
        + response
        + '}';
  }
}
