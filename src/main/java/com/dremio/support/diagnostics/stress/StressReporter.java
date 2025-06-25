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

import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles progress reporting and metrics tracking for stress test execution.
 *
 * <p>This class manages all reporting functionality including: - Query execution counters
 * (submitted, successful, failed) - Periodic progress reporting every 5 seconds - Final summary
 * reporting - Execution timing and throughput calculations
 */
public class StressReporter {

  // Metrics and tracking fields
  private final AtomicInteger counter = new AtomicInteger(0); // Total queries processed
  private final AtomicInteger submittedCounter = new AtomicInteger(0); // Total queries submitted
  private final AtomicInteger failureCounter = new AtomicInteger(0); // Total failed queries
  private final AtomicInteger successfulCounter = new AtomicInteger(0); // Total successful queries
  private final AtomicLong totalDurationMS = new AtomicLong(0); // Total execution time

  // Progress reporting fields
  private final Timer timer = new Timer(); // Timer for periodic reporting
  private long durationLastRun = 0; // Duration at last report
  private long successfulLastRun = 0; // Successful queries at last report
  private int failuresLastRun = 0; // Failed queries at last report
  private int submittedLastRun = 0; // Submitted queries at last report
  private final AtomicInteger queryIndex; // Current query index for sequential execution
  private final long durationTargetMS; // Target duration for stress test

  /**
   * Creates a new StressReporter instance.
   *
   * @param queryIndex AtomicInteger tracking current query index for sequential execution
   * @param durationTargetMS Target duration for the stress test in milliseconds
   */
  public StressReporter(AtomicInteger queryIndex, long durationTargetMS) {
    this.queryIndex = queryIndex;
    this.durationTargetMS = durationTargetMS;
  }

  /** Increments the counter for total queries processed. */
  public void incrementCounter() {
    counter.incrementAndGet();
  }

  /** Increments the counter for queries submitted. */
  public void incrementSubmittedCounter() {
    submittedCounter.incrementAndGet();
  }

  /** Increments the counter for failed queries. */
  public void incrementFailureCounter() {
    failureCounter.incrementAndGet();
  }

  /**
   * Increments the counter for successful queries and adds to total duration.
   *
   * @param queryDurationMS Duration of the successful query in milliseconds
   */
  public void incrementSuccessfulCounter(long queryDurationMS) {
    totalDurationMS.addAndGet(queryDurationMS);
    successfulCounter.incrementAndGet();
  }

  /**
   * Starts periodic progress reporting that runs every 5 seconds. Reports include total and
   * incremental metrics for throughput and failure rates.
   *
   * @param startTime Start time of the stress test for calculating elapsed time
   */
  public void startReporting(Instant startTime) {
    timer.schedule(
        new TimerTask() {
          /**
           * Periodic reporting task that calculates and displays progress metrics. Runs every 5
           * seconds to show current throughput, failure rates, and elapsed time.
           */
          public void run() {
            final Instant now = Instant.now();
            final long msElapsed = now.toEpochMilli() - startTime.toEpochMilli();
            final int successful = successfulCounter.get();
            final int failures = failureCounter.get();
            final int submitted = submittedCounter.get();
            final int index = queryIndex.get();

            // Calculate incremental metrics since last report
            final long successfulThisRun = successful - successfulLastRun;
            successfulLastRun = successful;
            final long secondsElapsed = (msElapsed - durationLastRun) / 1000;
            durationLastRun = msElapsed;
            final int failuresThisRun = failures - failuresLastRun;
            failuresLastRun = failures;
            final int submittedThisRun = submitted - submittedLastRun;
            submittedLastRun = submitted;

            // Print progress report with current and total metrics
            System.out.printf(
                "%s - queries submitted (total): %d; queries successful (total): %d; queries"
                    + " successful per second (current phase): %.2f; failure rate: %.2f %% (current"
                    + " phase) - time elapsed: %s/%s - last query index: %d%n",
                Instant.now(),
                submitted,
                successful,
                (float) successfulThisRun / secondsElapsed,
                ((float) failuresThisRun / submittedThisRun) * 100.0,
                Human.getHumanDurationFromMillis(msElapsed),
                Human.getHumanDurationFromMillis(durationTargetMS),
                index);
          }
        },
        5 * 1000, // Initial delay: 5 seconds
        5 * 1000); // Repeat interval: 5 seconds
  }

  /**
   * Prints the final summary report with overall statistics.
   *
   * @param startTime Start time of the stress test
   */
  public void printFinalSummary(Instant startTime) {
    final Instant now = Instant.now();
    final long msElapsed = now.toEpochMilli() - startTime.toEpochMilli();
    final int submitted = submittedCounter.get();
    final int successful = successfulCounter.get();
    final int failures = failureCounter.get();
    final int index = queryIndex.get();
    final long secondsElapsed = msElapsed / 1000;

    System.out.printf(
        "%s - Stress Summary: queries submitted: %d; queries successful: %d; queries"
            + " successful per second: %.2f; failure rate: %.2f %% - time elapsed:"
            + " %s/%s - last query index: %d%n",
        Instant.now(),
        submitted,
        successful,
        (float) submitted / secondsElapsed,
        ((float) failures / submitted) * 100.0,
        Human.getHumanDurationFromMillis(msElapsed),
        Human.getHumanDurationFromMillis(durationTargetMS),
        index);
  }

  /** Stops the periodic reporting timer. */
  public void stopReporting() {
    timer.cancel();
  }

  /**
   * Gets the current count of submitted queries.
   *
   * @return Number of submitted queries
   */
  public int getSubmittedCount() {
    return submittedCounter.get();
  }

  /**
   * Gets the current count of successful queries.
   *
   * @return Number of successful queries
   */
  public int getSuccessfulCount() {
    return successfulCounter.get();
  }

  /**
   * Gets the current count of failed queries.
   *
   * @return Number of failed queries
   */
  public int getFailureCount() {
    return failureCounter.get();
  }
}
