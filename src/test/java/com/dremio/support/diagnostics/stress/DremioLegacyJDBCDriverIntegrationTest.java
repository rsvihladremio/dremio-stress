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
 * Integration tests for DremioLegacyJDBCDriver using Testcontainers with a real Dremio OSS
 * instance. These tests verify the complete JDBC driver functionality including connection
 * establishment, SQL execution, and context switching against a real Dremio server.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DremioLegacyJDBCDriverIntegrationTest {

  private static final String DREMIO_IMAGE = "dremio/dremio-oss:25.2.0";
  private static final String DEFAULT_USERNAME = "dremio";
  private static final String DEFAULT_PASSWORD = "dremio123";
  private static final int DREMIO_PORT = 9047;
  private static final int JDBC_PORT = 31010;
  private static final int ZOOKEEPER_PORT = 2181;

  @Container public static final GenericContainer<?> dremioContainer = createDremioContainer();

  @SuppressWarnings("resource") // Container lifecycle is managed by Testcontainers framework
  private static GenericContainer<?> createDremioContainer() {
    return new GenericContainer<>(DREMIO_IMAGE)
        .withExposedPorts(DREMIO_PORT, JDBC_PORT, ZOOKEEPER_PORT)
        .withClasspathResourceMapping(
            "dremio.conf", "/opt/dremio/conf/dremio.conf", BindMode.READ_ONLY)
        .waitingFor(
            Wait.forListeningPort().forPorts(JDBC_PORT).withStartupTimeout(Duration.ofMinutes(5)))
        .withStartupTimeout(Duration.ofMinutes(5));
  }

  private static String jdbcConnectionString;
  private static DremioLegacyJDBCDriver dremioDriver;

  @BeforeAll
  public static void setUpClass() throws Exception {
    // Get the mapped ports
    Integer mappedHttpPort = dremioContainer.getMappedPort(DREMIO_PORT);
    Integer mappedJdbcPort = dremioContainer.getMappedPort(JDBC_PORT);
    Integer mappedZookeeperPort = dremioContainer.getMappedPort(ZOOKEEPER_PORT);

    // Construct JDBC connection string
    jdbcConnectionString =
        String.format(
            "jdbc:dremio:direct=localhost:%d;user=%s;password=%s",
            mappedJdbcPort, DEFAULT_USERNAME, DEFAULT_PASSWORD);

    System.out.println("Dremio container started on HTTP port: " + mappedHttpPort);
    System.out.println("Dremio JDBC connection string: " + jdbcConnectionString);
    System.out.println("Dremio ZooKeeper port: " + mappedZookeeperPort);

    int durationSeconds = 20;
    System.out.println("Waiting for Dremio to be fully ready...0/" + durationSeconds + " seconds");

    // Wait a bit more for Dremio to be fully ready
    for (int i = 0; i < durationSeconds; i++) {
      Thread.sleep(1000);
      System.out.println(
          "Waiting for Dremio to be fully ready..." + (i + 1) + "/" + durationSeconds + " seconds");
    }

    try {
      dremioDriver = new DremioLegacyJDBCDriver(jdbcConnectionString);
      System.out.println("Successfully created DremioLegacyJDBCDriver instance");
    } catch (Exception e) {
      System.err.println("Failed to create DremioLegacyJDBCDriver: " + e.getMessage());
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
  public void testRunSimpleSelectQuery() throws IOException {
    String sql = "SELECT 1 as test_column";

    DremioApiResponse response = dremioDriver.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }

  @Test
  public void testRunInvalidSqlQuery() throws IOException {
    String sql = "SELECT * FROM non_existent_table";

    DremioApiResponse response = dremioDriver.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccessful(), "Query should fail");
    assertNotNull(response.getErrorMessage(), "Error message should not be null for failed query");
    assertTrue(
        response.getErrorMessage().contains("Failed to execute SQL")
            || response.getErrorMessage().contains("Object")
            || response.getErrorMessage().contains("not found"),
        "Error message should contain failure info");
  }

  @Test
  public void testRunQueryWithSyntaxError() throws IOException {
    String sql = "INVALID SQL SYNTAX HERE";

    DremioApiResponse response = dremioDriver.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertFalse(response.isSuccessful(), "Query should fail");
    assertNotNull(response.getErrorMessage(), "Error message should not be null for failed query");
  }

  @Test
  public void testQueryWithSpecialCharacters() throws IOException {
    String sql = "SELECT 'Hello, World! @#$%^&*()' as special_chars";

    DremioApiResponse response = dremioDriver.runSQL(sql, Collections.emptyList());

    assertNotNull(response, "Response should not be null");
    assertTrue(response.isSuccessful(), "Query with special characters should be successful");
    assertNull(response.getErrorMessage(), "Error message should be null for successful query");
  }
}
