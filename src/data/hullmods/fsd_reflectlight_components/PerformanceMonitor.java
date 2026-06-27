package data.hullmods.fsd_reflectlight_components;

import com.fs.starfarer.api.Global;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class PerformanceMonitor {
  private static final Logger log = Global.getLogger(PerformanceMonitor.class);
  private static final Map<String, Long> operationStartTimes = new HashMap<>();
  private static final Map<String, Long> totalOperationTimes = new HashMap<>();
  private static final Map<String, Integer> operationCounts = new HashMap<>();
  private static float lastReportTime = 0f;
  private static final float REPORT_INTERVAL = 5.0f;
  private static boolean enabled = false;

  public static void setEnabled(boolean isEnabled) {
    enabled = isEnabled;
    if (enabled) {
      if (Global.getCombatEngine() != null) {
        reset();
      }
    }
  }

  public static void reset() {
    operationStartTimes.clear();
    totalOperationTimes.clear();
    operationCounts.clear();
    if (Global.getCombatEngine() != null) {
      lastReportTime = Global.getCombatEngine().getTotalElapsedTime(false);
    } else {
      lastReportTime = 0f;
    }
  }

  public static void startOperation(String operationName) {
    if (!enabled) return;
    operationStartTimes.put(operationName, System.nanoTime());
  }

  public static void endOperation(String operationName) {
    if (!enabled) return;
    if (!operationStartTimes.containsKey(operationName)) return;
    long startTime = operationStartTimes.get(operationName);
    long duration = System.nanoTime() - startTime;
    totalOperationTimes.put(
        operationName,
        totalOperationTimes.containsKey(operationName)
            ? totalOperationTimes.get(operationName)
            : 0L + duration);
    operationCounts.put(
        operationName,
        operationCounts.containsKey(operationName) ? operationCounts.get(operationName) : 0 + 1);
  }

  public static void reportIfNeeded() {
    if (!enabled || Global.getCombatEngine() == null) return;
    float currentTime = Global.getCombatEngine().getTotalElapsedTime(false);
    if (currentTime - lastReportTime < REPORT_INTERVAL) return;
    int shipCount = Global.getCombatEngine().getShips().size();
    StringBuilder report = new StringBuilder();
    report.append("====== PerformanceMonitor performance report ======\n");
    report.append("battlefield ship count: ").append(shipCount).append("\n");
    List<String> sortedOperations = new ArrayList<String>(totalOperationTimes.keySet());
    java.util.Collections.sort(
        sortedOperations,
        new java.util.Comparator<String>() {
          public int compare(String a, String b) {
            long avgA =
                totalOperationTimes.get(a)
                    / Math.max(1, operationCounts.containsKey(a) ? operationCounts.get(a) : 1);
            long avgB =
                totalOperationTimes.get(b)
                    / Math.max(1, operationCounts.containsKey(b) ? operationCounts.get(b) : 1);
            return (avgB < avgA) ? -1 : ((avgB == avgA) ? 0 : 1);
          }
        });
    for (String operation : sortedOperations) {
      long totalTime = totalOperationTimes.get(operation);
      int count = operationCounts.containsKey(operation) ? operationCounts.get(operation) : 0;
      if (count == 0) continue;
      double avgTimeMs = (totalTime / (double) count) / 1_000_000.0;
      report
          .append(operation)
          .append(": execution count=")
          .append(count)
          .append(", average time=")
          .append(String.format("%.3f", avgTimeMs))
          .append("ms\n");
    }
    report.append("=======================================");
    log.info(report.toString());
    reset();
  }
}
