package org.project.reportingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.project.reportingservice.dto.EmployeePerformanceDto;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.dto.ReportingResultDto;
import org.project.reportingservice.iservice.IJiraClient;
import org.project.reportingservice.iservice.ITimeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("ReportingService Tests")
class ReportingServiceTest {

  @Mock private IJiraClient jiraClient;

  @Mock private ITimeUtils timeUtils;

  private ReportingService reportingService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    reportingService = new ReportingService(jiraClient, timeUtils);

    // Default stubbing
    lenient()
        .when(timeUtils.getExpectedHoursForMonth(any(YearMonth.class), eq(8.0)))
        .thenReturn(160.0);
    lenient().when(timeUtils.getCurrentYearMonth()).thenReturn(YearMonth.now());
    lenient().when(timeUtils.getCurrentMonth(any(LocalDateTime.class))).thenReturn("August");
    lenient().when(timeUtils.getCurrentYear()).thenReturn("2025");
    lenient().when(timeUtils.isInCurrentMonth(any(LocalDateTime.class))).thenReturn(true);
    lenient()
        .when(timeUtils.getExpectedHoursForMonth(any(YearMonth.class), eq(8.0)))
        .thenReturn(160.0);

    // Add missing method stubs for performance calculations
    lenient()
        .when(timeUtils.calculatePerformancePercentage(anyDouble(), anyDouble()))
        .thenAnswer(
            invocation -> {
              Double actual = invocation.getArgument(0);
              Double expected = invocation.getArgument(1);
              return expected > 0 ? (actual / expected) * 100.0 : 0.0;
            });

