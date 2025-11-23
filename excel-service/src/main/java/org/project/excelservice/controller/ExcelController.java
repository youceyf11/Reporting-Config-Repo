package org.project.excelservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.project.excelservice.service.ExcelReportService;
import org.project.excelservice.service.ExcelSyncService;
import org.project.excelservice.service.KpiExcelReportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequestMapping("/api/excel")
public class ExcelController {

  private final ExcelSyncService excelSyncService;
  private final ExcelReportService excelReportService;
  private final KpiExcelReportService kpiExcelReportService;

  public ExcelController(ExcelSyncService excelSyncService, ExcelReportService excelReportService, 
                        KpiExcelReportService kpiExcelReportService) {
    this.excelSyncService = excelSyncService;
    this.excelReportService = excelReportService;
    this.kpiExcelReportService = kpiExcelReportService;
  }

  @GetMapping("/{projectKey}")
  public Mono<ResponseEntity<String>> sync(@PathVariable String projectKey) {
    return excelSyncService
        .sync(projectKey)
        .map(result -> ResponseEntity.ok("Sync completed successfully"))
        .onErrorResume(
            error -> {
              log.error("Error during sync: {}", error.getMessage());
              return Mono.just(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body("Failed to sync data for project: " + projectKey));
            });
  }

  @GetMapping(
      value = "/performance",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public Mono<ResponseEntity<ByteArrayResource>> download(@RequestParam String projectKey) {
    return Mono.fromSupplier(() -> excelReportService.generateEmployeeReport(projectKey))
        .map(
            bytes ->
                ResponseEntity.ok()
                    .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"performance_" + projectKey + ".xlsx\"")
                    .contentType(
                        MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(bytes));
  }

  @GetMapping(
      value = "/kpi-report",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public Mono<ResponseEntity<ByteArrayResource>> downloadKpiReport(@RequestParam String projectKey) {
    log.info("📊 Generating comprehensive KPI Excel report for project: {}", projectKey);
    
    return Mono.fromSupplier(() -> kpiExcelReportService.generateSimpleReport(projectKey))
        .map(bytes -> {
          String filename = String.format("KPI_Dashboard_%s_%s.xlsx", 
              projectKey, 
              java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
          
          return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
              .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
              .body(bytes);
        })
        .onErrorResume(error -> {
          log.error("❌ Failed to generate KPI report for project {}: {}", projectKey, error.getMessage());
          return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });
  }

  @GetMapping(
      value = "/simple-report",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public Mono<ResponseEntity<ByteArrayResource>> downloadSimpleReport(@RequestParam String projectKey) {
    log.info("Generating simple Excel report for project: {}", projectKey);
    
    return Mono.fromSupplier(() -> kpiExcelReportService.generateSimpleReport(projectKey))
        .map(bytes -> {
          String filename = String.format("Monthly_Report_%s_%s.xlsx", 
              projectKey, 
              java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
          
          return ResponseEntity.ok()
              .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
              .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
              .body(bytes);
        })
        .onErrorResume(error -> {
          log.error("Failed to generate simple report for project {}: {}", projectKey, error.getMessage());
          return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        });
  }

  @GetMapping("/health")
  public Mono<ResponseEntity<String>> health() {
    return Mono.just(ResponseEntity.ok("Excel Service is healthy! 📊✅"));
  }
}
