package org.project.reportingservice.controller;

import org.project.reportingservice.dto.WeeklyStatsDto;
import org.project.reportingservice.dto.WeeklySummaryDto;
import org.project.reportingservice.entity.EmployeePerformanceMetric;
import org.project.reportingservice.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

  private final ReportingService reportingService;

  public ReportingController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  // 1. MONTHLY INDIVIDUAL METRICS (Used for Monthly Bar Charts)
  // URL: /api/reports/performance/2025-11
  @GetMapping("/performance/{period}")
  public ResponseEntity<List<EmployeePerformanceMetric>> getMonthlyIndividualMetrics(@PathVariable String period) {
    return ResponseEntity.ok(reportingService.getMetricsByPeriod(period));
  }

  // 2. MONTHLY TEAM METRICS (Used for Team KPIs)
  // URL: /api/reports/performance/2025-11/team
  @GetMapping("/performance/{period}/team")
  public ResponseEntity<WeeklyStatsDto> getMonthlyTeamMetrics(@PathVariable String period) {
    return ResponseEntity.ok(reportingService.getMonthlyTeamStats(period));
  }

  // 3. WEEKLY DETAILED ANALYSIS (Used for Weekly Trend Lines)
  // URL: /api/reports/weekly-detailed?startDate=2025-11-01&endDate=2025-11-30
  // Returns both Team Totals AND Employee Breakdown per week
  @GetMapping("/weekly-detailed")
  public ResponseEntity<Map<String, WeeklySummaryDto>> getWeeklyDetailedAnalysis(
          @RequestParam String startDate,
          @RequestParam String endDate) {
    return ResponseEntity.ok(reportingService.getDetailedWeeklyAnalysis(startDate, endDate));
  }

  @GetMapping("/health")
  public String health() {
    return "Reporting Service is UP";
  }
}