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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * StressExec is the main execution engine for running stress tests against Dremio.
 *
 * <p>This class orchestrates the execution of multiple SQL queries concurrently to simulate load on
 * a Dremio cluster. It supports various configuration options including: - Sequential or random
 * query execution - Configurable concurrency levels - Query parameter substitution - Real-time
 * progress reporting - Support for both stress configuration files and query JSON files
 *
 * <p>The stress test can be configured to run for a specific duration or until all queries in a
 * sequence have been executed. Progress is reported every 5 seconds showing throughput,
 * success/failure rates, and elapsed time.
 */
public class StressExec {

  private static final Logger logger = Logger.getLogger(StressExec.class.getName());

  // Core configuration fields
  private final Random random; // Random number generator for query selection
  private final File jsonConfig; // Configuration file or directory containing queries
  private final QueriesGeneratorFileType
      fileType; // Type of configuration file (STRESS_JSON or QUERIES_JSON)
  private final QueriesSequence queriesSequence; // Execution order (SEQUENTIAL or RANDOM)
  private final Integer queryIndexForRestart; // Starting index for sequential execution
  private final Integer limitResults; // Optional limit to add to queries

  // Connection configuration
  private final Protocol protocol; // HTTP or HTTPS
  private final String dremioHost; // Dremio coordinator hostname
  private final String dremioUser; // Username for authentication
  private final String dremioPassword; // Password for authentication
  private final Integer timeoutSeconds; // Query timeout in seconds
  private final boolean skipSSLVerification; // Whether to skip SSL certificate verification

  // Execution configuration
  private final long durationTargetMS; // Target duration for stress test in milliseconds
  private final Integer maxQueriesInFlight; // Maximum concurrent queries
  private final ConnectApi connectApi; // API connection interface

  /**
   * Primary constructor that creates a StressExec instance with a SecureRandom generator. This is
   * the main constructor used by external callers.
   *
   * @param connectApi API connection interface for Dremio
   * @param jsonConfig Configuration file or directory containing queries
   * @param fileType Type of configuration file (STRESS_JSON or QUERIES_JSON)
   * @param queriesSequence Execution order (SEQUENTIAL or RANDOM)
   * @param queryIndexForRestart Starting index for sequential execution (null for beginning)
   * @param limitResults Optional limit to add to queries (null for no limit)
   * @param protocol Connection protocol (HTTP, JDBC, LegacyJDBC)
   * @param dremioHost Dremio coordinator hostname
   * @param dremioUser Username for authentication
   * @param dremioPassword Password for authentication
   * @param maxQueriesInFlight Maximum number of concurrent queries
   * @param timeoutSeconds Query timeout in seconds
   * @param durationSeconds Target duration for stress test in seconds
   * @param skipSSLVerification Whether to skip SSL certificate verification
   */
  public StressExec(
      final ConnectApi connectApi,
      final File jsonConfig,
      final QueriesGeneratorFileType fileType,
      final QueriesSequence queriesSequence,
      final Integer queryIndexForRestart,
      final Integer limitResults,
      final Protocol protocol,
      final String dremioHost,
      final String dremioUser,
      final String dremioPassword,
      final Integer maxQueriesInFlight,
      final Integer timeoutSeconds,
      final Integer durationSeconds,
      final boolean skipSSLVerification) {
    this(
        new SecureRandom(),
        connectApi,
        jsonConfig,
        fileType,
        queriesSequence,
        queryIndexForRestart,
        limitResults,
        protocol,
        dremioHost,
        dremioUser,
        dremioPassword,
        maxQueriesInFlight,
        timeoutSeconds,
        durationSeconds,
        skipSSLVerification);
  }

