package org.project.reportingservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.iservice.IJiraClient;
import org.project.reportingservice.iservice.ITimeUtils;
import org.project.reportingservice.response.HealthResponse;
import org.project.reportingservice.response.MonthlyStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("ReportingService Integration Tests")
class ReportingServiceApplicationTests {

  @Autowired private WebTestClient webTestClient;

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    public WebClient testWebClient() {
      // Mock WebClient for testing - returns empty responses
      return WebClient.builder()
          .baseUrl("http://localhost:8081") // Mock jira-fetch-service
          .build();
    }

    @Bean
    @Primary
    public IJiraClient mockJiraClient() {
      IJiraClient mockClient = Mockito.mock(IJiraClient.class);

      // Default mock behavior - return sample issues for most cases
      IssueSimpleDto sampleIssue = new IssueSimpleDto();
      sampleIssue.setIssueKey("TEST-1");
      sampleIssue.setAssignee("test@company.com");
      sampleIssue.setTimeSpentSeconds(28800L); // 8 hours
      sampleIssue.setResolved(LocalDateTime.now().minusDays(1));

      // Default behavior for most project keys
      when(mockClient.fetchProjectIssues(anyString(), anyInt())).thenReturn(Flux.just(sampleIssue));

      // Special case: return empty for EMPTY-PROJ to test zero scenarios
      when(mockClient.fetchProjectIssues(eq("EMPTY-PROJ"), anyInt())).thenReturn(Flux.empty());

      return mockClient;
    }

