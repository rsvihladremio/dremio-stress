package com.dremio.support.diagnostics.stress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StressMonitorTest {

  @Mock private ExecutorService mockExecutorService;
  @Mock private StressReporter mockReporter;

  private AtomicInteger queryIndex;
  private StressMonitor stressMonitor;

  private TestLogHandler testLogHandler;
  private static final Logger stressMonitorLogger = Logger.getLogger(StressMonitor.class.getName());

  // Custom Log Handler for testing log outputs
  private static class TestLogHandler extends Handler {
    private final List<LogRecord> records = new CopyOnWriteArrayList<>();

    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void flush() {}

    @Override
    public void close() throws SecurityException {}

    public boolean hasLogMatching(Predicate<LogRecord> predicate) {
      return records.stream().anyMatch(predicate);
    }
  }

  @BeforeEach
  void setUp() {
    queryIndex = new AtomicInteger(0);

    testLogHandler = new TestLogHandler();
    stressMonitorLogger.addHandler(testLogHandler);
    // To prevent logs from propagating to parent handlers (e.g., console) during tests,
    // which can make test output noisy.
    // stressMonitorLogger.setUseParentHandlers(false); // Uncomment if console logs are too verbose
  }

  @AfterEach
  void tearDown() {
    stressMonitorLogger.removeHandler(testLogHandler);
    // stressMonitorLogger.setUseParentHandlers(true); // Restore if changed in setUp
  }

  @Test
  void testShutdownOnDurationExceeded() throws InterruptedException {
    // Target duration 10ms. Monitor checks every 5s, so it will trigger after ~5s.
    stressMonitor = new StressMonitor(10L, queryIndex, mockReporter);
    int totalQueries = 100; // High enough not to be the trigger

    when(mockExecutorService.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);

    stressMonitor.startMonitoring(Instant.now(), mockExecutorService, totalQueries);

    // Verify shutdownNow is called. The monitor sleeps for 5 seconds, then checks.
    verify(mockExecutorService, timeout(6000).atLeastOnce()).shutdownNow();

    // Allow time for logs to be processed
    Thread.sleep(200);
    assertTrue(
        testLogHandler.hasLogMatching(
            record ->
                record.getLevel() == Level.WARNING
                    && record
                        .getMessage()
                        .contains("Executor service did not terminate gracefully")),
        "Expected warning log for graceful shutdown failure was not found.");
  }

  @Test
  void testShutdownOnQueryCountReached() throws InterruptedException {
    long durationTargetMS = 60000L; // Long duration, won't be the trigger
    stressMonitor = new StressMonitor(durationTargetMS, queryIndex, mockReporter);
    int totalQueries = 1;
    queryIndex.set(0); // So, queryIndex.get() + 1 (which is 1) >= totalQueries (1)

    when(mockExecutorService.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);

    stressMonitor.startMonitoring(Instant.now(), mockExecutorService, totalQueries);

    verify(mockExecutorService, timeout(6000).atLeastOnce()).shutdownNow();

    Thread.sleep(200);
    assertTrue(
        testLogHandler.hasLogMatching(
            record ->
                record.getLevel() == Level.WARNING
                    && record
                        .getMessage()
                        .contains("Executor service did not terminate gracefully")),
        "Expected warning log for graceful shutdown failure was not found.");
  }

  @Test
  void testGracefulShutdownSuccess() throws InterruptedException {
    stressMonitor = new StressMonitor(10L, queryIndex, mockReporter);
    int totalQueries = 100;

    when(mockExecutorService.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);

    stressMonitor.startMonitoring(Instant.now(), mockExecutorService, totalQueries);

    // shutdownNow() is called at least once due to the unconditional call in StressMonitor.
    verify(mockExecutorService, timeout(6000).atLeastOnce()).shutdownNow();

    Thread.sleep(200);
    assertFalse(
        testLogHandler.hasLogMatching(
            record ->
                record.getLevel() == Level.WARNING
                    && record
                        .getMessage()
                        .contains("Executor service did not terminate gracefully")),
        "Unexpected warning log for graceful shutdown failure.");
  }

  @Test
  void testInterruptionDuringAwaitTermination() throws InterruptedException {
    stressMonitor = new StressMonitor(10L, queryIndex, mockReporter);
    int totalQueries = 100;

    when(mockExecutorService.awaitTermination(5, TimeUnit.SECONDS))
        .thenThrow(new InterruptedException("Test-induced interrupt"));

    stressMonitor.startMonitoring(Instant.now(), mockExecutorService, totalQueries);

    verify(mockExecutorService, timeout(6000).atLeastOnce()).shutdownNow();

    Thread.sleep(200);
    assertTrue(
        testLogHandler.hasLogMatching(
            record ->
                record.getLevel() == Level.WARNING
                    && record
                        .getMessage()
                        .contains("Stress monitor thread interrupted during awaitTermination")),
        "Expected warning log for interruption during awaitTermination was not found.");
  }

  @Test
  void testMonitorThreadInterruptedDuringSleep() throws InterruptedException {
    stressMonitor = new StressMonitor(Long.MAX_VALUE, queryIndex, mockReporter);
    int totalQueries = Integer.MAX_VALUE;
    queryIndex.set(0);

    when(mockExecutorService.isShutdown()).thenReturn(false);

    stressMonitor.startMonitoring(Instant.now(), mockExecutorService, totalQueries);

    Thread monitorThread = null;
    for (int i = 0; i < 20; i++) { // Try for a few seconds to find the thread
      monitorThread =
          Thread.getAllStackTraces().keySet().stream()
              .filter(t -> "stress-monitor-thread".equals(t.getName()))
              .findFirst()
              .orElse(null);
      if (monitorThread != null) break;
      Thread.sleep(100);
    }

    assertNotNull(monitorThread, "Could not find stress-monitor-thread");

    monitorThread.interrupt();
    monitorThread.join(1000); // Wait for thread to process interruption and finish

    Thread.sleep(200); // Allow logs to process
    assertTrue(
        testLogHandler.hasLogMatching(
            record ->
                record.getLevel() == Level.INFO
                    && record.getMessage().equals("Stress monitor thread interrupted")),
        "Expected info log for monitor thread interruption was not found.");

    // Verify that executorService methods related to shutdown are NOT called
    // when the monitor's sleep is interrupted, as it should just break its loop.
    verify(mockExecutorService, never()).shutdown();
    verify(mockExecutorService, never()).shutdownNow();
    verify(mockExecutorService, never()).awaitTermination(anyLong(), any(TimeUnit.class));
  }
}
