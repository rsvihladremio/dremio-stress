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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for StressExec class. */
public class StressExecTest {

  @Mock private ConnectApi mockConnectApi;

  @Mock private DremioApi mockDremioApi;

  @TempDir Path tempDir;

  private StressExec stressExec;
  private Random deterministicRandom;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    deterministicRandom = new Random(42); // Fixed seed for predictable tests
  }

  @Test
  void testMapSqlWithSimpleQuery() {
    // Create a simple query config
    QueryConfig queryConfig = new QueryConfig();
    queryConfig.setQuery("SELECT * FROM table1");
    queryConfig.setSqlContext(Arrays.asList("schema1", "catalog1"));

    stressExec = createStressExec();
    List<Query> result = stressExec.mapSql(queryConfig, new HashMap<>());

    assertEquals(1, result.size());
    Query query = result.get(0);
    assertEquals("SELECT * FROM table1", query.getQueryText());
    assertEquals(Arrays.asList("schema1", "catalog1"), query.getContext());
  }

  @Test
  void testMapSqlWithParameters() {
    QueryConfig queryConfig = new QueryConfig();
    queryConfig.setQuery("SELECT * FROM :table WHERE id = :id");

    Map<String, List<Object>> parameters = new HashMap<>();
    parameters.put("table", Arrays.asList("users", "orders", "products"));
    parameters.put("id", Arrays.asList(1, 2, 3));
    queryConfig.setParameters(parameters);

    stressExec = createStressExec();
    List<Query> result = stressExec.mapSql(queryConfig, new HashMap<>());

    assertEquals(1, result.size());
    Query query = result.get(0);

    // With fixed random seed, we can predict the parameter selection
    // The query should have parameters substituted
    assertFalse(query.getQueryText().contains(":table"));
    assertFalse(query.getQueryText().contains(":id"));
    assertTrue(query.getQueryText().contains("SELECT * FROM"));
    assertTrue(query.getQueryText().contains("WHERE id ="));
  }

  @Test
  void testMapSqlWithSequence() {
    QueryConfig queryConfig = new QueryConfig();
    queryConfig.setQuery("SELECT * FROM table WHERE id = :seq_id");

    Sequence sequence = new Sequence();
    sequence.setName("seq_id");
    sequence.setStart(1);
    sequence.setEnd(3);
    sequence.setStep(1);
    queryConfig.setSequence(sequence);

    stressExec = createStressExec();
    List<Query> result = stressExec.mapSql(queryConfig, new HashMap<>());

    assertEquals(3, result.size());
    assertEquals("SELECT * FROM table WHERE id = 1", result.get(0).getQueryText());
    assertEquals("SELECT * FROM table WHERE id = 2", result.get(1).getQueryText());
    assertEquals("SELECT * FROM table WHERE id = 3", result.get(2).getQueryText());
  }

  @Test
  void testMapSqlWithQueryGroup() {
    QueryConfig queryConfig = new QueryConfig();
    queryConfig.setQueryGroup("test_group");

    QueryGroup queryGroup = new QueryGroup();
    queryGroup.setName("test_group");
    queryGroup.setQueries(Arrays.asList("SELECT 1", "SELECT 2"));

    Map<String, QueryGroup> queryGroups = new HashMap<>();
    queryGroups.put("test_group", queryGroup);

    stressExec = createStressExec();
    List<Query> result = stressExec.mapSql(queryConfig, queryGroups);

    assertEquals(2, result.size());
    assertEquals("SELECT 1", result.get(0).getQueryText());
    assertEquals("SELECT 2", result.get(1).getQueryText());
  }

  @Test
  void testParseQueryConfigsWithFiltering() throws JsonProcessingException {
    stressExec = createStressExec();

    // Create test data that should be filtered out
    String jsonData =
        "{\"username\": \"$dremio$\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT 1\"}\n"
            + "{\"username\": \"user1\", \"outcome\": \"FAILED\", \"queryText\": \"SELECT 2\"}\n"
            + "{\"username\": \"user2\", \"outcome\": \"COMPLETED\", \"queryText\": \"NA\"}\n"
            + "{\"username\": \"user3\", \"outcome\": \"COMPLETED\", \"queryText\": \"CREATE TABLE test (id INT)\"}\n"
            + "{\"username\": \"user4\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT * FROM valid_table\"}\n";

    Scanner scanner = new Scanner(jsonData);
    List<QueryConfig> result = stressExec.parseQueryConfigs(scanner);

    // Only the last query should pass the filtering
    assertEquals(1, result.size());
    assertTrue(result.get(0).getQuery().contains("SELECT * FROM valid_table"));
  }

  @Test
  void testParseQueryConfigsWithLimitResults() throws JsonProcessingException {
    File tempFile = createTempJsonFile();

    stressExec =
        new StressExec(
            deterministicRandom,
            mockConnectApi,
            tempFile,
            QueriesGeneratorFileType.QUERIES_JSON,
            QueriesSequence.RANDOM,
            null,
            100, // limitResults
            Protocol.HTTP,
            "localhost",
            "user",
            "pass",
            10,
            30,
            60,
            false);

    String jsonData =
        "{\"username\": \"user1\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT * FROM table1\", \"context\": \"[]\", \"queryId\": \"123\"}\n"
            + "{\"username\": \"user2\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT * FROM table2 LIMIT 50\", \"context\": \"[]\", \"queryId\": \"456\"}\n";

    Scanner scanner = new Scanner(jsonData);
    List<QueryConfig> result = stressExec.parseQueryConfigs(scanner);

    assertEquals(2, result.size());

    // First query should have LIMIT added
    String firstQuery = result.get(0).getQuery();
    assertTrue(firstQuery.contains("LIMIT 100"));

    // Second query should have LIMIT replaced
    String secondQuery = result.get(1).getQuery();
    assertTrue(secondQuery.contains("LIMIT 100"));
    assertFalse(secondQuery.contains("LIMIT 50"));
  }

  @Test
  void testOpenQueryJsonWithGzipFile() throws IOException {
    // Create a temporary .json.gz file
    Path gzipFile = tempDir.resolve("test.json.gz");
    String jsonContent =
        "{\"username\": \"user1\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT 1\", \"context\": \"[]\", \"queryId\": \"123\"}\n";

    // Write gzipped content
    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
    try (java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos)) {
      gzos.write(jsonContent.getBytes());
    }
    Files.write(gzipFile, baos.toByteArray());

    stressExec = createStressExec();
    List<QueryConfig> result = stressExec.openQueryJson(gzipFile.toFile());

    assertEquals(1, result.size());
    assertTrue(result.get(0).getQuery().contains("SELECT 1"));
  }

  @Test
  void testOpenQueryJsonWithUnsupportedFileType() throws IOException {
    Path unsupportedFile = tempDir.resolve("test.txt");
    Files.write(unsupportedFile, "some content".getBytes());

    stressExec = createStressExec();
    List<QueryConfig> result = stressExec.openQueryJson(unsupportedFile.toFile());

    assertEquals(0, result.size());
  }

  @Test
  void testGetQueriesWithStressJson() throws IOException {
    // Create a stress config JSON file
    Path stressConfigFile = tempDir.resolve("stress.json");
    String stressConfig =
        "{\n"
            + "  \"queries\": [\n"
            + "    {\n"
            + "      \"query\": \"SELECT 1\",\n"
            + "      \"frequency\": 2,\n"
            + "      \"sqlContext\": [\"schema1\"]\n"
            + "    },\n"
            + "    {\n"
            + "      \"query\": \"SELECT 2\",\n"
            + "      \"frequency\": 1,\n"
            + "      \"sqlContext\": [\"schema2\"]\n"
            + "    }\n"
            + "  ]\n"
            + "}\n";
    Files.write(stressConfigFile, stressConfig.getBytes());

    stressExec =
        new StressExec(
            deterministicRandom,
            mockConnectApi,
            stressConfigFile.toFile(),
            QueriesGeneratorFileType.STRESS_JSON,
            QueriesSequence.RANDOM,
            null,
            null,
            Protocol.HTTP,
            "localhost",
            "user",
            "pass",
            10,
            30,
            60,
            false);

    List<QueryConfig> result = stressExec.getQueries();

    // First query has frequency 2, second has frequency 1, so total should be 3
    assertEquals(3, result.size());

    // Count occurrences of each query
    long query1Count = result.stream().filter(q -> q.getQuery().equals("SELECT 1")).count();
    long query2Count = result.stream().filter(q -> q.getQuery().equals("SELECT 2")).count();

    assertEquals(2, query1Count);
    assertEquals(1, query2Count);
  }

  @Test
  void testGetQueriesWithQueriesJsonDirectory() throws IOException {
    // Create a directory with multiple JSON files
    Path queriesDir = tempDir.resolve("queries");
    Files.createDirectory(queriesDir);

    // Create first JSON file
    Path jsonFile1 = queriesDir.resolve("queries1.json");
    String jsonContent1 =
        "{\"username\": \"user1\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT 1\", \"context\": \"[]\", \"queryId\": \"123\"}\n";
    Files.write(jsonFile1, jsonContent1.getBytes());

    // Create second JSON file
    Path jsonFile2 = queriesDir.resolve("queries2.json");
    String jsonContent2 =
        "{\"username\": \"user2\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT 2\", \"context\": \"[]\", \"queryId\": \"456\"}\n";
    Files.write(jsonFile2, jsonContent2.getBytes());

    stressExec =
        new StressExec(
            deterministicRandom,
            mockConnectApi,
            queriesDir.toFile(),
            QueriesGeneratorFileType.QUERIES_JSON,
            QueriesSequence.RANDOM,
            null,
            null,
            Protocol.HTTP,
            "localhost",
            "user",
            "pass",
            10,
            30,
            60,
            false);

    List<QueryConfig> result = stressExec.getQueries();

    assertEquals(2, result.size());
  }

  @Test
  void testGetQueriesWithNonExistentFile() {
    File nonExistentFile = new File(tempDir.toFile(), "nonexistent.json");

    stressExec =
        new StressExec(
            deterministicRandom,
            mockConnectApi,
            nonExistentFile,
            QueriesGeneratorFileType.QUERIES_JSON,
            QueriesSequence.RANDOM,
            null,
            null,
            Protocol.HTTP,
            "localhost",
            "user",
            "pass",
            10,
            30,
            60,
            false);

    assertThrows(RuntimeException.class, () -> stressExec.getQueries());
  }

  @Test
  void testConstructorWithDefaultRandom() {
    StressExec exec =
        new StressExec(
            mockConnectApi,
            createTempJsonFile(),
            QueriesGeneratorFileType.QUERIES_JSON,
            QueriesSequence.RANDOM,
            null,
            null,
            Protocol.HTTP,
            "localhost",
            "user",
            "pass",
            10,
            30,
            60,
            false);

    assertNotNull(exec);
  }

  private StressExec createStressExec() {
    return new StressExec(
        deterministicRandom,
        mockConnectApi,
        createTempJsonFile(),
        QueriesGeneratorFileType.QUERIES_JSON,
        QueriesSequence.RANDOM,
        null,
        null,
        Protocol.HTTP,
        "localhost",
        "user",
        "pass",
        10,
        30,
        60,
        false);
  }

  private File createTempJsonFile() {
    try {
      Path tempFile = tempDir.resolve("test.json");
      String jsonContent =
          "{\"username\": \"user1\", \"outcome\": \"COMPLETED\", \"queryText\": \"SELECT 1\", \"context\": \"[]\", \"queryId\": \"123\"}\n";
      Files.write(tempFile, jsonContent.getBytes());
      return tempFile.toFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
