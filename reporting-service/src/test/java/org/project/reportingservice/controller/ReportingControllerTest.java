package org.project.reportingservice.controller;

import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.MonthlyStatsDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.dto.WeeklyStatsDto;
import org.project.reportingservice.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

@WebFluxTest(controllers = ReportingController.class)
@DisplayName("ReportingController Tests")
class ReportingControllerTest {

  @Autowired private WebTestClient webTestClient;

  @MockBean private ReportingService reportingService;

  @Nested
  @DisplayName("Monthly Report Endpoint")
  class MonthlyReportTests {

    @Test
    @DisplayName("GET /api/reporting/monthly - should return report when valid project key")
    void getMonthlyReport_shouldReturnReport_whenValidProjectKey() {
      // Arrange
      List<EmployeePerformanceDto> employees =
          List.of(
              createEmployeeDto("alice@company.com", 120.0, 8, 15.0, 75.0, "GOOD"),
              createEmployeeDto("bob@company.com", 100.0, 6, 16.7, 62.5, "AVERAGE"));
      ReportingResultDto expectedReport = new ReportingResultDto("August", employees);
      expectedReport.setProjectKey("PROJ-1");

      when(reportingService.generateMonthlyReport("PROJ-1")).thenReturn(Mono.just(expectedReport));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "PROJ-1")
                      .build())
          .accept(MediaType.APPLICATION_JSON)
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.projectKey")
          .isEqualTo("PROJ-1")
          .jsonPath("$.totalEmployees")
          .isEqualTo(2)
          .jsonPath("$.totalHoursWorked")
          .isEqualTo(220.0)
          .jsonPath("$.employeeRankings")
          .isArray()
          .jsonPath("$.employeeRankings[0].employeeEmail")
          .isEqualTo("alice@company.com")
          .jsonPath("$.employeeRankings[0].totalHoursWorked")
          .isEqualTo(120.0)
          .jsonPath("$.employeeRankings[1].employeeEmail")
          .isEqualTo("bob@company.com");
    }

    @Test
    @DisplayName("GET /api/reporting/monthly - should return empty report when no data")
    void getMonthlyReport_shouldReturnEmptyReport_whenNoData() {
      // Arrange
      when(reportingService.generateMonthlyReport("EMPTY-PROJ")).thenReturn(Mono.empty());

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "EMPTY-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.month")
          .isEqualTo("No data found")
          .jsonPath("$.year")
          .isEqualTo("")
          .jsonPath("$.employeeRankings")
          .isEmpty();
    }

    @Test
    @DisplayName("GET /api/reporting/monthly - should handle service error gracefully")
    void getMonthlyReport_shouldHandleError_whenServiceFails() {
      // Arrange
      when(reportingService.generateMonthlyReport("ERROR-PROJ"))
          .thenReturn(Mono.error(new RuntimeException("Jira service unavailable")));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "ERROR-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.month")
          .isEqualTo("Error occurred")
          .jsonPath("$.year")
          .isEqualTo("")
          .jsonPath("$.employeeRankings")
          .isEmpty();
    }

    @Test
    @DisplayName("GET /api/reporting/monthly - should return 400 when projectKey missing")
    void getMonthlyReport_shouldReturn400_whenProjectKeyMissing() {
      // Act & Assert - Now expects 400 BAD_REQUEST with validation
      webTestClient.get().uri("/api/reporting/monthly").exchange().expectStatus().isBadRequest();
    }
  }

  @Nested
  @DisplayName("Top Active Employees Endpoint")
  class TopActiveEmployeesTests {

    @Test
    @DisplayName("GET /api/reporting/monthly/top - should return top employees with default limit")
    void getTopActiveEmployees_shouldReturnTopEmployees_withDefaultLimit() {
      // Arrange
      List<EmployeePerformanceDto> topEmployees =
          List.of(
              createEmployeeDto("alice@company.com", 150.0, 10, 15.0, 93.75, "EXCELLENT"),
              createEmployeeDto("bob@company.com", 130.0, 8, 16.25, 81.25, "GOOD"));

      when(reportingService.getTopActiveEmployees("PROJ-1", 10))
          .thenReturn(Mono.just(topEmployees));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "PROJ-1")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBodyList(EmployeePerformanceDto.class)
          .hasSize(2);
    }

    @Test
    @DisplayName("GET /api/reporting/monthly/top - should respect custom limit")
    void getTopActiveEmployees_shouldRespectCustomLimit() {
      // Arrange
      List<EmployeePerformanceDto> topEmployees =
          List.of(createEmployeeDto("alice@company.com", 150.0, 10, 15.0, 93.75, "EXCELLENT"));

      when(reportingService.getTopActiveEmployees("PROJ-1", 1)).thenReturn(Mono.just(topEmployees));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "PROJ-1")
                      .queryParam("limit", "1")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBodyList(EmployeePerformanceDto.class)
          .hasSize(1);
    }

    @Test
    @DisplayName("GET /api/reporting/monthly/top - should return 404 when no employees found")
    void getTopActiveEmployees_shouldReturn404_whenNoEmployees() {
      // Arrange
      when(reportingService.getTopActiveEmployees("EMPTY-PROJ", 10)).thenReturn(Mono.empty());

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "EMPTY-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isNotFound();
    }

    @Test
    @DisplayName("GET /api/reporting/monthly/top - should return 500 when service error")
    void getTopActiveEmployees_shouldReturn500_whenServiceError() {
      // Arrange
      when(reportingService.getTopActiveEmployees("ERROR-PROJ", 10))
          .thenReturn(Mono.error(new RuntimeException("Database connection failed")));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "ERROR-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .is5xxServerError();
    }
  }

  @Nested
  @DisplayName("Monthly Statistics Endpoint")
  class MonthlyStatisticsTests {

    @Test
    @DisplayName("GET /api/reporting/monthly/stats - should return statistics successfully")
    void getMonthlyStatistics_shouldReturnStats_whenValidProject() {
      // Arrange
      when(reportingService.getMonthlyStatistics("PROJ-1"))
          .thenReturn(Mono.just(Tuples.of(250.0, 5)));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/stats")
                      .queryParam("projectKey", "PROJ-1")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.totalHoursWorked")
          .isEqualTo(250.0)
          .jsonPath("$.totalEmployees")
          .isEqualTo(5)
          .jsonPath("$.averageHoursPerEmployee")
          .isEqualTo(50.0);
    }

    @Test
    @DisplayName("GET /api/reporting/monthly/stats - should return zero stats when no data")
    void getMonthlyStatistics_shouldReturnZeroStats_whenNoData() {
      // Arrange
      when(reportingService.getMonthlyStatistics("EMPTY-PROJ"))
          .thenReturn(Mono.just(Tuples.of(0.0, 0)));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/stats")
                      .queryParam("projectKey", "EMPTY-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.totalHoursWorked")
          .isEqualTo(0.0)
          .jsonPath("$.totalEmployees")
          .isEqualTo(0)
          .jsonPath("$.averageHoursPerEmployee")
          .isEqualTo(0.0);
    }

    @Test
    @DisplayName("GET /api/reporting/monthly/stats - should handle service error")
    void getMonthlyStatistics_shouldHandleError_whenServiceFails() {
      // Arrange
      when(reportingService.getMonthlyStatistics("ERROR-PROJ"))
          .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/stats")
                      .queryParam("projectKey", "ERROR-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .is5xxServerError()
          .expectBody()
          .jsonPath("$.totalHoursWorked")
          .isEqualTo(0.0)
          .jsonPath("$.totalEmployees")
          .isEqualTo(0)
          .jsonPath("$.averageHoursPerEmployee")
          .isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("Health Check Endpoint")
  class HealthCheckTests {

    @Test
    @DisplayName("GET /api/reporting/health - should return UP status")
    void healthCheck_shouldReturnUP() {
      // Act & Assert - No mocking needed as it returns static response
      webTestClient
          .get()
          .uri("/api/reporting/health")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.status")
          .isEqualTo("UP")
          .jsonPath("$.message")
          .isEqualTo("Reporting service is healthy");
    }
  }

  @Nested
  @DisplayName("Employee Weekly Stats Endpoint")
  class EmployeeWeeklyStatsTests {

    @Test
    @DisplayName(
        "GET /api/reporting/weekly/stats/{projectKey}/{assignee} - should return weekly stats")
    void getEmployeeWeeklyStats_shouldReturnStats_whenValidEmployee() {
      // Arrange
      WeeklyStatsDto weeklyStats = new WeeklyStatsDto();
      weeklyStats.setAssignee("alice@company.com");
      weeklyStats.setTotalWeeklyHours(40.0);

      when(reportingService.getEmployeeWeeklyStats("PROJ-1", "alice@company.com"))
          .thenReturn(Mono.just(weeklyStats));

      // Act & Assert
      webTestClient
          .get()
          .uri("/api/reporting/weekly/stats/PROJ-1/alice@company.com")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(WeeklyStatsDto.class);
    }

    @Test
    @DisplayName(
        "GET /api/reporting/weekly/stats/{projectKey}/{assignee} - should return 404 when employee not found")
    void getEmployeeWeeklyStats_shouldReturn404_whenEmployeeNotFound() {
      // Arrange
      when(reportingService.getEmployeeWeeklyStats("PROJ-1", "nonexistent@company.com"))
          .thenReturn(Mono.empty());

      // Act & Assert
      webTestClient
          .get()
          .uri("/api/reporting/weekly/stats/PROJ-1/nonexistent@company.com")
          .exchange()
          .expectStatus()
          .isNotFound();
    }
  }

  @Nested
  @DisplayName("Employee Monthly Stats Endpoint")
  class EmployeeMonthlyStatsTests {

    @Test
    @DisplayName(
        "GET /api/reporting/monthly/stats/{projectKey}/{assignee} - should return monthly stats")
    void getEmployeeMonthlyStats_shouldReturnStats_whenValidEmployee() {
      // Arrange
      Map<Integer, Double> hoursByMonth = Map.of(8, 120.0); // August: 120 hours
      MonthlyStatsDto monthlyStats = new MonthlyStatsDto("alice@company.com", hoursByMonth, 160.0);

      when(reportingService.getEmployeeMonthlyStats("PROJ-1", "alice@company.com", 160.0))
          .thenReturn(Mono.just(monthlyStats));

      // Act & Assert
      webTestClient
          .get()
          .uri("/api/reporting/monthly/stats/PROJ-1/alice@company.com")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(MonthlyStatsDto.class);
    }

    @Test
    @DisplayName(
        "GET /api/reporting/monthly/stats/{projectKey}/{assignee} - should use custom expected hours")
    void getEmployeeMonthlyStats_shouldUseCustomExpectedHours() {
      // Arrange
      MonthlyStatsDto monthlyStats = new MonthlyStatsDto();
      monthlyStats.setAssignee("alice@company.com");
      monthlyStats.setHoursByMonth(Map.of(1, 120.0)); // Set monthly hours instead of weekly

      when(reportingService.getEmployeeMonthlyStats("PROJ-1", "alice@company.com", 180.0))
          .thenReturn(Mono.just(monthlyStats));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/stats/PROJ-1/alice@company.com")
                      .queryParam("expectedHours", "180.0")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(MonthlyStatsDto.class);
    }

    @Test
    @DisplayName(
        "GET /api/reporting/monthly/stats/{projectKey}/{assignee} - should return 404 when employee not found")
    void getEmployeeMonthlyStats_shouldReturn404_whenEmployeeNotFound() {
      // Arrange
      when(reportingService.getEmployeeMonthlyStats("PROJ-1", "nonexistent@company.com", 160.0))
          .thenReturn(Mono.empty());

      // Act & Assert
      webTestClient
          .get()
          .uri("/api/reporting/monthly/stats/PROJ-1/nonexistent@company.com")
          .exchange()
          .expectStatus()
          .isNotFound();
    }
  }

  @Nested
  @DisplayName("Detailed Monthly Statistics Endpoint")
  class DetailedMonthlyStatisticsTests {

    @Test
    @DisplayName("GET /api/reporting/monthly/detailed/{projectKey} - should return detailed stats")
    void getDetailedMonthlyStatistics_shouldReturnDetailedStats() {
      // Arrange
      Map<String, Map<Integer, Double>> detailedStats =
          Map.of(
              "alice@company.com", Map.of(1, 40.0, 2, 35.0, 3, 45.0),
              "bob@company.com", Map.of(1, 30.0, 2, 25.0, 3, 35.0));

      when(reportingService.getDetailedMonthlyStatistics("PROJ-1"))
          .thenReturn(Mono.just(detailedStats));

      // Act & Assert
      webTestClient
          .get()
          .uri("/api/reporting/monthly/detailed/PROJ-1")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$['alice@company.com']['1']")
          .isEqualTo(40.0)
          .jsonPath("$['bob@company.com']['1']")
          .isEqualTo(30.0);
    }
  }

  @Nested
  @DisplayName("Performance Tests")
  class PerformanceTests {

    @Test
    @DisplayName("Should handle large employee list efficiently")
    void shouldHandleLargeEmployeeList() {
      // Arrange
      List<EmployeePerformanceDto> largeList = generateLargeEmployeeList(100);
      ReportingResultDto largeReport = new ReportingResultDto("LARGE-PROJ", largeList);

      when(reportingService.generateMonthlyReport("LARGE-PROJ")).thenReturn(Mono.just(largeReport));

      // Act & Assert
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "LARGE-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody()
          .jsonPath("$.employeeRankings")
          .isArray()
          .jsonPath("$.employeeRankings.length()")
          .isEqualTo(100);
    }
  }

  // Helper methods
  private EmployeePerformanceDto createEmployeeDto(
      String email, Double hours, Integer issues, Double avgTime, Double percentage, String level) {
    EmployeePerformanceDto dto = new EmployeePerformanceDto();
    dto.setEmployeeEmail(email);
    dto.setTotalHoursWorked(hours);
    dto.setTotalIssuesResolved(issues);
    dto.setAverageResolutionTimeHours(avgTime);
    dto.setPerformancePercentage(percentage);
    dto.setPerformanceLevel(level);
    dto.setExpectedHoursThisMonth(160.0);
    return dto;
  }

  private List<EmployeePerformanceDto> generateLargeEmployeeList(int size) {
    return java.util.stream.IntStream.range(0, size)
        .mapToObj(
            i ->
                createEmployeeDto(
                    "employee" + i + "@company.com",
                    100.0 + i,
                    5 + (i % 10),
                    15.0 + (i % 5),
                    60.0 + (i % 40),
                    i % 2 == 0 ? "GOOD" : "AVERAGE"))
        .toList();
  }
}
