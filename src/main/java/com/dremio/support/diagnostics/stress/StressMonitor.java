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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit; // Import TimeUnit
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StressMonitor is responsible for monitoring the progress and duration of a stress test and
 * triggering the shutdown of the executor service when completion conditions are met.
 */
public class StressMonitor {

  private static final Logger logger = Logger.getLogger(StressMonitor.class.getName());

  private final long durationTargetMS;
  private final AtomicInteger queryIndex;
  private final StressReporter reporter;

  /**
   * Constructs a StressMonitor.
   *
   * @param durationTargetMS Target duration for stress test in milliseconds.
   * @param queryIndex AtomicInteger tracking the current query index (for sequential execution).
   * @param reporter StressReporter instance for progress reporting.
   */
  public StressMonitor(long durationTargetMS, AtomicInteger queryIndex, StressReporter reporter) {
    this.durationTargetMS = durationTargetMS;
    this.queryIndex = queryIndex;
    this.reporter = reporter;
  }

  /**
   * Starts a background monitoring thread that checks for completion conditions. The monitor checks
   * periodically for either time-based or query-count-based completion. When completion conditions
   * are met, it prints a final summary and shuts down the executor.
   *
   * @param startTime Start time of the stress test.
   * @param executorService The executor service to shut down when complete.
   * @param totalQueries Total number of queries available for sequential execution.
   */
  public void startMonitoring(
      Instant startTime, ExecutorService executorService, Integer totalQueries) {
    new Thread(
            // Background monitoring thread that checks for completion conditions
            () -> {
              while (!executorService.isShutdown()) { // Continue as long as executor is running
                try {
                  Thread.sleep(5 * 1000); // Check every 5 seconds
                } catch (InterruptedException e) {
                  logger.log(Level.INFO, "Stress monitor thread interrupted", e);
                  Thread.currentThread().interrupt(); // Restore interrupt flag
                  break; // Exit loop if interrupted
                }

                final Instant now = Instant.now();
                long msElapsed = now.toEpochMilli() - startTime.toEpochMilli();

                // Check completion conditions: time limit reached OR all queries processed
                // (sequential mode)
                if (msElapsed > durationTargetMS || queryIndex.get() + 1 >= totalQueries) {
                  logger.info("Completion condition met. Shutting down executor.");
                  // Allow a small grace period for in-flight queries to finish if possible
                  // before forcing shutdown. Note: executorService.shutdownNow() will interrupt
                  // running tasks.
                  try {
                    // Give a short time, but don't block indefinitely
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                      logger.warning(
                          "Executor service did not terminate gracefully within 5 seconds. Forcing shutdown.");
                      executorService.shutdownNow(); // Force shutdown if grace period expires
                    }
                  } catch (InterruptedException e) {
                    logger.log(
                        Level.WARNING,
                        "Stress monitor thread interrupted during awaitTermination",
                        e);
                    executorService.shutdownNow(); // Force shutdown on interruption
                    Thread.currentThread().interrupt(); // Restore interrupt flag
                  }

                  // Print final summary report outside the thread or ensure it's synchronized
                  // or wait for thread completion before printing final summary in run()
                  // For now, print here, but might need refactoring for proper order.
                  // Let's rely on the main run() method to print the final summary after
                  // the executor fully terminates. So, remove printFinalSummary from here.
                  executorService.shutdownNow(); // Ensure shutdown happens

                  break; // Exit monitoring loop
                }
              }
              logger.info("Stress monitor thread finished.");
            },
            "stress-monitor-thread") // Named thread for easier debugging
        .start();
  }
}
