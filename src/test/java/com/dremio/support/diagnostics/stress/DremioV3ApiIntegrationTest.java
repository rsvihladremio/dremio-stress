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

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for DremioV3Api using Testcontainers with a real Dremio OSS instance. These
 * tests verify the complete API workflow including authentication, SQL execution, and job status
 * monitoring against a real Dremio server.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DremioV3ApiIntegrationTest {

  private static final String DREMIO_IMAGE = "dremio/dremio-oss:25.2.0";
  private static final String DEFAULT_USERNAME = "dremio";
  private static final String DEFAULT_PASSWORD = "dremio123";
  private static final int DREMIO_PORT = 9047;
  private static final int TIMEOUT_SECONDS = 30;

  @Container public static final GenericContainer<?> dremioContainer = createDremioContainer();

  @SuppressWarnings("resource") // Container lifecycle is managed by Testcontainers framework
  private static GenericContainer<?> createDremioContainer() {
    return new GenericContainer<>(DREMIO_IMAGE)
        .withExposedPorts(DREMIO_PORT)
        .withClasspathResourceMapping(
            "dremio.conf", "/opt/dremio/conf/dremio.conf", BindMode.READ_ONLY)
        .waitingFor(
            Wait.forHttp("/")
                .forPort(DREMIO_PORT)
                .withStartupTimeout(Duration.ofMinutes(5)))
        .withStartupTimeout(Duration.ofMinutes(5));
  }

  private static String dremioBaseUrl;
  private static DremioV3Api dremioApi;

  @BeforeAll
  public static void setUpClass() throws Exception {
    // Get the mapped port and construct base URL
    Integer mappedPort = dremioContainer.getMappedPort(DREMIO_PORT);
    dremioBaseUrl = String.format("http://localhost:%d", mappedPort);

    System.out.println("Dremio container started on: " + dremioBaseUrl);

    // Wait a bit more for Dremio to be fully ready
    Thread.sleep(10000);

    // Create the API instance
    HttpApiCall apiCall = new HttpApiCall(false);
    UsernamePasswordAuth auth = new UsernamePasswordAuth(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    try {
      dremioApi = new DremioV3Api(apiCall, auth, dremioBaseUrl, TIMEOUT_SECONDS);
      System.out.println("Successfully authenticated with Dremio API");
    } catch (Exception e) {
      System.err.println("Failed to authenticate with Dremio: " + e.getMessage());
      throw e;
    }
  }

  @AfterAll
  public static void tearDownClass() {
    if (dremioContainer != null) {
      dremioContainer.stop();
    }
  }

  @Test
  public void testGetUrl() {
    String url = dremioApi.getUrl();
    assertNotNull(url, "URL should not be null");
    assertEquals(dremioBaseUrl, url, "URL should match the base URL");
  }

  @Test
  public void testRunSimpleSelectQuery() throws IOException {
    String sql = "SELECT 1 as test_column";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }

  @Test
  public void testRunSelectWithMultipleColumns() throws IOException {
    String sql = "SELECT 1 as col1, 'test' as col2, true as col3";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }

  @Test
  public void testRunQueryWithContext() throws IOException {
    String sql = "SELECT 1 as test_column";

    DremioApiResponse response = dremioApi.runSQL(sql, Arrays.asList("@dremio"));

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }

  @Test
  public void testRunInvalidSqlQuery() throws IOException {
    String sql = "SELECT * FROM non_existent_table";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccessful(), "Query should fail");
    assertNotNull(response.getErrorMessage(), "Error message should not be null for failed query");
    assertTrue(
        response.getErrorMessage().contains("FAILED")
            || response.getErrorMessage().contains("Object")
            || response.getErrorMessage().contains("not found"),
        "Error message should contain failure info");
  }

  @Test
  public void testRunQueryWithSyntaxError() throws IOException {
    String sql = "INVALID SQL SYNTAX HERE";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccessful(), "Query should fail");
    assertNotNull(response.getErrorMessage(), "Error message should not be null for failed query");
  }

  @Test
  public void testRunEmptyQuery() throws IOException {
    String sql = "";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccessful(), "Empty query should fail");
    assertNotNull(response.getErrorMessage(), "Error message should not be null for empty query");
    assertTrue(
        response.getErrorMessage().contains("sql cannot be empty")
            || response.getErrorMessage().contains("InvalidParameterException"),
        "Error message should mention invalid parameter");
  }

  @Test
  public void testRunNullQuery() throws IOException {
    DremioApiResponse response = dremioApi.runSQL(null, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccessful(), "Null query should fail");
    assertNotNull(response.getErrorMessage(), "Error message should not be null for null query");
    assertTrue(
        response.getErrorMessage().contains("sql cannot be empty")
            || response.getErrorMessage().contains("InvalidParameterException"),
        "Error message should mention invalid parameter");
  }

  @Test
  public void testRunQueryWithNullContext() throws IOException {
    String sql = "SELECT 1 as test_column";

    DremioApiResponse response = dremioApi.runSQL(sql, null);

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query should be successful with null context");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }



  @Test
  public void testRunQueryWithLongExecution() throws IOException {
    // This query should take some time but complete within our timeout
    String sql = "SELECT COUNT(*) FROM (SELECT * FROM (VALUES (1), (2), (3), (4), (5)) t(x))";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Long-running query should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }

  @Test
  public void testMultipleSequentialQueries() throws IOException {
    // Test running multiple queries in sequence to ensure the API can handle multiple calls
    for (int i = 1; i <= 3; i++) {
      String sql = "SELECT " + i + " as query_number";

      DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

      assertNotNull(response, "Response " + i + " should not be null");
      assertTrue(response.isSuccessful(), "Query " + i + " should be successful");
      assertNull(
          response.getErrorMessage(), "Error message should be null for successful query " + i);
    }
  }

  @Test
  public void testQueryWithSpecialCharacters() throws IOException {
    String sql = "SELECT 'Hello, World! @#$%^&*()' as special_chars";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query with special characters should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }



  @Test
  public void testAuthenticationFlow() throws IOException {
    // Test that we can create a new API instance (which tests authentication)
    HttpApiCall newApiCall = new HttpApiCall(false);
    UsernamePasswordAuth auth = new UsernamePasswordAuth(DEFAULT_USERNAME, DEFAULT_PASSWORD);

    DremioV3Api newApi = new DremioV3Api(newApiCall, auth, dremioBaseUrl, TIMEOUT_SECONDS);

    assertNotNull(newApi, "New API instance should be created");
    assertEquals(dremioBaseUrl, newApi.getUrl(), "URL should match");

    // Test that the new instance can execute queries
    String sql = "SELECT 'auth_test' as test";
    DremioApiResponse response = newApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query should be successful");
  }

  @Test
  public void testInvalidAuthentication() {
    // Test authentication with invalid credentials
    HttpApiCall apiCall = new HttpApiCall(false);
    UsernamePasswordAuth invalidAuth = new UsernamePasswordAuth("invalid", "credentials");

    try {
      new DremioV3Api(apiCall, invalidAuth, dremioBaseUrl, TIMEOUT_SECONDS);
      fail("Should have thrown an exception for invalid credentials");
    } catch (RuntimeException e) {
      assertTrue(
          e.getMessage().contains("token") || e.getMessage().contains("auth"),
          "Exception should mention token or authentication");
    } catch (IOException e) {
      // This is also acceptable as authentication might fail with IOException
      assertNotNull(e.getMessage(), "Exception should have a message");
    }
  }

  @Test
  public void testJobStatusMonitoring() throws IOException {
    // Test a query that should complete successfully and verify job monitoring works
    String sql = "SELECT COUNT(*) FROM (VALUES (1), (2), (3), (4), (5)) t(x)";

    long startTime = System.currentTimeMillis();
    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());
    long endTime = System.currentTimeMillis();

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query should be successful");

    // Verify that the query took some time (indicating job monitoring occurred)
    long executionTime = endTime - startTime;
    assertTrue(executionTime > 100, "Query should take some time for job monitoring");
    assertTrue(executionTime < 30000, "Query should complete within reasonable time");
  }

  @Test
  public void testQueryWithDifferentDataTypes() throws IOException {
    String sql =
        "SELECT "
            + "  CAST(123 AS INTEGER) as int_col, "
            + "  CAST(123.45 AS DOUBLE) as double_col, "
            + "  CAST('2023-01-01' AS DATE) as date_col, "
            + "  CAST(true AS BOOLEAN) as bool_col, "
            + "  CAST('text data' AS VARCHAR) as varchar_col";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query with different data types should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }

  @Test
  public void testQueryWithJoins() throws IOException {
    String sql =
        "SELECT t1.x, t2.y FROM "
            + "(VALUES (1), (2), (3)) t1(x) "
            + "JOIN (VALUES (1), (2), (3)) t2(y) ON t1.x = t2.y";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query with joins should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }

  @Test
  public void testQueryWithAggregation() throws IOException {
    String sql =
        "SELECT "
            + "  COUNT(*) as total_count, "
            + "  SUM(x) as sum_x, "
            + "  AVG(x) as avg_x, "
            + "  MIN(x) as min_x, "
            + "  MAX(x) as max_x "
            + "FROM (VALUES (1), (2), (3), (4), (5)) t(x)";

    DremioApiResponse response = dremioApi.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query with aggregation should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }
}
