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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for StressReporter class. */
public class StressReporterTest {

  private StressReporter reporter;
  private AtomicInteger queryIndex;
  private final long durationTargetMS = 60000; // 60 seconds
  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() {
    queryIndex = new AtomicInteger(0);
    reporter = new StressReporter(queryIndex, durationTargetMS);

    // Capture System.out for testing output
    outputStream = new ByteArrayOutputStream();
    originalOut = System.out;
    System.setOut(new PrintStream(outputStream));
  }

  @AfterEach
  void tearDown() {
    reporter.stopReporting();
    System.setOut(originalOut);
  }

  @Test
  void testInitialCounters() {
    assertEquals(0, reporter.getSubmittedCount());
    assertEquals(0, reporter.getSuccessfulCount());
    assertEquals(0, reporter.getFailureCount());
  }

  @Test
  void testIncrementSubmittedCounter() {
    reporter.incrementSubmittedCounter();
    assertEquals(1, reporter.getSubmittedCount());

    reporter.incrementSubmittedCounter();
    assertEquals(2, reporter.getSubmittedCount());
  }

  @Test
  void testIncrementSuccessfulCounter() {
    long queryDuration = 1500; // 1.5 seconds
    reporter.incrementSuccessfulCounter(queryDuration);
    assertEquals(1, reporter.getSuccessfulCount());

    reporter.incrementSuccessfulCounter(2500);
    assertEquals(2, reporter.getSuccessfulCount());
  }

  @Test
  void testIncrementFailureCounter() {
    reporter.incrementFailureCounter();
    assertEquals(1, reporter.getFailureCount());

    reporter.incrementFailureCounter();
    assertEquals(2, reporter.getFailureCount());
  }

  @Test
  void testIncrementCounter() {
    reporter.incrementCounter();
    reporter.incrementCounter();
    // Note: incrementCounter is internal, no direct getter available
    // This test mainly ensures no exceptions are thrown
  }

  @Test
  void testMultipleCounterOperations() {
    reporter.incrementSubmittedCounter();
    reporter.incrementSubmittedCounter();
    reporter.incrementSuccessfulCounter(1000);
    reporter.incrementFailureCounter();

    assertEquals(2, reporter.getSubmittedCount());
    assertEquals(1, reporter.getSuccessfulCount());
    assertEquals(1, reporter.getFailureCount());
  }

  @Test
  void testPrintFinalSummary() {
    // Set up some test data
    reporter.incrementSubmittedCounter();
    reporter.incrementSubmittedCounter();
    reporter.incrementSuccessfulCounter(1500);
    reporter.incrementFailureCounter();
    queryIndex.set(42);

    Instant startTime = Instant.now().minusSeconds(30); // 30 seconds ago
    reporter.printFinalSummary(startTime);

    String output = outputStream.toString();

    // Verify the summary contains expected information
    assertTrue(output.contains("Stress Summary"));
    assertTrue(output.contains("queries submitted: 2"));
    assertTrue(output.contains("queries successful: 1"));
    assertTrue(output.contains("failure rate: 50.00"));
    assertTrue(output.contains("last query index: 42"));
    assertTrue(output.contains("time elapsed"));
  }

  @Test
  void testReportingStartAndStop() {
    Instant startTime = Instant.now();

    // Start reporting
    reporter.startReporting(startTime);

    // Wait a short time to ensure timer is active
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Stop reporting - should not throw exception
    assertDoesNotThrow(() -> reporter.stopReporting());
  }

  @Test
  void testZeroFailureRate() {
    reporter.incrementSubmittedCounter();
    reporter.incrementSubmittedCounter();
    reporter.incrementSuccessfulCounter(1000);
    reporter.incrementSuccessfulCounter(2000);
    // No failures

    queryIndex.set(10);
    Instant startTime = Instant.now().minusSeconds(10);
    reporter.printFinalSummary(startTime);

    String output = outputStream.toString();
    assertTrue(output.contains("failure rate: 0.00"));
  }

  @Test
  void testHundredPercentFailureRate() {
    reporter.incrementSubmittedCounter();
    reporter.incrementSubmittedCounter();
    reporter.incrementFailureCounter();
    reporter.incrementFailureCounter();
    // No successes

    queryIndex.set(5);
    Instant startTime = Instant.now().minusSeconds(15);
    reporter.printFinalSummary(startTime);

    String output = outputStream.toString();
    assertTrue(output.contains("failure rate: 100.00"));
  }

  @Test
  void testQueryIndexTracking() {
    queryIndex.set(100);

    reporter.incrementSubmittedCounter();
    reporter.incrementSuccessfulCounter(500);

    Instant startTime = Instant.now().minusSeconds(5);
    reporter.printFinalSummary(startTime);

    String output = outputStream.toString();
    assertTrue(output.contains("last query index: 100"));
  }

  @Test
  void testConcurrentCounterOperations() throws InterruptedException {
    int numThreads = 10;
    int operationsPerThread = 100;
    Thread[] threads = new Thread[numThreads];

    // Create threads that increment counters concurrently
    for (int i = 0; i < numThreads; i++) {
      threads[i] =
          new Thread(
              () -> {
                for (int j = 0; j < operationsPerThread; j++) {
                  reporter.incrementSubmittedCounter();
                  if (j % 2 == 0) {
                    reporter.incrementSuccessfulCounter(1000);
                  } else {
                    reporter.incrementFailureCounter();
                  }
                }
              });
    }

    // Start all threads
    for (Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // Verify final counts
    assertEquals(numThreads * operationsPerThread, reporter.getSubmittedCount());
    assertEquals(numThreads * operationsPerThread / 2, reporter.getSuccessfulCount());
    assertEquals(numThreads * operationsPerThread / 2, reporter.getFailureCount());
  }
}
