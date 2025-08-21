package org.project.reportingservice.controller;

import java.util.List;
import java.util.Map;
import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.MonthlyStatsDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.WeeklyStatsDto;
import org.project.reportingservice.response.HealthResponse;
import org.project.reportingservice.response.MonthlyStatsResponse;
import org.project.reportingservice.service.ReportingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * Contrôleur REST pour les endpoIntegers de reporting Tous les endpoIntegers sont réactifs
 * (retournent Mono/Flux)
 */
@RestController
@RequestMapping("/api/reporting")
public class ReportingController {

  private final ReportingService reportingService;

  private static final Logger log = LoggerFactory.getLogger(ReportingController.class);

  public ReportingController(ReportingService reportingService) {
    this.reportingService = reportingService;
  }

  /**
   * EndpoInteger principal - Génère le rapport mensuel complet
   *
   * @param projectKey clé du projet JIRA (paramètre requis)
   * @return Mono contenant le rapport complet
   */
  @GetMapping("/monthly")
  public Mono<ResponseEntity<ReportingResultDto>> getMonthlyReport(
      @RequestParam String projectKey) {

    // Validate project key
    if (projectKey == null || projectKey.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    return reportingService
        .generateMonthlyReport(projectKey)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(
            ResponseEntity.ok(new ReportingResultDto("No data found", "", projectKey, List.of())))
        .onErrorResume(
            error -> {
              error.printStackTrace();
              // Return an empty ReportingResultDto with a message
              ReportingResultDto emptyResult =
                  new ReportingResultDto("Error occurred", "", projectKey, List.of());
              return Mono.just(ResponseEntity.ok(emptyResult));
            });
  }

  /**
   * EndpoInteger pour obtenir le top N des employés les plus actifs
   *
   * @param projectKey clé du projet JIRA
   * @param limit nombre d'employés à retourner (défaut: 10)
   * @return Mono contenant la liste des top employés
   */
  @GetMapping("/monthly/top")
  public Mono<ResponseEntity<List<EmployeePerformanceDto>>> getTopActiveEmployees(
      @RequestParam String projectKey,
      @RequestParam(name = "limit", defaultValue = "10") String limitStr) {

    // 1. Parse and validate limit parameter
    Integer limit;
    try {
      limit = Integer.parseInt(limitStr);
    } catch (NumberFormatException ex) {
      // Non-numeric limit ⇒ 400 BAD_REQUEST
      return Mono.just(ResponseEntity.badRequest().build());
    }

    if (limit < 0) {
      // Negative limit ⇒ 400 BAD_REQUEST
      return Mono.just(ResponseEntity.badRequest().build());
    }

    // 2. Fast-path: limit == 0 ⇒ return empty list with 200 OK
    if (limit == 0) {
      return Mono.just(ResponseEntity.ok(List.of()));
    }

    // 3. Delegate to service
    return reportingService
        .getTopActiveEmployees(projectKey, limit)
        // Success → 200 OK
        .map(ResponseEntity::ok)
        // No employees → 404 NOT_FOUND
        .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
        // Service error → 500 INTERNAL_SERVER_ERROR
        .onErrorResume(err -> Mono.just(ResponseEntity.status(500).build()));
  }

  /**
   * EndpoInteger pour obtenir les statistiques globales du mois
   *
   * @param projectKey clé du projet JIRA
   * @return Mono contenant les statistiques (total heures, nombre employés)
   */
  /*  @GetMapping("/monthly/stats")
  public Mono<ResponseEntity<MonthlyStatsResponse>> getMonthlyStatistics(
          @RequestParam String projectKey) {
      return reportingService.getMonthlyStatistics(projectKey)
              .map(this::createStatsResponse)
              .map(ResponseEntity::ok)
              .defaultIfEmpty(ResponseEntity.ok(new MonthlyStatsResponse(0.0, 0, 0.0)))
              .onErrorResume(error -> {
                  log.error("Erreur interne:", error);
                  return Mono.just(ResponseEntity.status(500)
                          .body(new MonthlyStatsResponse(0.0, 0, 0.0)));

              });

  } */
  /* @GetMapping("/monthly/stats")
  public Mono<ResponseEntity<MonthlyStatsResponse>> getMonthlyStatistics(
          @RequestParam String projectKey) {

      return reportingService.getMonthlyStatistics(projectKey)
              .map(this::createStatsResponse)
              .map(ResponseEntity::ok)
              .onErrorResume(error -> {
                  log.error("Erreur lors de la génération des statistiques pour le projet {}: {}",
                          projectKey, error.getMessage(), error);

                  // Réponse d'erreur avec des valeurs par défaut correctes
                  MonthlyStatsResponse errorResponse = new MonthlyStatsResponse(0.0, 0, 0.0);
                  return Mono.just(ResponseEntity.status(500).body(errorResponse));
              })
              .defaultIfEmpty(ResponseEntity.ok(new MonthlyStatsResponse(0.0, 0, 0.0)));
  } */

  @GetMapping("/monthly/stats")
  public Mono<ResponseEntity<MonthlyStatsResponse>> getMonthlyStatistics(
      @RequestParam String projectKey) {

    // Validate project key
    if (projectKey == null || projectKey.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    return reportingService
        .getMonthlyStatistics(projectKey)
        .map(this::createStatsResponse)
        .map(ResponseEntity::ok)
        .onErrorResume(
            error -> {
              log.error(
                  "Error generating monthly statistics for project {}: {}",
                  projectKey,
                  error.getMessage(),
                  error);
              MonthlyStatsResponse errorResponse = new MonthlyStatsResponse(0.0, 0, 0.0);
              return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
  }

  /* @GetMapping("/test-response")
  public Mono<ResponseEntity<MonthlyStatsResponse>> testResponse() {
      MonthlyStatsResponse response = new MonthlyStatsResponse(10.0, 2, 5.0);
      return Mono.just(ResponseEntity.ok(response));
  } */

  /**
   * EndpoInteger pour vérifier la santé du service
   *
   * @return Mono contenant le statut du service
   */
  @GetMapping("/health")
  public Mono<ResponseEntity<HealthResponse>> healthCheck() {
    return Mono.just(ResponseEntity.ok(new HealthResponse("UP", "Reporting service is healthy")));
  }

  /** Get weekly statistics for a specific employee */
  @GetMapping("/weekly/stats/{projectKey}/{assignee}")
  public Mono<ResponseEntity<WeeklyStatsDto>> getEmployeeWeeklyStats(
      @PathVariable String projectKey, @PathVariable String assignee) {

    // Validate project key
    if (projectKey == null || projectKey.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    // Validate assignee
    if (assignee == null || assignee.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    return reportingService
        .getEmployeeWeeklyStats(projectKey, assignee)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /** Get monthly statistics for a specific employee */
  @GetMapping("/monthly/stats/{projectKey}/{assignee}")
  public Mono<ResponseEntity<MonthlyStatsDto>> getEmployeeMonthlyStats(
      @PathVariable String projectKey,
      @PathVariable String assignee,
      @RequestParam(defaultValue = "160.0") Double expectedHours) {

    // Validate project key
    if (projectKey == null || projectKey.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    // Validate assignee
    if (assignee == null || assignee.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    // Validate expected hours
    if (expectedHours < 0) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    return reportingService
        .getEmployeeMonthlyStats(projectKey, assignee, expectedHours)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /**
   * Get weekly statistics for all employees in a project. Mirrors
   * ReportingService#getWeeklyStatistics so that Excel-Service can consume the endpoint
   * `/api/reporting/weekly/stats?projectKey=XYZ`.
   *
   * @param projectKey Jira project key
   * @return map: employeeEmail -> { weekNumber -> hoursWorked }
   */
  @GetMapping("/weekly/stats")
  public Mono<ResponseEntity<Map<String, Map<Integer, Double>>>> getWeeklyStatistics(
      @RequestParam String projectKey) {

    // basic validation
    if (projectKey == null || projectKey.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }

    return reportingService
        .getWeeklyStatistics(projectKey)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  @GetMapping("/monthly/detailed/{projectKey}")
  public Mono<ResponseEntity<Map<String, Map<Integer, Double>>>> getDetailedMonthlyStatistics(
      @PathVariable String projectKey) {
    // Validate project key
    if (projectKey == null || projectKey.trim().isEmpty()) {
      return Mono.just(ResponseEntity.badRequest().build());
    }
    return reportingService
        .getDetailedMonthlyStatistics(projectKey)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }

  /**
   * Crée la réponse des statistiques à partir du tuple
   *
   * @param stats tuple contenant (total heures, nombre employés)
   * @return objet de réponse des statistiques
   */
  /* private MonthlyStatsResponse createStatsResponse(Tuple2<Double, Integer> stats) {
      // Ajoutez une vérification null
      if (stats == null) {
          return new MonthlyStatsResponse(0.0, 0, 0.0);
      }

      Double totalHours = stats.getT1() != null ? stats.getT1() : 0.0;
      Integer employeeCount = stats.getT2() != null ? stats.getT2() : 0;
      Double average = employeeCount > 0 ? totalHours / employeeCount : 0.0;

      return new MonthlyStatsResponse(totalHours, employeeCount, average);
  } */
  private MonthlyStatsResponse createStatsResponse(Tuple2<Double, Integer> stats) {
    try {
      Double totalHours = stats.getT1() != null ? stats.getT1() : 0.0;
      Integer employeeCount = stats.getT2() != null ? stats.getT2() : 0;
      Double averagePerEmployee = employeeCount > 0 ? totalHours / employeeCount : 0.0;

      return new MonthlyStatsResponse(totalHours, employeeCount, averagePerEmployee);
    } catch (Exception e) {
      log.error("Erreur lors de la création de la réponse des statistiques:", e);
      return new MonthlyStatsResponse(0.0, 0, 0.0);
    }
  }
}