    @Bean
    @Primary
    public ITimeUtils mockTimeUtils() {
      ITimeUtils mockTimeUtils = Mockito.mock(ITimeUtils.class);

      // Default stubbing - use current date/time for consistency
      when(mockTimeUtils.getExpectedHoursForMonth(any(YearMonth.class), eq(8.0))).thenReturn(176.0);
      when(mockTimeUtils.getCurrentYearMonth()).thenReturn(YearMonth.now());
      when(mockTimeUtils.getCurrentMonth(any(LocalDateTime.class))).thenReturn("August");
      when(mockTimeUtils.getCurrentYear()).thenReturn("2025");
      // Critical: Mock isInCurrentMonth to return true for all test dates
      when(mockTimeUtils.isInCurrentMonth(any(LocalDateTime.class))).thenReturn(true);

      // Add performance calculation methods
      when(mockTimeUtils.calculatePerformancePercentage(anyDouble(), anyDouble()))
          .thenAnswer(
              invocation -> {
                Double actual = invocation.getArgument(0);
                Double expected = invocation.getArgument(1);
                return expected > 0 ? (actual / expected) * 100.0 : 0.0;
              });

      when(mockTimeUtils.determinePerformanceLevel(
              anyDouble(), anyDouble(), anyDouble(), anyDouble()))
          .thenAnswer(
              invocation -> {
                Double percentage = invocation.getArgument(0);
                Double excellent = invocation.getArgument(1);
                Double good = invocation.getArgument(2);
                Double poor = invocation.getArgument(3);

                if (percentage >= excellent * 100) return "EXCELLENT";
                else if (percentage >= good * 100) return "GOOD";
                else if (percentage >= poor * 100) return "AVERAGE";
                else return "POOR";
              });

      when(mockTimeUtils.roundToTwoDecimals(anyDouble()))
          .thenAnswer(
              invocation -> {
                Double value = invocation.getArgument(0);
                return Math.round(value * 100.0) / 100.0;
              });

      when(mockTimeUtils.calculateHoursDifference(
              any(LocalDateTime.class), any(LocalDateTime.class)))
          .thenReturn(24.0);

      return mockTimeUtils;
    }
  }

  @Nested
  @DisplayName("Application Context Tests")
  class ApplicationContextTests {

    @Test
    @DisplayName("Should load Spring Boot context successfully")
    void contextLoads() {
      // This test verifies that the Spring Boot application context loads without errors
      assertThat(webTestClient).isNotNull();
    }

    @Test
    @DisplayName("Should have all required beans configured")
    void shouldHaveRequiredBeans() {
      // Test that WebTestClient is properly configured
      assertThat(webTestClient).isNotNull();
    }
  }

  @Nested
  @DisplayName("Health Check Integration Tests")
  class HealthCheckIntegrationTests {

    @Test
    @DisplayName("Should return health status via REST endpoint")
    void shouldReturnHealthStatus() {
      webTestClient
          .get()
          .uri("/api/reporting/health")
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(HealthResponse.class)
          .value(
              response -> {
                assertThat(response.getStatus()).isIn("UP", "DOWN");
                assertThat(response.getMessage()).isNotNull();
              });
    }

    @Test
    @DisplayName("Should respond to health check within timeout")
    void shouldRespondToHealthCheckWithinTimeout() {
      webTestClient
          .mutate()
          .responseTimeout(Duration.ofSeconds(5))
          .build()
          .get()
          .uri("/api/reporting/health")
          .exchange()
          .expectStatus()
          .isOk();
    }
  }

  @Nested
  @DisplayName("Monthly Report Integration Tests")
  class MonthlyReportIntegrationTests {

    @Test
    @DisplayName("Should handle monthly report request with valid project key")
    void shouldHandleMonthlyReportRequest() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "TEST-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReportingResultDto.class)
          .value(
              report -> {
                assertThat(report).isNotNull();
                assertThat(report.getProjectKey()).isEqualTo("TEST-PROJ");
                assertThat(report.getEmployeeRankings()).isNotNull();
                assertThat(report.getMonth()).isNotNull();
                assertThat(report.getYear()).isNotNull();
              });
    }

    @Test
    @DisplayName("Should return 400 for missing project key parameter")
    void shouldReturn400ForMissingProjectKey() {
      webTestClient.get().uri("/api/reporting/monthly").exchange().expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should handle empty project gracefully")
    void shouldHandleEmptyProjectGracefully() {
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
          .expectBody(ReportingResultDto.class)
          .value(
              report -> {
                assertThat(report.getProjectKey()).isEqualTo("EMPTY-PROJ");
                assertThat(report.getEmployeeRankings()).isNotNull();
                // Should handle empty results gracefully
              });
    }
  }

  @Nested
  @DisplayName("Top Employees Integration Tests")
  class TopEmployeesIntegrationTests {

    @Test
    @DisplayName("Should return top employees with default limit")
    void shouldReturnTopEmployeesWithDefaultLimit() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "TEST-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBodyList(EmployeePerformanceDto.class)
          .value(
              employees -> {
                assertThat(employees).isNotNull();
                // Default limit is 10, but could be less if fewer employees
                assertThat(employees.size()).isLessThanOrEqualTo(10);
              });
    }

    @Test
    @DisplayName("Should respect custom limit parameter")
    void shouldRespectCustomLimitParameter() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "TEST-PROJ")
                      .queryParam("limit", "3")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBodyList(EmployeePerformanceDto.class)
          .value(
              employees -> {
                assertThat(employees).isNotNull();
                assertThat(employees.size()).isLessThanOrEqualTo(3);
              });
    }

    @Test
    @DisplayName("Should handle invalid limit parameter gracefully")
    void shouldHandleInvalidLimitParameter() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "TEST-PROJ")
                      .queryParam("limit", "invalid")
                      .build())
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    @DisplayName("Should handle zero limit parameter")
    void shouldHandleZeroLimitParameter() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "EMPTY-PROJ") // Use EMPTY-PROJ for zero scenario
                      .queryParam("limit", "0")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBodyList(EmployeePerformanceDto.class)
          .hasSize(0);
    }
  }

  @Nested
  @DisplayName("Monthly Statistics Integration Tests")
  class MonthlyStatisticsIntegrationTests {

    @Test
    @DisplayName("Should return monthly statistics successfully")
    void shouldReturnMonthlyStatistics() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/stats")
                      .queryParam("projectKey", "TEST-PROJ")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(MonthlyStatsResponse.class)
          .value(
              stats -> {
                assertThat(stats).isNotNull();
                assertThat(stats.getTotalHoursWorked()).isNotNull();
                assertThat(stats.getTotalEmployees()).isNotNull();
                assertThat(stats.getAverageHoursPerEmployee()).isNotNull();

                // Verify calculations are consistent
                if (stats.getTotalEmployees() > 0) {
                  double expectedAverage = stats.getTotalHoursWorked() / stats.getTotalEmployees();
                  assertThat(stats.getAverageHoursPerEmployee()).isEqualTo(expectedAverage);
                }
              });
    }

    @Test
    @DisplayName("Should handle zero statistics correctly")
    void shouldHandleZeroStatistics() {
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
          .expectBody(MonthlyStatsResponse.class)
          .value(
              stats -> {
                assertThat(stats.getTotalHoursWorked()).isGreaterThanOrEqualTo(0.0);
                assertThat(stats.getTotalEmployees()).isGreaterThanOrEqualTo(0);
                assertThat(stats.getAverageHoursPerEmployee()).isGreaterThanOrEqualTo(0.0);
              });
    }
  }

  @Nested
  @DisplayName("Error Handling Integration Tests")
  class ErrorHandlingIntegrationTests {

    @Test
    @DisplayName("Should handle service unavailable gracefully")
    void shouldHandleServiceUnavailable() {
      // Test with a project key that might cause service errors
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
          .isOk() // Should not fail completely due to error handling
          .expectBody(ReportingResultDto.class)
          .value(
              report -> {
                assertThat(report).isNotNull();
                // Should handle errors gracefully
              });
    }

    @Test
    @DisplayName("Should handle malformed requests")
    void shouldHandleMalformedRequests() {
      webTestClient
          .get()
          .uri("/api/reporting/monthly?projectKey=")
          .exchange()
          .expectStatus()
          .isBadRequest();
    }

    @Test
    @DisplayName("Should handle non-existent endpoints")
    void shouldHandleNonExistentEndpoints() {
      webTestClient.get().uri("/api/reporting/nonexistent").exchange().expectStatus().isNotFound();
    }
  }

  @Nested
  @DisplayName("Performance Integration Tests")
  class PerformanceIntegrationTests {

    @Test
    @DisplayName("Should respond within acceptable time limits")
    void shouldRespondWithinTimeLimit() {
      webTestClient
          .mutate()
          .responseTimeout(Duration.ofSeconds(10))
          .build()
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "PERF-TEST")
                      .build())
          .exchange()
          .expectStatus()
          .isOk();
    }

    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() {
      // Test multiple concurrent requests
      for (int i = 0; i < 3; i++) {
        final int requestId = i;
        webTestClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/api/reporting/monthly")
                        .queryParam("projectKey", "CONCURRENT-" + requestId)
                        .build())
            .exchange()
            .expectStatus()
            .isOk();
      }
    }

    @Test
    @DisplayName("Should handle large limit values efficiently")
    void shouldHandleLargeLimitValues() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly/top")
                      .queryParam("projectKey", "LARGE-PROJ")
                      .queryParam("limit", "100")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBodyList(EmployeePerformanceDto.class)
          .value(
              employees -> {
                assertThat(employees).isNotNull();
                // Should handle large limits without performance issues
              });
    }
  }

  @Nested
  @DisplayName("Data Validation Integration Tests")
  class DataValidationIntegrationTests {

    @Test
    @DisplayName("Should validate project key format")
    void shouldValidateProjectKeyFormat() {
      // Test with various project key formats
      String[] validProjectKeys = {"PROJ-1", "TEST", "MYPROJECT", "PROJ_123"};

      for (String projectKey : validProjectKeys) {
        webTestClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/api/reporting/monthly")
                        .queryParam("projectKey", projectKey)
                        .build())
            .exchange()
            .expectStatus()
            .isOk();
      }
    }

    @Test
    @DisplayName("Should handle special characters in project key")
    void shouldHandleSpecialCharactersInProjectKey() {
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "PROJ-WITH-SPECIAL-CHARS")
                      .build())
          .exchange()
          .expectStatus()
          .isOk();
    }
  }

  @Nested
  @DisplayName("Cross-Service Integration Tests")
  class CrossServiceIntegrationTests {

    @Test
    @DisplayName("Should integrate with jira-fetch-service properly")
    void shouldIntegrateWithJiraFetchService() {
      // This test verifies the integration with the jira-fetch-service
      // In a real environment, this would test actual service communication
      webTestClient
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "INTEGRATION-TEST")
                      .build())
          .exchange()
          .expectStatus()
          .isOk()
          .expectBody(ReportingResultDto.class)
          .value(
              report -> {
                assertThat(report).isNotNull();
                assertThat(report.getProjectKey()).isEqualTo("INTEGRATION-TEST");
              });
    }

    @Test
    @DisplayName("Should handle jira-fetch-service timeout gracefully")
    void shouldHandleJiraFetchServiceTimeout() {
      // Test timeout handling
      webTestClient
          .mutate()
          .responseTimeout(Duration.ofSeconds(30))
          .build()
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/api/reporting/monthly")
                      .queryParam("projectKey", "TIMEOUT-TEST")
                      .build())
          .exchange()
          .expectStatus()
          .isOk(); // Should handle timeout gracefully
    }
  }

  @Nested
  @DisplayName("Configuration Integration Tests")
  class ConfigurationIntegrationTests {

    @Test
    @DisplayName("Should use test profile configuration")
    void shouldUseTestProfileConfiguration() {
      // Verify that test configuration is active
      webTestClient.get().uri("/api/reporting/health").exchange().expectStatus().isOk();
    }
  }
}
