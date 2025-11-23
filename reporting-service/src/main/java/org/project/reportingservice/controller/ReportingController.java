package org.project.reportingservice.controller;

import org.project.reportingservice.entity.EmployeePerformanceMetric;
import org.project.reportingservice.service.ReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

  private final ReportingService reportingService;

  public ReportingController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  @GetMapping("/performance/{period}")
  public ResponseEntity<List<EmployeePerformanceMetric>> getPerformanceByMonth(
          @PathVariable String period) {
    return ResponseEntity.ok(reportingService.getMetricsByPeriod(period));
  }

  @GetMapping("/health")
  public String health() {
    return "Reporting Service is UP";
  }
}