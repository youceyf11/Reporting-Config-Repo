package org.project.chartservice.service;

import org.project.chartservice.IService.IChartService;
import org.project.chartservice.enums.ChartType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ChartService implements IChartService {

  private final ReportingService reportingService;
  private final ChartGenerationService chartGenerationService;
  private final EmailService emailService;

  @Autowired
  public ChartService(
      ReportingService reportingService,
      ChartGenerationService chartGenerationService,
      EmailService emailService) {
    this.reportingService = reportingService;
    this.chartGenerationService = chartGenerationService;
    this.emailService = emailService;
  }

  /*
  public Mono<byte[]> generateTestChart(ChartType chartType, String projectKey) {
      return chartGenerationService.generateChart(getTestEmployees(), chartType, projectKey);
  }

  private List<EmployeePerformanceDto> getTestEmployees() {
      List<EmployeePerformanceDto> employees = new ArrayList<>();
      employees.add(new EmployeePerformanceDto("Alice", 100.0, 160.0));
      employees.add(new EmployeePerformanceDto("Bob", 130.0, 160.0));
      return employees;
  } */

  /**
   * Generates a monthly chart based on the project key and chart type.
   *
   * @param projectKey The key of the project for which the chart is generated.
   * @param chartType The type of chart to generate.
   * @return A Mono containing the byte array of the generated chart image.
   */
  @Override
  public Mono<byte[]> generateMonthlyChart(String projectKey, ChartType chartType) {
    return reportingService
        .getMonthlyReportingData(projectKey)
        .flatMap(
            reportingData ->
                chartGenerationService.generateChart(
                    reportingData.getEmployees(), chartType, projectKey));
  }

  @Override
  public Mono<byte[]> generateWeeklyChart(String projectKey, ChartType chartType) {
    return reportingService
        .getMonthlyReportingData(projectKey)
        .flatMap(
            reportingData ->
                chartGenerationService.generateChart(
                    reportingData.getEmployees(), chartType, projectKey));
  }

  @Override
  public Mono<byte[]> generateComparativeChart(String projectKey) {
    return reportingService
        .getMonthlyReportingData(projectKey)
        .flatMap(
            reportingData ->
                chartGenerationService.generateChart(
                    reportingData.getEmployees(), ChartType.COMPARATIVE, projectKey));
  }

  /**
   * Generates a chart based on the project key and chart type, then sends it via email.
   *
   * @param projectKey The key of the project for which the chart is generated.
   * @param chartType The type of chart to generate.
   * @param toEmail The email address to send the chart to.
   * @return A Mono that completes when the email is sent.
   */
  @Override
  public Mono<Void> generateAndEmailChart(String projectKey, ChartType chartType, String toEmail) {
    return generateMonthlyChart(projectKey, chartType)
        .flatMap(
            chartData -> {
              String chartName = getChartDisplayName(chartType);
              String subject = String.format("%s Chart - Project %s", chartName, projectKey);
              return emailService.sendChartByEmail(
                  toEmail, subject, chartData, chartName, projectKey);
            });
  }

  private String getChartDisplayName(ChartType chartType) {
    switch (chartType) {
      case WEEKLY_BAR:
        return "Weekly Hours";
      case MONTHLY_BAR:
        return "Monthly Hours";
      case COMPARATIVE:
        return "Comparative Analysis";
      default:
        return "Chart";
    }
  }
}
