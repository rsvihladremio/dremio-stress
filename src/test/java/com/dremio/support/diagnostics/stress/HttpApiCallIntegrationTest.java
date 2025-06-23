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

import static org.junit.Assert.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for HttpApiCall using a real HTTP server. These tests verify the complete HTTP
 * request/response cycle including JSON parsing and error handling.
 */
public class HttpApiCallIntegrationTest {

  private Server server;
  private int serverPort;
  private String baseUrl;

  @Before
  public void setUp() throws Exception {
    // Create Jetty server
    server = new Server();

    // Create connector on random port
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(0); // Use random available port
    server.addConnector(connector);

    // Create servlet context
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);

    // Add test servlets
    context.addServlet(new ServletHolder(new TestGetServlet()), "/api/test-get");
    context.addServlet(new ServletHolder(new UsersServlet()), "/api/users");
    context.addServlet(new ServletHolder(new HeadersTestServlet()), "/api/headers-test");
    context.addServlet(new ServletHolder(new TestPostServlet()), "/api/test-post");
    context.addServlet(new ServletHolder(new EchoServlet()), "/api/echo");
    context.addServlet(new ServletHolder(new NotFoundServlet()), "/api/not-found");
    context.addServlet(new ServletHolder(new ServerErrorServlet()), "/api/server-error");
    context.addServlet(new ServletHolder(new BadRequestServlet()), "/api/bad-request");
    context.addServlet(new ServletHolder(new UnauthorizedServlet()), "/api/unauthorized");

