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

/**
 * Represents a generic HTTP API response containing the status code, an optional message, and the
 * parsed response body as a map.
 */
public class HttpApiResponse {
  /** The HTTP response status code (e.g., 200, 404, 500). */
  private int responseCode;
  /** An optional message associated with the response, often from the API response body. */
  private String message;
  /** The parsed response body, typically as a Map if the response is JSON. */
  private Map<String, Object> response;

  /**
   * Gets the HTTP response status code.
   *
   * @return the response code.
   */
  public int getResponseCode() {
    return responseCode;
  }

  /**
   * Sets the HTTP response status code.
   *
   * @param responseCode the response code to set.
   */
  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  /**
   * Gets the message associated with the response.
   *
   * @return the message string.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message associated with the response.
   *
   * @param message the message string to set.
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * Gets the parsed response body as a Map.
   *
   * @return the response body map.
   */
  public Map<String, Object> getResponse() {
    return response;
  }

  /**
   * Sets the parsed response body as a Map.
   *
   * @param response the response body map to set.
   */
  public void setResponse(Map<String, Object> response) {
    this.response = response;
  }

  /**
   * Provides a string representation of the HttpApiResponse object.
   *
   * @return a string representation including response code, message, and response map.
   */
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
