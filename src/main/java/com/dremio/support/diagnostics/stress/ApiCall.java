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
import java.net.URL;
import java.util.Map;

/**
 * Interface for making HTTP API calls to Dremio. This abstraction allows for different
 * implementations of API calling mechanisms, enabling flexibility for testing and future
 * extensions.
 */
public interface ApiCall {
  /**
   * Submits a POST request to the specified URL with headers and a body.
   *
   * @param url The URL to submit the request to
   * @param headers Map of HTTP headers to include with the request
   * @param body The request body to send
   * @return The HTTP response encapsulated in an HttpApiResponse object
   * @throws IOException If an I/O error occurs during the request
   */
  HttpApiResponse submitPost(URL url, Map<String, String> headers, String body) throws IOException;

  /**
   * Submits a GET request to the specified URL with headers.
   *
   * @param url The URL to submit the request to
   * @param headers Map of HTTP headers to include with the request
   * @return The HTTP response encapsulated in an HttpApiResponse object
   * @throws IOException If an I/O error occurs during the request
   */
  HttpApiResponse submitGet(URL url, Map<String, String> headers) throws IOException;
}
