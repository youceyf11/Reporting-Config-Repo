package org.project.excelservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.project.excelservice.dto.ReportingDtos.EmployeePerformanceDto;
import org.project.excelservice.dto.ReportingDtos.ReportingResultDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ExcelReportServiceTest {

  @Mock private WebClient reportingClient;

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Mock
  private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

  @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

  @Mock private WebClient.ResponseSpec responseSpec;

  private ExcelReportService excelReportService;

  @BeforeEach
  void setUp() {
    excelReportService = new ExcelReportService(reportingClient);
  }

  @Test
  @DisplayName("Should generate Excel report with employee performance data")
  void generateEmployeeReport_success() {
    // Arrange
    String projectKey = "PROJ";

    EmployeePerformanceDto employee =
        EmployeePerformanceDto.builder()
            .assignee("john.doe@example.com")
            .totalHoursWorked(160.0)
            .performancePercentage(85.5)
            .performanceLevel("GOOD")
            .averageResolutionTimeHours(24.5)
            .ranking(1)
            .resolvedIssuesCount(15)
            .build();

    ReportingResultDto reportingResult =
        ReportingResultDto.builder()
            .month("August")
            .year("2025")
            .projectKey(projectKey)
            .employeeRankings(List.of(employee))
            .build();

    Map<String, Map<Integer, Double>> weeklyStats =
        Map.of(
            "john.doe@example.com",
            Map.of(
                32, 40.0,
                33, 42.0,
                34, 38.0,
                35, 40.0));

    ParameterizedTypeReference<Map<String, Map<Integer, Double>>> weeklyStatsType =
        new ParameterizedTypeReference<>() {};

    // Mock WebClient chain
    when(reportingClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
        .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

    when(responseSpec.bodyToMono(eq(ReportingResultDto.class)))
        .thenReturn(Mono.just(reportingResult));
    when(responseSpec.bodyToMono(eq(weeklyStatsType))).thenReturn(Mono.just(weeklyStats));

    // Act
    ByteArrayResource result = excelReportService.generateEmployeeReport(projectKey);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getByteArray()).isNotEmpty();
    assertThat(result.getByteArray().length).isGreaterThan(0);
  }

  @Test
  @DisplayName("Should handle empty employee rankings")
  void generateEmployeeReport_emptyRankings() {
    // Arrange
    String projectKey = "EMPTY";

    ReportingResultDto reportingResult =
        ReportingResultDto.builder()
            .month("August")
            .year("2025")
            .projectKey(projectKey)
            .employeeRankings(List.of())
            .build();

    Map<String, Map<Integer, Double>> weeklyStats = Map.of();

    ParameterizedTypeReference<Map<String, Map<Integer, Double>>> weeklyStatsType =
        new ParameterizedTypeReference<>() {};

    // Mock WebClient chain
    when(reportingClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
        .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

    when(responseSpec.bodyToMono(eq(ReportingResultDto.class)))
        .thenReturn(Mono.just(reportingResult));
    when(responseSpec.bodyToMono(eq(weeklyStatsType))).thenReturn(Mono.just(weeklyStats));

    // Act
    ByteArrayResource result = excelReportService.generateEmployeeReport(projectKey);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getByteArray()).isNotEmpty();
  }

  @Test
  @DisplayName("Should handle null employee rankings")
  void generateEmployeeReport_nullRankings() {
    // Arrange
    String projectKey = "NULL";

    ReportingResultDto reportingResult =
        ReportingResultDto.builder()
            .month("August")
            .year("2025")
            .projectKey(projectKey)
            .employeeRankings(null)
            .build();

    Map<String, Map<Integer, Double>> weeklyStats = Map.of();

    ParameterizedTypeReference<Map<String, Map<Integer, Double>>> weeklyStatsType =
        new ParameterizedTypeReference<>() {};

    // Mock WebClient chain
    when(reportingClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
        .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

    when(responseSpec.bodyToMono(eq(ReportingResultDto.class)))
        .thenReturn(Mono.just(reportingResult));
    when(responseSpec.bodyToMono(eq(weeklyStatsType))).thenReturn(Mono.just(weeklyStats));

    // Act
    ByteArrayResource result = excelReportService.generateEmployeeReport(projectKey);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.getByteArray()).isNotEmpty();
  }

  @Test
  @DisplayName("Should handle WebClient error")
  void generateEmployeeReport_webClientError() {
    // Arrange
    String projectKey = "ERROR";

    // Mock WebClient chain
    when(reportingClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(any(java.util.function.Function.class)))
        .thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

    when(responseSpec.bodyToMono(eq(ReportingResultDto.class)))
        .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

    // Act & Assert
    assertThatThrownBy(() -> excelReportService.generateEmployeeReport(projectKey))
        .isInstanceOf(RuntimeException.class);
  }
}