    // Start the server
    server.start();
    serverPort = connector.getLocalPort();
    baseUrl = "http://localhost:" + serverPort;
    System.out.println("Test HTTP server started on port: " + serverPort);
  }

  @After
  public void tearDown() throws Exception {
    if (server != null) {
      server.stop();
      System.out.println("Test HTTP server stopped");
    }
  }

  // Servlet classes for test endpoints

  private static class TestGetServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.getWriter()
          .write("{\"status\":\"success\",\"data\":\"test data\",\"timestamp\":1234567890}");
    }
  }

  private static class UsersServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.getWriter()
          .write(
              "{\"users\":[{\"id\":1,\"name\":\"John\"},{\"id\":2,\"name\":\"Jane\"}],\"total\":2}");
    }
  }

  private static class HeadersTestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      String userAgent = req.getHeader("User-Agent");
      String authorization = req.getHeader("Authorization");
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_OK);
      String response =
          String.format(
              "{\"received_headers\":{\"user_agent\":\"%s\",\"authorization\":\"%s\"}}",
              userAgent != null ? userAgent : "null",
              authorization != null ? authorization : "null");
      resp.getWriter().write(response);
    }
  }

  private static class TestPostServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      String requestBody = readRequestBody(req);
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_CREATED);
      String response =
          String.format(
              "{\"status\":\"created\",\"received_data\":%s,\"id\":12345}",
              requestBody.isEmpty() ? "null" : requestBody);
      resp.getWriter().write(response);
    }
  }

  private static class EchoServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      String requestBody = readRequestBody(req);
      String contentType = req.getHeader("Content-Type");
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_OK);
      String response =
          String.format(
              "{\"echo\":{\"body\":%s,\"content_type\":\"%s\"}}",
              requestBody.isEmpty() ? "null" : requestBody,
              contentType != null ? contentType : "null");
      resp.getWriter().write(response);
    }
  }

  private static class NotFoundServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
      resp.getWriter().write("{\"error\":\"Resource not found\",\"code\":\"NOT_FOUND\"}");
    }
  }

  private static class ServerErrorServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      resp.getWriter().write("{\"error\":\"Internal server error\",\"code\":\"INTERNAL_ERROR\"}");
    }
  }

  private static class BadRequestServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      resp.getWriter().write("{\"error\":\"Bad request\",\"code\":\"BAD_REQUEST\"}");
    }
  }

  private static class UnauthorizedServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
      resp.setContentType("application/json");
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      resp.getWriter().write("{\"error\":\"Unauthorized\",\"code\":\"UNAUTHORIZED\"}");
    }
  }

  private static String readRequestBody(HttpServletRequest req) throws IOException {
    StringBuilder body = new StringBuilder();
    try (BufferedReader reader = req.getReader()) {
      String line;
      while ((line = reader.readLine()) != null) {
        body.append(line);
      }
    }
    return body.toString();
  }

  @Test
  public void testSuccessfulGetRequest() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/test-get");
    Map<String, String> headers = new HashMap<>();
    headers.put("Accept", "application/json");

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 200", 200, response.getResponseCode());
    assertEquals("Response message should be OK", "OK", response.getMessage());
    assertNotNull("Response body should not be null", response.getResponse());
    assertEquals("Status should be success", "success", response.getResponse().get("status"));
    assertEquals("Data should match", "test data", response.getResponse().get("data"));
    assertEquals("Timestamp should match", 1234567890, response.getResponse().get("timestamp"));
  }

  @Test
  public void testGetRequestWithComplexResponse() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/users");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 200", 200, response.getResponseCode());
    assertNotNull("Response body should not be null", response.getResponse());
    assertEquals("Total should be 2", 2, response.getResponse().get("total"));
    assertNotNull("Users array should exist", response.getResponse().get("users"));
  }

  @Test
  public void testGetRequestWithHeaders() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/headers-test");
    Map<String, String> headers = new HashMap<>();
    headers.put("User-Agent", "HttpApiCall-Test/1.0");
    headers.put("Authorization", "Bearer test-token-123");

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 200", 200, response.getResponseCode());

    @SuppressWarnings("unchecked")
    Map<String, Object> receivedHeaders =
        (Map<String, Object>) response.getResponse().get("received_headers");
    assertNotNull("Received headers should not be null", receivedHeaders);
    assertEquals(
        "User-Agent should match", "HttpApiCall-Test/1.0", receivedHeaders.get("user_agent"));
    assertEquals(
        "Authorization should match",
        "Bearer test-token-123",
        receivedHeaders.get("authorization"));
  }

  @Test
  public void testSuccessfulPostRequest() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/test-post");
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    String requestBody = "{\"name\":\"test\",\"value\":42}";

    HttpApiResponse response = apiCall.submitPost(url, headers, requestBody);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 201", 201, response.getResponseCode());
    assertEquals("Response message should be Created", "Created", response.getMessage());
    assertNotNull("Response body should not be null", response.getResponse());
    assertEquals("Status should be created", "created", response.getResponse().get("status"));
    assertEquals("ID should be set", 12345, response.getResponse().get("id"));
  }

  @Test
  public void testPostRequestWithNullBody() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/test-post");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitPost(url, headers, null);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 201", 201, response.getResponseCode());
    assertNotNull("Response body should not be null", response.getResponse());
    assertEquals("Status should be created", "created", response.getResponse().get("status"));
  }

  @Test
  public void testPostRequestWithEmptyBody() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/test-post");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitPost(url, headers, "");

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 201", 201, response.getResponseCode());
    assertNotNull("Response body should not be null", response.getResponse());
  }

  @Test
  public void testPostEchoEndpoint() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/echo");
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    String requestBody = "{\"message\":\"hello world\"}";

    HttpApiResponse response = apiCall.submitPost(url, headers, requestBody);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 200", 200, response.getResponseCode());

    @SuppressWarnings("unchecked")
    Map<String, Object> echo = (Map<String, Object>) response.getResponse().get("echo");
    assertNotNull("Echo should not be null", echo);
    assertEquals("Content type should match", "application/json", echo.get("content_type"));
  }

  // Error handling tests

  @Test
  public void testGetNotFound() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/not-found");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 404", 404, response.getResponseCode());
    assertTrue(
        "Response message should contain Not Found", response.getMessage().contains("Not Found"));
    assertTrue(
        "Response message should contain error details",
        response.getMessage().contains("Resource not found"));
  }

  @Test
  public void testGetServerError() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/server-error");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 500", 500, response.getResponseCode());

    // The response message should contain the HTTP status message and error details
    assertNotNull("Response message should not be null", response.getMessage());
    assertTrue(
        "Response message should contain Server Error",
        response.getMessage().contains("Server Error"));
    assertTrue(
        "Response message should contain error details",
        response.getMessage().contains("Internal server error"));
  }

  @Test
  public void testGetBadRequest() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/bad-request");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 400", 400, response.getResponseCode());
    assertTrue(
        "Response message should contain Bad Request",
        response.getMessage().contains("Bad Request"));
    assertTrue(
        "Response message should contain error details",
        response.getMessage().contains("Bad request"));
  }

  @Test
  public void testGetUnauthorized() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/unauthorized");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 401", 401, response.getResponseCode());
    assertTrue(
        "Response message should contain Unauthorized",
        response.getMessage().contains("Unauthorized"));
    assertTrue(
        "Response message should contain error details",
        response.getMessage().contains("Unauthorized"));
  }

  @Test
  public void testGetWithEmptyHeaders() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/test-get");
    Map<String, String> headers = new HashMap<>();

    HttpApiResponse response = apiCall.submitGet(url, headers);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 200", 200, response.getResponseCode());
    assertNotNull("Response body should not be null", response.getResponse());
    assertEquals("Status should be success", "success", response.getResponse().get("status"));
  }

  @Test
  public void testPostWithMultipleHeaders() throws Exception {
    HttpApiCall apiCall = new HttpApiCall(false);
    URL url = new URL(baseUrl + "/api/echo");
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("Accept", "application/json");
    headers.put("User-Agent", "HttpApiCall-Integration-Test/1.0");
    headers.put("X-Custom-Header", "custom-value");
    String requestBody = "{\"test\":\"integration\"}";

    HttpApiResponse response = apiCall.submitPost(url, headers, requestBody);

    assertNotNull("Response should not be null", response);
    assertEquals("Response code should be 200", 200, response.getResponseCode());
    assertNotNull("Response body should not be null", response.getResponse());

    @SuppressWarnings("unchecked")
    Map<String, Object> echo = (Map<String, Object>) response.getResponse().get("echo");
    assertNotNull("Echo should not be null", echo);
    assertEquals("Content type should match", "application/json", echo.get("content_type"));
  }
}