  /**
   * Internal constructor that accepts a custom Random instance. This constructor is primarily used
   * for testing to provide deterministic behavior.
   *
   * @param random Custom random number generator
   * @param connectApi API connection interface for Dremio
   * @param jsonConfig Configuration file or directory containing queries
   * @param fileType Type of configuration file (STRESS_JSON or QUERIES_JSON)
   * @param queriesSequence Execution order (SEQUENTIAL or RANDOM)
   * @param queryIndexForRestart Starting index for sequential execution (null for beginning)
   * @param limitResults Optional limit to add to queries (null for no limit)
   * @param protocol Connection protocol (HTTP, JDBC, LegacyJDBC)
   * @param dremioHost Dremio coordinator hostname
   * @param dremioUser Username for authentication
   * @param dremioPassword Password for authentication
   * @param maxQueriesInFlight Maximum number of concurrent queries
   * @param timeoutSeconds Query timeout in seconds
   * @param durationSeconds Target duration for stress test in seconds
   * @param skipSSLVerification Whether to skip SSL certificate verification
   */
  public StressExec(
      final Random random,
      final ConnectApi connectApi,
      final File jsonConfig,
      final QueriesGeneratorFileType fileType,
      final QueriesSequence queriesSequence,
      final Integer queryIndexForRestart,
      final Integer limitResults,
      final Protocol protocol,
      final String dremioHost,
      final String dremioUser,
      final String dremioPassword,
      final Integer maxQueriesInFlight,
      final Integer timeoutSeconds,
      final Integer durationSeconds,
      final boolean skipSSLVerification) {
    this.random = random;
    this.connectApi = connectApi;
    this.jsonConfig = jsonConfig;
    this.fileType = fileType;
    this.queriesSequence = queriesSequence;
    this.queryIndexForRestart = queryIndexForRestart;
    this.limitResults = limitResults;
    this.protocol = protocol;
    this.dremioHost = dremioHost;
    this.dremioUser = dremioUser;
    this.dremioPassword = dremioPassword;
    this.maxQueriesInFlight = maxQueriesInFlight;
    this.timeoutSeconds = timeoutSeconds;
    this.durationTargetMS = durationSeconds * 1000L; // Convert to milliseconds
    this.skipSSLVerification = skipSSLVerification;
    this.reporter = new StressReporter(queryIndex, durationTargetMS);
    this.stressMonitor =
        new StressMonitor(durationTargetMS, queryIndex, reporter); // Instantiate the monitor
  }

  // Progress reporting
  private final StressReporter reporter;
  private final AtomicInteger queryIndex =
      new AtomicInteger(-1); // Current query index for sequential execution
  private final StressMonitor stressMonitor; // Add the new monitor field

