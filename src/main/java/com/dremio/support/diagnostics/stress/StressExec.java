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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class StressExec {

  private static final Logger logger = Logger.getLogger(StressExec.class.getName());
  private final Random random;
  private final File jsonConfig;
  private final QueriesGeneratorFileType fileType;
  private final Protocol protocol;
  private final String dremioHost;
  private final String dremioUser;
  private final String dremioPassword;
  private final Integer timeoutSeconds;
  private final long durationTargetMS;
  private final Integer maxQueriesInFlight;
  private final ConnectApi connectApi;
  private final boolean skipSSLVerification;

  public StressExec(
      final ConnectApi connectApi,
      final File jsonConfig,
      final QueriesGeneratorFileType fileType,
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
        protocol,
        dremioHost,
        dremioUser,
        dremioPassword,
        maxQueriesInFlight,
        timeoutSeconds,
        durationSeconds,
        skipSSLVerification);
  }

  public StressExec(
      final Random random,
      final ConnectApi connectApi,
      final File jsonConfig,
      final QueriesGeneratorFileType fileType,
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
    this.protocol = protocol;
    this.dremioHost = dremioHost;
    this.dremioUser = dremioUser;
    this.dremioPassword = dremioPassword;
    this.maxQueriesInFlight = maxQueriesInFlight;
    this.timeoutSeconds = timeoutSeconds;
    this.durationTargetMS = durationSeconds * 1000L;
    this.skipSSLVerification = skipSSLVerification;
  }

  private final AtomicInteger counter = new AtomicInteger(0);
  private final AtomicInteger submittedCounter = new AtomicInteger(0);
  private final AtomicInteger failureCounter = new AtomicInteger(0);
  private final AtomicInteger successfulCounter = new AtomicInteger(0);
  private final AtomicLong totalDurationMS = new AtomicLong(0);

  private final Timer timer = new Timer();
  long durationLastRun = 0;
  long successfulLastRun = 0;
  int failuresLastRun = 0;
  int submittedLastRun = 0;

  private void startReporting(Instant d) {

    timer.schedule(
        new TimerTask() {
          public void run() {
            final Instant now = Instant.now();
            final long msElapsed = now.toEpochMilli() - d.toEpochMilli();
            final int successful = successfulCounter.get();
            final int failures = failureCounter.get();
            final int submitted = submittedCounter.get();

            final long successfulThisRun = successful - successfulLastRun;
            successfulLastRun = successful;
            final long secondsElapsed = (msElapsed - durationLastRun) / 1000;
            durationLastRun = msElapsed;
            final int failuresThisRun = failures - failuresLastRun;
            failuresLastRun = failures;
            final int submittedThisRun = submitted - submittedLastRun;
            submittedLastRun = submitted;
            System.out.printf(
                "%s - queries submitted (total): %d; queries successful (total): %d; queries"
                    + " successful per second (current phase): %.2f; failure rate: %.2f %% (current"
                    + " phase) - time elapsed: %s/%s%n",
                Instant.now(),
                submitted,
                successful,
                (float) successfulThisRun / secondsElapsed,
                ((float) failuresThisRun / submittedThisRun) * 100.0,
                Human.getHumanDurationFromMillis(msElapsed),
                Human.getHumanDurationFromMillis(durationTargetMS));
          }
        },
        5 * 1000,
        5 * 1000);
  }

  private StressConfig getConfig() {
    try (InputStream st = Files.newInputStream(jsonConfig.toPath())) {
      final ObjectMapper objectMapper = new ObjectMapper();
      // TODO cache value
      return objectMapper.readValue(st, StressConfig.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void runQuery(DremioApi dremioApi, Query mappedSql) {
    {
      try {
        Instant startTime = Instant.now();
        DremioApiResponse response = null;
        submittedCounter.incrementAndGet();
        response = dremioApi.runSQL(mappedSql.getQueryText(), mappedSql.getContext());
        if (response == null) {
          throw new RuntimeException(
              String.format("query %s failed with an empty response", mappedSql));
        }
        if (!response.isSuccessful()) {
          final String errMsg = response.getErrorMessage();
          throw new RuntimeException(
              String.format("query %s failed with error %s", mappedSql, errMsg));
        }
        Instant endTime = Instant.now();
        long queryTime = endTime.toEpochMilli() - startTime.toEpochMilli();
        totalDurationMS.addAndGet(queryTime);
        successfulCounter.incrementAndGet();
        logger.info(() -> String.format("query %s successful", mappedSql));
      } catch (final Exception e) {
        failureCounter.incrementAndGet();
        logger.info(
            () ->
                String.format(
                    "query %s failed %s %s", mappedSql, e, ExceptionUtils.getStackTrace(e)));
      }
    }
  }

  public List<QueryConfig> getQueries() {
    if (this.fileType == QueriesGeneratorFileType.STRESS_JSON) {
      final StressConfig config = getConfig();
      return getQueryConfigs(config);
    } else {
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
      if (queriesConfig.isEmpty()) {
        throw new RuntimeException("no valid queries were found");
      } else {
        logger.info("found a total of " + queriesConfig.size() + " queries");
      }
      return queriesConfig;
    }
  }

  public List<QueryConfig> openQueryJson(File jsonConfig) {
    logger.info("opening " + jsonConfig);
    List<QueryConfig> parsedQueryConfigs = new ArrayList<>();

    if (jsonConfig.toString().endsWith(".json.gz")) {
      try (GZIPInputStream gzst = new GZIPInputStream(Files.newInputStream(jsonConfig.toPath()))) {
        try (Scanner scanner = new Scanner(gzst)) {
          parsedQueryConfigs = parseQueryConfigs(scanner);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else if (jsonConfig.toString().endsWith(".json")) {
      try (InputStream st = Files.newInputStream(jsonConfig.toPath())) {
        try (Scanner scanner = new Scanner(st)) {
          parsedQueryConfigs = parseQueryConfigs(scanner);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      logger.warning("file type not supported - skipping " + jsonConfig);
    }
    return parsedQueryConfigs;
  }

  public List<QueryConfig> parseQueryConfigs(Scanner scanner) throws JsonProcessingException {
    final ObjectMapper objectMapper = new ObjectMapper();
    List<QueryConfig> configs = new ArrayList<>();
    int skipCount = 0;
    int includeCount = 0;
    while (scanner.hasNextLine()) {
      final String line = scanner.nextLine();
      final QueryJsonRow row = objectMapper.readValue(line, QueryJsonRow.class);
      final QueryConfig query = new QueryConfig();
      if (skipQuery(row)) {
        skipCount += 1;
        continue;
      } else {
        includeCount += 1;
      }
      String context = row.getContext();
      List<String> sqlContext = new ArrayList<>();
      if (!context.equals("") && !context.equals("[]")) {
        // TODO: May need additional handling of escaped chars, like \"
        context = context.substring(1, context.length() - 1); // Remove square brackets
        sqlContext = Arrays.asList(context.split(",\\s*"));
      }
      String queryText = row.getQueryText();
      boolean addLimit = false; // TODO: Pass in as config or CLI argument
      if (addLimit) {
        if (queryText.toLowerCase().contains("limit")) {
          queryText = queryText.replaceAll("limit \\d+", "limit 1");
          queryText = queryText.replaceAll("LIMIT \\d+", "LIMIT 1");
        } else {
          queryText += " LIMIT 1";
        }
      }

      query.setFrequency(1);
      query.setParameters(new HashMap<>());
      query.setQuery(queryText);
      query.setSqlContext(sqlContext);
      configs.add(query);
    }
    System.out.println("Total number of queries included: " + includeCount);
    System.out.println("Total number of queries excluded: " + skipCount);
    return configs;
  }

  private boolean skipQuery(QueryJsonRow row) {
    if (row.getUsername().equals("$dremio$")) {
      // Internal queries are context dependent (e.g. on reflection IDs) and usually cannot be
      // re-run
      return true;
    } else if (!row.getOutcome().equals("COMPLETED")) {
      // Queries that did not finish successfully, are not expected to work
      return true;
    } else if (row.getQueryText().equals("NA")) {
      // Ignore non-SQL queries from ODBC/JDBC connections
      return true;
    }
    // Ignore DDL/DML queries
    String queryText = row.getQueryText().toLowerCase();
    String[] ddlKeywords = {
      "create ",
      "alter ",
      "drop ",
      "insert ",
      "update ",
      "delete ",
      "grant ",
      "revoke ",
      "password "
    };
    for (String kw : ddlKeywords) {
      if (queryText.contains(kw)) {
        return true;
      }
    }
    return false;
  }
  /**
   * The stress job
   *
   * @return exit code of the process
   */
  public int run() {
    try {
      final DremioApi dremioApi =
          this.connectApi.connect(
              dremioUser,
              dremioPassword,
              dremioHost,
              timeoutSeconds,
              protocol,
              skipSSLVerification);

      final BlockingQueue<Runnable> queue =
          new LinkedBlockingQueue<>(this.maxQueriesInFlight * 1000);
      final List<QueryConfig> queryPool = getQueries();
      final Map<String, QueryGroup> queryGroups = getStringQueryGroupMap();
      final ExecutorService executorService =
          new ThreadPoolExecutor(
              this.maxQueriesInFlight, this.maxQueriesInFlight, 0L, TimeUnit.MILLISECONDS, queue);
      final Instant d = Instant.now();
      startReporting(d);
      try {
        monitorForEnd(d, executorService);
        while (!executorService.isShutdown()) {
          final int nextQuery = random.nextInt(queryPool.size());
          final QueryConfig query = queryPool.get(nextQuery);
          final List<Query> mappedSqls = mapSql(query, queryGroups);
          for (final Query mappedSql : mappedSqls) {
            final Runnable runnable = () -> runQuery(dremioApi, mappedSql);
            executorService.submit(runnable);
            counter.incrementAndGet();
          }
          if (queue.size() > this.maxQueriesInFlight * 10) {
            logger.fine("pausing as queue is too large");
            while (queue.size() > this.maxQueriesInFlight * 5) {
              // take out time pausing while we let the queue clear out
              Thread.sleep(500);
            }
          }
        }
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      } finally {
        timer.cancel();
        executorService.shutdown();
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE, "unable to connect", e);
      return 1;
    }
    return 0;
  }

  private void monitorForEnd(Instant d, ExecutorService executorService) {
    new Thread(
            () -> {
              while (true) {
                try {
                  Thread.sleep(5 * 1000);
                } catch (InterruptedException e) {
                  throw new RuntimeException(e);
                }
                final Instant now = Instant.now();
                long msElapsed = now.toEpochMilli() - d.toEpochMilli();
                if (msElapsed > durationTargetMS) {
                  final int submitted = submittedCounter.get();
                  final int successful = successfulCounter.get();
                  final int failures = failureCounter.get();
                  final long secondsElapsed = msElapsed / 1000;
                  try {
                    Thread.sleep(5 * 1000);
                  } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                  }
                  System.out.printf(
                      "%s - Stress Summary: queries submitted: %d; queries successful: %d; queries"
                          + " successful per second: %.2f; failure rate: %.2f %% - time elapsed:"
                          + " %s/%s%n",
                      Instant.now(),
                      submitted,
                      successful,
                      (float) submitted / secondsElapsed,
                      ((float) failures / submitted) * 100.0,
                      Human.getHumanDurationFromMillis(msElapsed),
                      Human.getHumanDurationFromMillis(durationTargetMS));
                  executorService.shutdownNow();
                }
              }
            },
            "reporting")
        .start();
  }

  private Map<String, QueryGroup> getStringQueryGroupMap() {
    final Map<String, QueryGroup> queryGroups = new HashMap<>();
    if (this.fileType == QueriesGeneratorFileType.STRESS_JSON) {
      final StressConfig config = getConfig();
      if (config.getQueryGroups() != null) {
        for (final QueryGroup g : config.getQueryGroups()) {
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

  private static List<QueryConfig> getQueryConfigs(StressConfig config) {
    final List<QueryConfig> queryPool = new ArrayList<>();
    for (final QueryConfig q : config.getQueries()) {
      int i = 0;
      final int frequency = Math.max(q.getFrequency(), 1);
      while (i < frequency) {
        i++;
        queryPool.add(q);
      }
    }
    return queryPool;
  }

  public List<Query> mapSql(final QueryConfig q, final Map<String, QueryGroup> queryGroupsMap) {
    final List<String> rawQueries = new ArrayList<>();
    if (q.getQueryGroup() != null && !q.getQueryGroup().isEmpty()) {
      final List<String> queries = queryGroupsMap.get(q.getQueryGroup()).getQueries();
      rawQueries.addAll(queries);
    } else if (q.getQuery() != null && !q.getQuery().isEmpty()) {
      rawQueries.add(q.getQuery());
    }
    final Map<String, List<Object>> parameters;
    if (q.getParameters() == null) {
      parameters = new HashMap<>();
    } else {
      parameters = q.getParameters();
    }
    final List<Query> mappedQueries = new ArrayList<>();
    for (final String sql : rawQueries) {
      final Query query = new Query();
      query.setContext(q.getSqlContext());
      if (parameters.size() > 0) {
        final String[] tokens = sql.split(" ");
        final int words = tokens.length;
        for (int i = 0; i < words; i++) {
          final String word = tokens[i];
          for (final Entry<String, List<Object>> x : parameters.entrySet()) {
            if (word.equals(":" + x.getKey())) {
              final int valueCount = x.getValue().size();
              if (valueCount > 0) {
                final int valueIndex = random.nextInt(valueCount);
                final String v = String.valueOf(x.getValue().get(valueIndex));
                tokens[i] = v;
              }
            } else if (word.equals("':" + x.getKey() + "'")) {
              final int valueCount = x.getValue().size();
              if (valueCount > 0) {
                final int valueIndex = random.nextInt(valueCount);
                final String v = String.valueOf(x.getValue().get(valueIndex));
                tokens[i] = "'" + v + "'";
              }
            }
          }
        }
        query.setQueryText(String.join(" ", tokens));
      } else {
        query.setQueryText(sql);
      }
      mappedQueries.add(query);
    }
    return mappedQueries;
  }
}