    lenient()
        .when(
            timeUtils.determinePerformanceLevel(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
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

    lenient()
        .when(timeUtils.roundToTwoDecimals(anyDouble()))
        .thenAnswer(
            invocation -> {
              Double value = invocation.getArgument(0);
              return Math.round(value * 100.0) / 100.0;
            });

    lenient()
        .when(
            timeUtils.calculateHoursDifference(any(LocalDateTime.class), any(LocalDateTime.class)))
        .thenReturn(24.0); // Default 24 hours difference
  }

  @Nested
  @DisplayName("generateMonthlyReport() Tests")
  class GenerateMonthlyReportTests {

    @Test
    @DisplayName("Should generate report with employee rankings when issues exist")
    void generateMonthlyReport_shouldGenerateReport_whenIssuesExist() {
      // Arrange
      List<IssueSimpleDto> mockIssues =
          List.of(
              createIssue("alice@company.com", 40.0, LocalDateTime.now().minusDays(5)),
              createIssue("bob@company.com", 35.0, LocalDateTime.now().minusDays(3)),
              createIssue("alice@company.com", 25.0, LocalDateTime.now().minusDays(2)),
              createIssue(null, 20.0, LocalDateTime.now().minusDays(1)) // Should be filtered out
              );

      when(jiraClient.fetchProjectIssues("PROJ-1", 100)).thenReturn(Flux.fromIterable(mockIssues));

      // Act & Assert
      StepVerifier.create(reportingService.generateMonthlyReport("PROJ-1"))
          .assertNext(
              report -> {
                assertThat(report).isNotNull();
                assertThat(report.getProjectKey()).isEqualTo("PROJ-1");
                assertThat(report.getEmployeeRankings()).hasSize(2);

                // Verify Alice (should be ranked first with 65 hours)
                EmployeePerformanceDto alice = report.getEmployeeRankings().get(0);
                assertThat(alice.getEmployeeEmail()).isEqualTo("alice@company.com");
                assertThat(alice.getTotalHoursWorked()).isEqualTo(65.0);
                assertThat(alice.getTotalIssuesResolved()).isEqualTo(2);
                assertThat(alice.getRanking()).isEqualTo(1);

                // Verify Bob (should be ranked second with 35 hours)
                EmployeePerformanceDto bob = report.getEmployeeRankings().get(1);
                assertThat(bob.getEmployeeEmail()).isEqualTo("bob@company.com");
                assertThat(bob.getTotalHoursWorked()).isEqualTo(35.0);
                assertThat(bob.getTotalIssuesResolved()).isEqualTo(1);
                assertThat(bob.getRanking()).isEqualTo(2);
              })
          .verifyComplete();

      verify(jiraClient).fetchProjectIssues("PROJ-1", 100);
    }

    @Test
    @DisplayName("Should handle empty issues list")
    void generateMonthlyReport_shouldHandleEmptyIssues() {
      // Arrange
      when(jiraClient.fetchProjectIssues("EMPTY-PROJ", 100)).thenReturn(Flux.empty());

      // Act & Assert
      StepVerifier.create(reportingService.generateMonthlyReport("EMPTY-PROJ"))
          .assertNext(
              report -> {
                assertThat(report).isNotNull();
                assertThat(report.getProjectKey()).isEqualTo("EMPTY-PROJ");
                assertThat(report.getEmployeeRankings()).isEmpty();
                assertThat(report.getTotalEmployees()).isEqualTo(0);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should filter out issues with null assignee")
    void generateMonthlyReport_shouldFilterNullAssignees() {
      // Arrange
      List<IssueSimpleDto> issuesWithNulls =
          List.of(
              createIssue(null, 30.0, LocalDateTime.now().minusDays(5)),
              createIssue("", 25.0, LocalDateTime.now().minusDays(4)),
              createIssue("   ", 20.0, LocalDateTime.now().minusDays(3)),
              createIssue("valid@company.com", 15.0, LocalDateTime.now().minusDays(2)));

      when(jiraClient.fetchProjectIssues("PROJ-1", 100))
          .thenReturn(Flux.fromIterable(issuesWithNulls));

      // Act & Assert
      StepVerifier.create(reportingService.generateMonthlyReport("PROJ-1"))
          .assertNext(
              report -> {
                assertThat(report.getEmployeeRankings()).hasSize(1);
                assertThat(report.getEmployeeRankings().get(0).getEmployeeEmail())
                    .isEqualTo("valid@company.com");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle Jira client error")
    void generateMonthlyReport_shouldHandleJiraError() {
      // Arrange
      when(jiraClient.fetchProjectIssues("ERROR-PROJ", 100))
          .thenReturn(Flux.error(new RuntimeException("Jira API unavailable")));

      // Act & Assert
      StepVerifier.create(reportingService.generateMonthlyReport("ERROR-PROJ"))
          .expectError(RuntimeException.class)
          .verify();
    }

    @Test
    @DisplayName("Should calculate performance levels correctly")
    void generateMonthlyReport_shouldCalculatePerformanceLevels() {
      // Arrange
      List<IssueSimpleDto> issues =
          List.of(
              createIssue(
                  "excellent@company.com", 150.0, LocalDateTime.now().minusDays(5)), // 93.75%
              createIssue("good@company.com", 130.0, LocalDateTime.now().minusDays(4)), // 81.25%
              createIssue("average@company.com", 100.0, LocalDateTime.now().minusDays(3)), // 62.5%
              createIssue("poor@company.com", 60.0, LocalDateTime.now().minusDays(2)) // 37.5%
              );

      when(jiraClient.fetchProjectIssues("PROJ-1", 100)).thenReturn(Flux.fromIterable(issues));

      // Act & Assert
      StepVerifier.create(reportingService.generateMonthlyReport("PROJ-1"))
          .assertNext(
              report -> {
                List<EmployeePerformanceDto> rankings = report.getEmployeeRankings();

                // Verify performance levels are assigned correctly
                assertThat(rankings.get(0).getPerformanceLevel()).isEqualTo("EXCELLENT"); // 93.75%
                assertThat(rankings.get(1).getPerformanceLevel()).isEqualTo("GOOD"); // 81.25%
                assertThat(rankings.get(2).getPerformanceLevel()).isEqualTo("AVERAGE"); // 62.5%
                assertThat(rankings.get(3).getPerformanceLevel()).isEqualTo("POOR"); // 37.5%
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getTopActiveEmployees() Tests")
  class GetTopActiveEmployeesTests {

    @Test
    @DisplayName("Should return top N employees based on hours worked")
    void getTopActiveEmployees_shouldReturnTopN() {
      // Arrange
      List<IssueSimpleDto> mockIssues =
          List.of(
              createIssue("alice@company.com", 150.0, LocalDateTime.now().minusDays(5)),
              createIssue("bob@company.com", 130.0, LocalDateTime.now().minusDays(4)),
              createIssue("charlie@company.com", 110.0, LocalDateTime.now().minusDays(3)),
              createIssue("diana@company.com", 90.0, LocalDateTime.now().minusDays(2)));

      // Mock the generateMonthlyReport call
      when(jiraClient.fetchProjectIssues("PROJ-1", 100)).thenReturn(Flux.fromIterable(mockIssues));

      // Act & Assert
      StepVerifier.create(reportingService.getTopActiveEmployees("PROJ-1", 2))
          .assertNext(
              topEmployees -> {
                assertThat(topEmployees).hasSize(2);
                assertThat(topEmployees.get(0).getEmployeeEmail()).isEqualTo("alice@company.com");
                assertThat(topEmployees.get(1).getEmployeeEmail()).isEqualTo("bob@company.com");
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle null limit parameter")
    void getTopActiveEmployees_shouldHandleNullLimit() {
      // Arrange
      when(jiraClient.fetchProjectIssues("PROJ-1", 100)).thenReturn(Flux.empty());

      // Act & Assert
      StepVerifier.create(reportingService.getTopActiveEmployees("PROJ-1", null))
          .assertNext(
              topEmployees -> {
                assertThat(topEmployees).isEmpty();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when no employees found")
    void getTopActiveEmployees_shouldReturnEmptyList_whenNoEmployees() {
      // Arrange
      when(jiraClient.fetchProjectIssues("EMPTY-PROJ", 100)).thenReturn(Flux.empty());

      // Act & Assert
      StepVerifier.create(reportingService.getTopActiveEmployees("EMPTY-PROJ", 5))
          .assertNext(
              topEmployees -> {
                assertThat(topEmployees).isEmpty();
              })
          .verifyComplete();
    }
  }

  @Nested
  @DisplayName("getMonthlyStatistics() Tests")
  class GetMonthlyStatisticsTests {

    @Test
    @DisplayName("Should calculate total hours and employee count correctly")
    void getMonthlyStatistics_shouldCalculateCorrectly() {
      // Arrange
      List<IssueSimpleDto> issues =
          List.of(
              createIssue("alice@company.com", 80.0, LocalDateTime.now().minusDays(5)),
              createIssue("bob@company.com", 70.0, LocalDateTime.now().minusDays(4)),
              createIssue("alice@company.com", 40.0, LocalDateTime.now().minusDays(3)),
              createIssue("charlie@company.com", 60.0, LocalDateTime.now().minusDays(2)));

      when(jiraClient.fetchProjectIssues("PROJ-1", 100)).thenReturn(Flux.fromIterable(issues));

      // Act & Assert
      StepVerifier.create(reportingService.getMonthlyStatistics("PROJ-1"))
          .assertNext(
              stats -> {
                Double totalHours = stats.getT1();
                Integer employeeCount = stats.getT2();

                assertThat(totalHours).isEqualTo(250.0); // 120 + 70 + 60
                assertThat(employeeCount).isEqualTo(3); // alice, bob, charlie
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should return zero statistics when no data")
    void getMonthlyStatistics_shouldReturnZero_whenNoData() {
      // Arrange
      when(jiraClient.fetchProjectIssues("EMPTY-PROJ", 100)).thenReturn(Flux.empty());

      // Act & Assert
      StepVerifier.create(reportingService.getMonthlyStatistics("EMPTY-PROJ"))
          .assertNext(
              stats -> {
                assertThat(stats.getT1()).isEqualTo(0.0);
                assertThat(stats.getT2()).isEqualTo(0);
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle service error gracefully")
    void getMonthlyStatistics_shouldHandleError() {
      // Arrange
      when(jiraClient.fetchProjectIssues("ERROR-PROJ", 100))
          .thenReturn(Flux.error(new RuntimeException("Service unavailable")));

      // Act & Assert
      StepVerifier.create(reportingService.getMonthlyStatistics("ERROR-PROJ"))
          .expectError(RuntimeException.class)
          .verify();
    }
  }

  @Nested
  @DisplayName("Performance and Edge Case Tests")
  class PerformanceAndEdgeCaseTests {

    @Test
    @DisplayName("Should handle large number of issues efficiently")
    void shouldHandleLargeDataset() {
      // Arrange
      List<IssueSimpleDto> largeDataset = generateLargeIssueDataset(1000);

      when(jiraClient.fetchProjectIssues("LARGE-PROJ", 100))
          .thenReturn(Flux.fromIterable(largeDataset));

      // Act & Assert
      StepVerifier.create(reportingService.generateMonthlyReport("LARGE-PROJ"))
          .assertNext(
              report -> {
                assertThat(report.getEmployeeRankings()).isNotEmpty();
                assertThat(report.getEmployeeRankings().size())
                    .isLessThanOrEqualTo(100); // Assuming max 100 unique employees
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle concurrent requests")
    void shouldHandleConcurrentRequests() {
      // Arrange
      when(jiraClient.fetchProjectIssues(anyString(), anyInt()))
          .thenReturn(Flux.just(createIssue("test@company.com", 50.0, LocalDateTime.now())));

      // Act & Assert - Test multiple concurrent calls
      Mono<ReportingResultDto> request1 = reportingService.generateMonthlyReport("PROJ-1");
      Mono<ReportingResultDto> request2 = reportingService.generateMonthlyReport("PROJ-2");
      Mono<ReportingResultDto> request3 = reportingService.generateMonthlyReport("PROJ-3");

      StepVerifier.create(Mono.zip(request1, request2, request3))
          .assertNext(
              tuple -> {
                assertThat(tuple.getT1()).isNotNull();
                assertThat(tuple.getT2()).isNotNull();
                assertThat(tuple.getT3()).isNotNull();
              })
          .verifyComplete();
    }

    @Test
    @DisplayName("Should handle issues with zero hours")
    void shouldHandleZeroHourIssues() {
      // Arrange
      List<IssueSimpleDto> issuesWithZeroHours =
          List.of(
              createIssue("alice@company.com", 0.0, LocalDateTime.now().minusDays(5)),
              createIssue("bob@company.com", 40.0, LocalDateTime.now().minusDays(4)),
              createIssue("charlie@company.com", 0.0, LocalDateTime.now().minusDays(3)));

      when(jiraClient.fetchProjectIssues("PROJ-1", 100))
          .thenReturn(Flux.fromIterable(issuesWithZeroHours));

      // Act & Assert
      StepVerifier.create(reportingService.generateMonthlyReport("PROJ-1"))
          .assertNext(
              report -> {
                assertThat(report.getEmployeeRankings()).hasSize(3);

                // Bob should be ranked first with 40 hours
                EmployeePerformanceDto topEmployee = report.getEmployeeRankings().get(0);
                assertThat(topEmployee.getEmployeeEmail()).isEqualTo("bob@company.com");
                assertThat(topEmployee.getTotalHoursWorked()).isEqualTo(40.0);
              })
          .verifyComplete();
    }
  }

  // Helper methods
  private IssueSimpleDto createIssue(String assignee, Double hours, LocalDateTime resolutionDate) {
    IssueSimpleDto issue = new IssueSimpleDto();
    issue.setAssignee(assignee);
    issue.setTimeSpentSeconds(
        hours != null ? (long) (hours * 3600) : null); // Convert hours to seconds
    issue.setResolved(resolutionDate);
    issue.setIssueKey("ISSUE-" + System.nanoTime());
    return issue;
  }

  private EmployeePerformanceDto createEmployeeDto(String email, Double hours, Integer ranking) {
    EmployeePerformanceDto dto = new EmployeePerformanceDto();
    dto.setEmployeeEmail(email);
    dto.setTotalHoursWorked(hours);
    dto.setRanking(ranking);
    dto.setExpectedHoursThisMonth(160.0);
    dto.setPerformancePercentage((hours / 160.0) * 100);
    return dto;
  }

  private List<IssueSimpleDto> generateLargeIssueDataset(int size) {
    return java.util.stream.IntStream.range(0, size)
        .mapToObj(
            i ->
                createIssue(
                    "employee" + (i % 100) + "@company.com",
                    10.0 + (i % 50),
                    LocalDateTime.now().minusDays(i % 30)))
        .toList();
  }
}