  /**
   * Loads and parses the stress configuration from the JSON file.
   *
   * @return StressConfig object containing the parsed configuration
   * @throws RuntimeException if the file cannot be read or parsed
   */
  private StressConfig getConfig() {
    try (InputStream st = Files.newInputStream(jsonConfig.toPath())) {
      final ObjectMapper objectMapper = new ObjectMapper();
      // TODO: Consider caching this value to avoid repeated file reads
      return objectMapper.readValue(st, StressConfig.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes a single query against Dremio and tracks its execution metrics. This method handles
   * query submission, response validation, timing, and error handling.
   *
   * @param dremioApi The Dremio API client to use for query execution
   * @param mappedSql The query object containing SQL text and context
   */
  private void runQuery(DremioApi dremioApi, Query mappedSql) {
    {
      try {
        Instant startTime = Instant.now();
        DremioApiResponse response = null;
        reporter.incrementSubmittedCounter();

        // Execute the SQL query with its context
        response = dremioApi.runSQL(mappedSql.getQueryText(), mappedSql.getContext());

        // Validate response
        if (response == null) {
          throw new RuntimeException(
              String.format("query %s failed with an empty response", mappedSql));
        }
        if (!response.isSuccessful()) {
          final String errMsg = response.getErrorMessage();
          throw new RuntimeException(
              String.format("query %s failed with error %s", mappedSql, errMsg));
        }

        // Track successful execution timing
        Instant endTime = Instant.now();
        long queryTime = endTime.toEpochMilli() - startTime.toEpochMilli();
        reporter.incrementSuccessfulCounter(queryTime);
        logger.info(() -> String.format("query %s successful", mappedSql));
      } catch (final Exception e) {
        // Track failed execution
        reporter.incrementFailureCounter();
        logger.info(
            () ->
                String.format(
                    "query %s failed %s %s", mappedSql, e, ExceptionUtils.getStackTrace(e)));
      }
    }
  }

  /**
   * Loads and returns the list of queries to execute based on the configured file type. Supports
   * both stress configuration files and query JSON files/directories.
   *
   * @return List of QueryConfig objects representing the queries to execute
   * @throws RuntimeException if no valid queries are found or files cannot be read
   */
  public List<QueryConfig> getQueries() {
    if (this.fileType == QueriesGeneratorFileType.STRESS_JSON) {
      // Load queries from stress configuration file
      final StressConfig config = getConfig();
      return getQueryConfigs(config);
    } else {
      // Load queries from JSON files (single file or directory)
      List<QueryConfig> queriesConfig = new ArrayList<>();
      if (jsonConfig.isDirectory()) {
        logger.info("provided path " + jsonConfig + " is dir. checking for queries.json.");
        File[] queriesDir = jsonConfig.listFiles();
        for (File queriesFile : queriesDir) {
          queriesConfig.addAll(openQueryJson(queriesFile));
        }
      } else if (jsonConfig.exists()) {
        logger.info("provided path is a single queries.json file");
        queriesConfig = openQueryJson(jsonConfig);
      } else {
        throw new RuntimeException("file or folder " + jsonConfig + " not found");
      }

      // Validate that we found at least one query
      if (queriesConfig.isEmpty()) {
        throw new RuntimeException("no valid queries were found");
      } else {
        logger.info("found a total of " + queriesConfig.size() + " queries");
      }
      return queriesConfig;
    }
  }

  /**
   * Opens and parses a single query JSON file, supporting both regular and gzipped files.
   *
   * @param jsonConfig The JSON file to parse (can be .json or .json.gz)
   * @return List of QueryConfig objects parsed from the file
   * @throws RuntimeException if the file cannot be read or parsed
   */
  public List<QueryConfig> openQueryJson(File jsonConfig) {
    logger.info("opening " + jsonConfig);
    List<QueryConfig> parsedQueryConfigs = new ArrayList<>();

    if (jsonConfig.toString().endsWith(".json.gz")) {
      // Handle gzipped JSON files
      try (GZIPInputStream gzst = new GZIPInputStream(Files.newInputStream(jsonConfig.toPath()))) {
        try (Scanner scanner = new Scanner(gzst)) {
          parsedQueryConfigs = parseQueryConfigs(scanner);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else if (jsonConfig.toString().endsWith(".json")) {
      // Handle regular JSON files
      try (InputStream st = Files.newInputStream(jsonConfig.toPath())) {
        try (Scanner scanner = new Scanner(st)) {
          parsedQueryConfigs = parseQueryConfigs(scanner);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      // Skip unsupported file types
      logger.warning("file type not supported - skipping " + jsonConfig);
    }
    return parsedQueryConfigs;
  }

  /**
   * Parses query configurations from a scanner (reading line-by-line JSON). Each line should
   * contain a JSON object representing a query from Dremio's query history. Applies filtering to
   * exclude internal queries, failed queries, and DDL/DML operations.
   *
   * @param scanner Scanner to read JSON lines from
   * @return List of QueryConfig objects parsed and filtered from the input
   * @throws JsonProcessingException if JSON parsing fails
   */
  public List<QueryConfig> parseQueryConfigs(Scanner scanner) throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    List<QueryConfig> configs = new ArrayList<>();
    int skipCount = 0;
    int includeCount = 0;

    while (scanner.hasNextLine()) {
      final String line = scanner.nextLine();
      final QueryJsonRow row = objectMapper.readValue(line, QueryJsonRow.class);
      final QueryConfig query = new QueryConfig();

      // Apply filtering rules to determine if query should be included
      if (skipQuery(row)) {
        skipCount += 1;
        continue;
      } else {
        includeCount += 1;
      }

      // Parse SQL context (schema/catalog path)
      String context = row.getContext();
      List<String> sqlContext = new ArrayList<>();
      if (context != null && !context.equals("") && !context.equals("[]")) {
        // TODO: May need additional handling of escaped characters like \"
        context = context.substring(1, context.length() - 1); // Remove square brackets
        sqlContext = Arrays.asList(context.split(",\\s*"));
      }

      // Process query text and apply result limiting if configured
      String queryText = row.getQueryText();
      if (!Objects.isNull(limitResults) && limitResults > 0) {
        if (queryText.toLowerCase().contains("limit")) {
          // Replace existing LIMIT clause
          queryText = queryText.replaceAll("limit \\d+", "limit " + limitResults);
          queryText = queryText.replaceAll("LIMIT \\d+", "LIMIT " + limitResults);
        } else {
          // Add LIMIT clause if none exists
          queryText += " LIMIT " + limitResults;
        }
      }

      // Add metadata comment to identify the original query
      String queryId = row.getQueryId();
      queryText = "--Replay of " + queryId + "\n" + queryText;

      // Configure the query object
      query.setFrequency(1);
      query.setParameters(new HashMap<>());
      query.setQuery(queryText);
      query.setSqlContext(sqlContext);
      configs.add(query);
    }

    // Report parsing statistics
    System.out.println("Total number of queries included: " + includeCount);
    System.out.println("Total number of queries excluded: " + skipCount);
    return configs;
  }

  /**
   * Determines whether a query should be skipped based on filtering criteria. Filters out internal
   * Dremio queries, failed queries, non-SQL queries, and DDL/DML operations.
   *
   * @param row The query row from the JSON file to evaluate
   * @return true if the query should be skipped, false if it should be included
   */
  private boolean skipQuery(QueryJsonRow row) {
    if (row.getUsername().equals("$dremio$")) {
      // Internal queries are context dependent (e.g. on reflection IDs) and usually cannot be
      // re-run successfully in a different environment
      return true;
    } else if (!row.getOutcome().equals("COMPLETED")) {
      // Queries that did not finish successfully are not expected to work in replay
      return true;
    } else if (row.getQueryText().equals("NA")) {
      // Ignore non-SQL queries from ODBC/JDBC connections (metadata queries, etc.)
      return true;
    }

    // Filter out DDL/DML queries that could modify the database state
    String queryText = row.getQueryText().toLowerCase();
    String[] ddlKeywords = {
      "create ", // CREATE TABLE, VIEW, etc.
      "alter ", // ALTER TABLE, etc.
      "drop ", // DROP TABLE, VIEW, etc.
      "insert ", // INSERT INTO
      "update ", // UPDATE statements
      "delete ", // DELETE statements
      "grant ", // GRANT permissions
      "revoke ", // REVOKE permissions
      "password " // Password-related operations
    };

    for (String kw : ddlKeywords) {
      if (queryText.contains(kw)) {
        return true;
      }
    }
    return false;
  }
  /**
   * Executes the main stress test workflow.
   *
   * <p>This method orchestrates the entire stress test execution including: - Establishing
   * connection to Dremio - Loading and preparing queries - Setting up concurrent execution with
   * thread pool - Starting progress reporting - Managing query execution (sequential or random) -
   * Monitoring for completion conditions - Cleanup and shutdown
   *
   * @return exit code of the process (0 for success, 1 for failure)
   */
  public int run() {
    ExecutorService executorService = null; // Declare outside try
    Instant d = null; // Declare outside try

    try { // Main try block
      // Establish connection to Dremio
      final DremioApi dremioApi = // Declare and assign here, make final
          this.connectApi.connect(
              dremioUser,
              dremioPassword,
              dremioHost,
              timeoutSeconds,
              protocol,
              skipSSLVerification);

      // Set up execution infrastructure
      final BlockingQueue<Runnable> queue = // Can be final inside try
          new LinkedBlockingQueue<>(this.maxQueriesInFlight * 1000);
      final List<QueryConfig> queryPool = getQueries(); // Can be final inside try
      final Map<String, QueryGroup> queryGroups =
          getStringQueryGroupMap(); // Can be final inside try

      // Initialize query index for sequential execution
      if (queriesSequence == QueriesSequence.SEQUENTIAL) {
        queryIndex.set(this.queryIndexForRestart);
      }

      // Create thread pool for concurrent query execution
      executorService = // Assign here
          new ThreadPoolExecutor(
              this.maxQueriesInFlight, this.maxQueriesInFlight, 0L, TimeUnit.MILLISECONDS, queue);

      // Start timing
      d = Instant.now(); // Assign here

      // Start progress reporting
      reporter.startReporting(d);

      // Start background monitoring for completion conditions
      stressMonitor.startMonitoring(d, executorService, queryPool.size()); // Use the new monitor

      // Main execution loop
      // This loop continues until the monitor signals the executor to shut down (isTerminated
      // becomes true).
      // InterruptedException from Thread.sleep inside the loop will cause the loop to exit and
      // propagate.
      while (!executorService.isTerminated()) { // Check if executor is terminated
        final int nextQuery;

        // Determine next query based on execution sequence
        if (queriesSequence == QueriesSequence.SEQUENTIAL) {
          if (queryIndex.get() + 1 < queryPool.size()) {
            nextQuery = queryIndex.incrementAndGet();
          } else {
            // All queries submitted in sequential mode, wait for completion
            final int waitTime = 10;
            System.out.println(
                "finished submitting queries, waiting "
                    + waitTime
                    + "s for latest queries to finish...");
            Thread.sleep(waitTime * 1000); // Can throw InterruptedException
            continue; // Continue checking isTerminated()
          }
        } else if (queriesSequence == QueriesSequence.RANDOM) {
          nextQuery = random.nextInt(queryPool.size());
        } else {
          throw new RuntimeException("unexpected queriesSequence: " + queriesSequence);
        }

        // Process the selected query and submit for execution
        final QueryConfig query = queryPool.get(nextQuery);
        final List<Query> mappedSqls = mapSql(query, queryGroups);
        for (final Query mappedSql : mappedSqls) {
          // Create a runnable task that executes the query asynchronously
          // Need a final variable for the lambda to capture
          final Query currentMappedSql = mappedSql;
          final Runnable runnable = () -> runQuery(dremioApi, currentMappedSql);
          // submit can throw RejectedExecutionException if executor is shutting down
          executorService.submit(runnable);
          reporter.incrementCounter();
        }

        // Throttle submission if queue becomes too large
        if (queue.size() > this.maxQueriesInFlight * 10) {
          logger.fine("pausing as queue is too large");
          while (queue.size() > this.maxQueriesInFlight * 5) {
            Thread.sleep(500); // Can throw InterruptedException
          }
        }
      }
      // Loop finished (either terminated or interrupted)

    } catch (InterruptedException e) {
      // Handle interruption during the loop or setup before the loop starts
      // Note: connectApi.connect can throw InterruptedException in some implementations
      logger.log(Level.SEVERE, "StressExec main thread interrupted during execution or setup", e);
      Thread.currentThread().interrupt(); // Restore interrupt flag
      // Return 1 indicates failure
      return 1;
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Unable to connect or read config", e);
      return 1;
    } catch (RuntimeException e) {
      logger.log(Level.SEVERE, "An unexpected error occurred during execution", e);
      return 1;
    } finally {
      // Clean up resources regardless of how the try block exited
      // Ensure executor shutdown if not already terminated (failsafe)
      // Note: executorService is declared outside the try block now
      if (executorService != null && !executorService.isTerminated()) {
        executorService.shutdownNow();
        try {
          // Wait a bit for tasks to cancel or finish
          if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.warning(
                "ExecutorService did not terminate within 10 seconds after shutdownNow.");
          }
        } catch (InterruptedException e) {
          logger.log(Level.WARNING, "Interrupted while waiting for executor to terminate.", e);
          Thread.currentThread().interrupt();
        }
      }
      // Print final summary here, after potential shutdown and wait
      // Note: d is declared outside the try block now. If d is null, setup failed before
      // reporting could start.
      if (d != null) { // Check if 'd' was assigned (implies reporting was started)
        reporter.printFinalSummary(d); // d should be assigned if this condition is met
        reporter.stopReporting(); // Ensure reporting stops
      } else {
        // If d is null, something failed before even starting the timer and reporter.
        // The relevant exception should already be logged by the catch blocks.
        logger.warning("Stress test setup did not complete successfully. Skipping final summary.");
      }
    }
    return 0;
  }

  /**
   * Builds a map of query groups from the stress configuration. Query groups allow organizing
   * related queries together for easier management.
   *
   * @return Map of query group names to QueryGroup objects
   * @throws InvalidParameterException if duplicate query group names are found
   */
  private Map<String, QueryGroup> getStringQueryGroupMap() {
    final Map<String, QueryGroup> queryGroups = new HashMap<>();
    if (this.fileType == QueriesGeneratorFileType.STRESS_JSON) {
      final StressConfig config = getConfig();
      if (config.getQueryGroups() != null) {
        for (final QueryGroup g : config.getQueryGroups()) {
          // Ensure unique query group names
          if (queryGroups.containsKey(g.getName())) {
            throw new InvalidParameterException(
                "unable to read stress yaml because there are least two query groups named "
                    + g.getName());
          }
          queryGroups.put(g.getName(), g);
        }
      }
    }
    return queryGroups;
  }

  /**
   * Expands query configurations based on their frequency settings. Queries with frequency > 1 will
   * be duplicated in the pool to increase their execution probability.
   *
   * @param config The stress configuration containing queries with frequency settings
   * @return List of QueryConfig objects with duplicates based on frequency
   */
  private static List<QueryConfig> getQueryConfigs(StressConfig config) {
    final List<QueryConfig> queryPool = new ArrayList<>();
    for (final QueryConfig q : config.getQueries()) {
      int i = 0;
      final int frequency = Math.max(q.getFrequency(), 1); // Ensure minimum frequency of 1
      while (i < frequency) {
        i++;
        queryPool.add(q); // Add query to pool 'frequency' number of times
      }
    }
    return queryPool;
  }

  /**
   * Maps a QueryConfig to one or more executable Query objects by processing parameters and
   * sequences.
   *
   * <p>This method handles: - Resolving query groups to their constituent queries - Parameter
   * substitution using random selection from parameter value lists - Sequence expansion for
   * generating multiple queries with incremental values - Setting SQL context for query execution
   *
   * @param q The QueryConfig to process
   * @param queryGroupsMap Map of query group names to QueryGroup objects
   * @return List of executable Query objects with parameters resolved
   */
  public List<Query> mapSql(final QueryConfig q, final Map<String, QueryGroup> queryGroupsMap) {
    final List<String> rawQueries = new ArrayList<>();

    // Resolve queries from either query group or direct query text
    if (q.getQueryGroup() != null && !q.getQueryGroup().isEmpty()) {
      final List<String> queries = queryGroupsMap.get(q.getQueryGroup()).getQueries();
      rawQueries.addAll(queries);
    } else if (q.getQuery() != null && !q.getQuery().isEmpty()) {
      rawQueries.add(q.getQuery());
    }

    // Prepare parameters for substitution
    final Map<String, List<Object>> parameters;
    if (q.getParameters() == null) {
      parameters = new HashMap<>();
    } else {
      parameters = q.getParameters();
    }

    final List<Query> mappedQueries = new ArrayList<>();
    Pattern tokenDetector = Pattern.compile("(:\\w+)"); // Matches :parameterName tokens

    for (final String sql : rawQueries) {
      String queryText = sql;

      // Perform parameter substitution
      if (!parameters.isEmpty()) {
        Matcher matcher = tokenDetector.matcher(queryText);
        while (matcher.find()) {
          String detectedToken = matcher.group(1); // e.g., ":param1"
          String parameterName = detectedToken.substring(1); // e.g., "param1"
          if (parameters.containsKey(parameterName)) {
            List<Object> value = parameters.get(parameterName);
            if (!value.isEmpty()) {
              // Randomly select a value from the parameter list
              final int valueIndex = random.nextInt(value.size());
              final String v = value.get(valueIndex).toString();
              queryText = queryText.replaceAll(detectedToken, v);
            }
          }
        }
      }

      // Handle sequence expansion (generates multiple queries with incremental values)
      if (q.getSequence() != null) {
        String parameterName = q.getSequence().getName();
        int start = q.getSequence().getStart();
        int end = q.getSequence().getEnd();
        int step = q.getSequence().getStep();
        String searchText = String.format(":%s", parameterName);

        // Generate one query for each value in the sequence
        for (int i = start; i <= end; i += step) {
          Query query = new Query();
          String queryTextWithSequence = queryText.replaceAll(searchText, String.valueOf(i));
          query.setQueryText(queryTextWithSequence);
          mappedQueries.add(query);
        }
      } else {
        // Single query without sequence expansion
        Query query = new Query();
        query.setContext(q.getSqlContext());
        query.setQueryText(queryText);
        mappedQueries.add(query);
      }
    }
    return mappedQueries;
  }
}
