package org.project.chartservice.controller;

import org.project.chartservice.enums.ChartType;
import org.project.chartservice.service.ChartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/charts")
public class ChartController {

  private final ChartService chartService;

  @Autowired
  public ChartController(ChartService chartService) {
    this.chartService = chartService;
  }

  // Endpoints standards (production)
  @GetMapping("/monthly/weekly")
  public Mono<ResponseEntity<byte[]>> getWeeklyChart(@RequestParam String projectKey) {
    return chartService
        .generateWeeklyChart(projectKey, ChartType.WEEKLY_BAR)
        .map(this::createImageResponse);
  }

  @GetMapping("/monthly/summary")
  public Mono<ResponseEntity<byte[]>> getMonthlyChart(@RequestParam String projectKey) {
    return chartService
        .generateMonthlyChart(projectKey, ChartType.MONTHLY_BAR)
        .map(this::createImageResponse);
  }

  @GetMapping("/monthly/comparative")
  public Mono<ResponseEntity<byte[]>> getComparativeChart(@RequestParam String projectKey) {
    return chartService.generateComparativeChart(projectKey).map(this::createImageResponse);
  }

  @PostMapping("/monthly/email")
  public Mono<ResponseEntity<String>> emailChart(
      @RequestParam String projectKey, @RequestParam String chartType, @RequestParam String email) {

    ChartType type = ChartType.valueOf(chartType.toUpperCase());

    return chartService
        .generateAndEmailChart(projectKey, type, email)
        .then(Mono.just(ResponseEntity.ok("Chart sent successfully to " + email)))
        .onErrorReturn(ResponseEntity.status(500).body("Failed to send chart"));
  }

  /*
  // Endpoints de test (utilisent des données fictives)
  @GetMapping("/test/weekly")
  public Mono<ResponseEntity<byte[]>> getTestWeeklyChart(@RequestParam String projectKey) {
      return chartService.generateTestChart(ChartType.WEEKLY_BAR, projectKey)
              .map(this::createImageResponse);
  }

  @GetMapping("/test/summary")
  public Mono<ResponseEntity<byte[]>> getTestMonthlyChart(@RequestParam String projectKey) {
      return chartService.generateTestChart(ChartType.MONTHLY_BAR, projectKey)
              .map(this::createImageResponse);
  }

  @GetMapping("/test/comparative")
  public Mono<ResponseEntity<byte[]>> getTestComparativeChart(@RequestParam String projectKey) {
      return chartService.generateTestChart(ChartType.COMPARATIVE, projectKey)
              .map(this::createImageResponse);
  }
  */

  private ResponseEntity<byte[]> createImageResponse(byte[] chartData) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.IMAGE_PNG);
    headers.setContentLength(chartData.length);
    headers.setCacheControl("no-cache");

    return ResponseEntity.ok().headers(headers).body(chartData);
  }
}
