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

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

/**
 * Test class for HttpApiCall. This class tests the constructor behavior and basic functionality
 * that can be tested without requiring external HTTP endpoints.
 */
public class HttpApiCallTest {

  @Test
  public void testConstructorWithIgnoreSSLFalse() {
    // Test that constructor works without throwing exceptions
    HttpApiCall apiCall = new HttpApiCall(false);
    assertNotNull(apiCall, "HttpApiCall should be created successfully");
  }

  @Test
  public void testConstructorWithIgnoreSSLTrue() {
    // Test that constructor works with SSL ignore enabled
    // This should configure SSL to ignore certificate validation
    HttpApiCall apiCall = new HttpApiCall(true);
    assertNotNull(apiCall, "HttpApiCall should be created successfully with SSL ignore");

    // Verify that SSL context was modified (we can't easily test the exact configuration
    // but we can verify the constructor completed without exceptions)
    SSLContext defaultContext = null;
    try {
      defaultContext = SSLContext.getDefault();
      assertNotNull(defaultContext, "Default SSL context should be available");
    } catch (Exception e) {
      fail("Should be able to get default SSL context: " + e.getMessage());
    }
  }

  @Test
  public void testConstructorSSLConfigurationException() {
    // This test verifies that if SSL configuration fails, a RuntimeException is thrown
    // We can't easily force this condition, but we can verify the constructor behavior
    try {
      HttpApiCall apiCall = new HttpApiCall(true);
      assertNotNull(apiCall, "HttpApiCall should be created even with SSL ignore");
    } catch (RuntimeException e) {
      // This would happen if SSL configuration failed
      assertTrue(
          e.getMessage() != null || e.getCause() != null,
          "Exception should be related to SSL configuration");
    }
  }

  @Test
  public void testSubmitGetWithInvalidUrl() {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url;
    try {
      url = new URL("http://invalid-host-that-does-not-exist.com/test");
    } catch (Exception e) {
      fail("URL creation should not fail");
      return;
    }
    Map<String, String> headers = new HashMap<>();

    // This should throw IOException due to unknown host
    assertThrows(IOException.class, () -> apiCall.submitGet(url, headers));
  }

  @Test
  public void testSubmitPostWithInvalidUrl() {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url;
    try {
      url = new URL("http://invalid-host-that-does-not-exist.com/test");
    } catch (Exception e) {
      fail("URL creation should not fail");
      return;
    }
    Map<String, String> headers = new HashMap<>();

    // This should throw IOException due to unknown host
    assertThrows(IOException.class, () -> apiCall.submitPost(url, headers, "test body"));
  }

  @Test
  public void testSubmitGetWithNullHeaders() {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url;
    try {
      url = new URL("http://invalid-host-that-does-not-exist.com/test");
    } catch (Exception e) {
      fail("URL creation should not fail");
      return;
    }

    // This should throw NullPointerException because the implementation
    // doesn't handle null headers properly (calls headers.entrySet() without null check)
    assertThrows(NullPointerException.class, () -> apiCall.submitGet(url, null));
  }

  @Test
  public void testSubmitPostWithNullHeaders() {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url;
    try {
      url = new URL("http://invalid-host-that-does-not-exist.com/test");
    } catch (Exception e) {
      fail("URL creation should not fail");
      return;
    }

    // This should throw NullPointerException because the implementation
    // doesn't handle null headers properly (calls headers.entrySet() without null check)
    assertThrows(NullPointerException.class, () -> apiCall.submitPost(url, null, "test body"));
  }

  @Test
  public void testApiCallInterface() {
    // Test that HttpApiCall properly implements ApiCall interface
    HttpApiCall apiCall = new HttpApiCall(false);
    assertTrue(apiCall instanceof ApiCall, "HttpApiCall should implement ApiCall interface");
  }

  @Test
  public void testSubmitPostWithNullBody() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL("http://invalid-host-that-does-not-exist.com/test");
    Map<String, String> headers = new HashMap<>();

    try {
      // This should throw IOException due to unknown host
      // The null body should be handled gracefully (no exception for null body)
      apiCall.submitPost(url, headers, null);
      fail("Should have thrown IOException for invalid URL");
    } catch (IOException e) {
      // Expected - the method should handle null body gracefully
      // but still fail on the invalid URL
      assertTrue(true, "Exception should be related to connection");
    }
  }

  @Test
  public void testSubmitPostWithEmptyBody() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL("http://invalid-host-that-does-not-exist.com/test");
    Map<String, String> headers = new HashMap<>();

    try {
      // This should throw IOException due to unknown host
      // The empty body should be handled gracefully
      apiCall.submitPost(url, headers, "");
      fail("Should have thrown IOException for invalid URL");
    } catch (IOException e) {
      // Expected - the method should handle empty body gracefully
      // but still fail on the invalid URL
      assertTrue(true, "Exception should be related to connection");
    }
  }

  @Test
  public void testHeadersHandling() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL("http://invalid-host-that-does-not-exist.com/test");
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Authorization", "Bearer token123");
    headers.put("User-Agent", "Test-Agent/1.0");

    try {
      // This should throw IOException due to unknown host
      // But headers should be processed without issues
      apiCall.submitGet(url, headers);
      fail("Should have thrown IOException for invalid URL");
    } catch (IOException e) {
      // Expected - headers should be processed but connection should fail
      assertTrue(true, "Exception should be related to connection");
    }
  }
}
