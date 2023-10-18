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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StressExec {

  private static final Logger logger = Logger.getLogger(StressExec.class.getName());
  private final Random random;
  private final File jsonConfig;
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
            final int submittedThisRun = submitted- submittedLastRun;
            submittedLastRun = submitted;
            System.out.printf(
                "%s - queries submitted (total): %d; queries successful (total): %d; queries successful per second (current phase): %.2f;"
                    + " failure rate: %.2f %% (current phase) - time elapsed: %s/%s%n",
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
      return objectMapper.readValue(st, StressConfig.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The stress job
   *
   * @return exit code of the process
   */
  public int run() {
    final FileMaker noOpFileMaker = () -> Paths.get("");
    try {
      final DremioApi dremioApi =
          this.connectApi.connect(
              dremioUser,
              dremioPassword,
              dremioHost,
              noOpFileMaker,
              timeoutSeconds,
              protocol,
              skipSSLVerification);

      var config = getConfig();
      final BlockingQueue<Runnable> queue =
          new LinkedBlockingQueue<>(this.maxQueriesInFlight * 1000);
      final List<QueryConfig> queryPool = getQueryConfigs(config);
      final Map<String, QueryGroup> queryGroups = getStringQueryGroupMap(config);
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
            final Runnable runnable =
                () -> {
                  Instant startTime = Instant.now();
                  DremioApiResponse response = null;
                  try {
                    submittedCounter.incrementAndGet();
                    response = dremioApi.runSQL(mappedSql.queryText(), query.sqlContext());
                  } catch (final Exception e) {
                    failureCounter.incrementAndGet();
                    logger.info(
                        () ->
                            String.format(
                                "query %s failed with error %s", mappedSql, e.getMessage()));
                  }
                  if (response != null) {
                    Instant endTime = Instant.now();
                    long queryTime = endTime.toEpochMilli() - startTime.toEpochMilli();
                    totalDurationMS.addAndGet(queryTime);
                    if (response.isSuccessful()) {
                      successfulCounter.incrementAndGet();
                      logger.info(() -> String.format("query %s successful", mappedSql));
                    } else {
                      failureCounter.incrementAndGet();
                      final String errMsg = response.getErrorMessage();
                      logger.info(
                          () -> String.format("query %s failed with error %s", mappedSql, errMsg));
                    }
                  } else {
                    failureCounter.incrementAndGet();
                  }
                };
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
                  System.out.printf("%s - Stress Summary: queries submitted: %d; queries successful: %d; queries"
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

  private static Map<String, QueryGroup> getStringQueryGroupMap(StressConfig config) {
    final Map<String, QueryGroup> queryGroups = new HashMap<>();
    if (config.queryGroups() != null) {
      for (final QueryGroup g : config.queryGroups()) {
        if (queryGroups.containsKey(g.name())) {
          throw new InvalidParameterException(
              "unable to read stress yaml because there are least two query groups named "
                  + g.name());
        }
        queryGroups.put(g.name(), g);
      }
    }
    return queryGroups;
  }

  private static List<QueryConfig> getQueryConfigs(StressConfig config) {
    final List<QueryConfig> queryPool = new ArrayList<>();
    for (final QueryConfig q : config.queries()) {
      int i = 0;
      final int frequency = Math.max(q.frequency(), 1);
      while (i < frequency) {
        i++;
        queryPool.add(q);
      }
    }
    return queryPool;
  }

  public List<Query> mapSql(final QueryConfig q, final Map<String, QueryGroup> queryGroupsMap) {
    final List<String> rawQueries = new ArrayList<>();
    if (q.queryGroup() != null && !q.queryGroup().isEmpty()) {
      final List<String> queries = queryGroupsMap.get(q.queryGroup()).queries();
      rawQueries.addAll(queries);
    } else if (q.query() != null && !q.query().isEmpty()) {
      rawQueries.add(q.query());
    }
    final List<Query> mappedQueries = new ArrayList<>();
    for (final String sql : rawQueries) {
      final var tokens = sql.split(" ");
      final int words = tokens.length;
      for (int i = 0; i < words; i++) {
        final String word = tokens[i];
        for (final var x : q.parameters().entrySet()) {
          if (word.equals(":" + x.getKey())) {
            final int valueCount = x.getValue().length;
            if (valueCount > 0) {
              final int valueIndex = random.nextInt(valueCount);
              final String v = String.valueOf(x.getValue()[valueIndex]);
              tokens[i] = v;
            }
          }
        }
      }
      mappedQueries.add(new Query(String.join(" ", tokens), q.sqlContext()));
    }
    return mappedQueries;
  }
}
