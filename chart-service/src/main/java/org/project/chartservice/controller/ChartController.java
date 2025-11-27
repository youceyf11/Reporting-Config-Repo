package org.project.chartservice.controller;

import org.project.chartservice.client.ReportingClient;
import org.project.chartservice.dto.EmployeeMetricDto;
import org.project.chartservice.dto.WeeklySummaryDto;
import org.project.chartservice.service.ChartGenerationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charts")
public class ChartController {

  private final ReportingClient reportingClient;
  private final ChartGenerationService chartGenService;

  public ChartController(ReportingClient reportingClient,
                         ChartGenerationService chartGenService) {
    this.reportingClient = reportingClient;
    this.chartGenService = chartGenService;
  }

  // 1. Monthly Performance (Existing)
  @GetMapping("/monthly")
  public ResponseEntity<byte[]> getMonthlyChart(@RequestParam(required = false) String period) {
    if (period == null) period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));

    List<EmployeeMetricDto> metrics = reportingClient.getMonthlyMetrics(period);
    byte[] image = chartGenService.generateMonthlyBarChart(metrics, period);

    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
  }

  // 2. Weekly Trend (Addressing the Weakness)
  @GetMapping("/weekly-trend")
  public ResponseEntity<byte[]> getWeeklyChart(@RequestParam String startDate,
                                               @RequestParam String endDate) {
    // Fetch aggregated weekly data from Reporting Service
    Map<String, WeeklySummaryDto> weeklyData = reportingClient.getWeeklyBreakdown(startDate, endDate);
    byte[] image = chartGenService.generateWeeklyTrendChart(weeklyData);

    return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            // Tell browser: "Don't store this. Ask me again next time."
            // Because a Kafka event might have happened 1 second ago.
            .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
            .header(HttpHeaders.PRAGMA, "no-cache")
            .header(HttpHeaders.EXPIRES, "0")
            .body(image);
  }
}