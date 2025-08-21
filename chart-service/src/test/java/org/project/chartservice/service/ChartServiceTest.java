package org.project.chartservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.chartservice.dto.EmployeePerformanceDto;
import org.project.chartservice.enums.ChartType;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ChartServiceTest {

  @Test
  void testGenerateMonthlyChart() {
    System.out.println("=== Test : Génération du graphique mensuel ===");

    ReportingService reportingService = Mockito.mock(ReportingService.class);
    ChartGenerationService chartGenerationService = new ChartGenerationService();
    EmailService emailService = Mockito.mock(EmailService.class);

    ChartService chartService =
        new ChartService(reportingService, chartGenerationService, emailService);

    List<EmployeePerformanceDto> employees =
        List.of(
            new EmployeePerformanceDto("Alice", 100.0, 160.0),
            new EmployeePerformanceDto("Bob", 120.0, 170.0));
    System.out.println("Employés utilisés pour le reporting :");
    employees.forEach(
        e ->
            System.out.printf(
                " - %s : %.1f heures, %.1f objectifs\n",
                e.getEmployeeEmail(), e.getTotalHoursWorked(), e.getExpectedHoursThisMonth()));

    var reportingData = Mockito.mock(org.project.chartservice.dto.ReportingResultDto.class);
    Mockito.when(reportingData.getEmployees()).thenReturn(employees);

    Mockito.when(reportingService.getMonthlyReportingData(anyString()))
        .thenReturn(Mono.just(reportingData));

    Mono<byte[]> result = chartService.generateMonthlyChart("projectKey", ChartType.MONTHLY_BAR);

    StepVerifier.create(result)
        .assertNext(
            bytes -> {
              System.out.println(
                  "Résultat du graphique généré (byte[]) : " + bytes.length + " bytes");
              assertTrue(bytes.length > 0, "Chart should contain data");
              System.out.println(
                  "Vérification : Le graphique contient " + bytes.length + " bytes de données");
              saveChartForVisualization(bytes, "monthly_chart_test");
            })
        .expectComplete()
        .verify();

    System.out.println("=== Fin du test ===");
  }

  @Test
  void testGenerateWeeklyChart() {
    System.out.println("=== Test : Génération du graphique hebdomadaire ===");

    ReportingService reportingService = Mockito.mock(ReportingService.class);
    ChartGenerationService chartGenerationService = new ChartGenerationService();
    EmailService emailService = Mockito.mock(EmailService.class);

    ChartService chartService =
        new ChartService(reportingService, chartGenerationService, emailService);

    List<EmployeePerformanceDto> employees =
        List.of(
            new EmployeePerformanceDto("Alice", 90.0, 40.0),
            new EmployeePerformanceDto("Bob", 110.0, 45.0));
    System.out.println("Employés utilisés pour le reporting :");
    employees.forEach(
        e ->
            System.out.printf(
                " - %s : %.1f heures, %.1f objectifs\n",
                e.getEmployeeEmail(), e.getTotalHoursWorked(), e.getExpectedHoursThisMonth()));

    var reportingData = Mockito.mock(org.project.chartservice.dto.ReportingResultDto.class);
    Mockito.when(reportingData.getEmployees()).thenReturn(employees);

    Mockito.when(reportingService.getMonthlyReportingData(anyString()))
        .thenReturn(Mono.just(reportingData));

    Mono<byte[]> result = chartService.generateWeeklyChart("projectKey", ChartType.WEEKLY_BAR);

    StepVerifier.create(result)
        .assertNext(
            bytes -> {
              System.out.println(
                  "Résultat du graphique généré (byte[]) : " + bytes.length + " bytes");
              assertTrue(bytes.length > 0, "Chart should contain data");
              System.out.println(
                  "Vérification : Le graphique contient " + bytes.length + " bytes de données");
              saveChartForVisualization(bytes, "weekly_chart_test");
            })
        .expectComplete()
        .verify();

    System.out.println("=== Fin du test ===");
  }

  @Test
  void testGenerateComparativeChart() {
    System.out.println("=== Test : Génération du graphique comparatif ===");

    ReportingService reportingService = Mockito.mock(ReportingService.class);
    ChartGenerationService chartGenerationService = new ChartGenerationService();
    EmailService emailService = Mockito.mock(EmailService.class);

    ChartService chartService =
        new ChartService(reportingService, chartGenerationService, emailService);

    List<EmployeePerformanceDto> employees =
        List.of(
            new EmployeePerformanceDto("Alice", 95.0, 42.0),
            new EmployeePerformanceDto("Bob", 105.0, 47.0));
    System.out.println("Employés utilisés pour le reporting :");
    employees.forEach(
        e ->
            System.out.printf(
                " - %s : %.1f heures réelles, %.1f heures attendues\n",
                e.getEmployeeEmail(), e.getTotalHoursWorked(), e.getExpectedHoursThisMonth()));

    var reportingData = Mockito.mock(org.project.chartservice.dto.ReportingResultDto.class);
    Mockito.when(reportingData.getEmployees()).thenReturn(employees);

    Mockito.when(reportingService.getMonthlyReportingData(anyString()))
        .thenReturn(Mono.just(reportingData));

    Mono<byte[]> result = chartService.generateComparativeChart("projectKey");

    StepVerifier.create(result)
        .assertNext(
            bytes -> {
              System.out.println(
                  "Résultat du graphique généré (byte[]) : " + bytes.length + " bytes");
              assertTrue(bytes.length > 0, "Chart should contain data");
              System.out.println(
                  "Vérification : Le graphique comparatif contient "
                      + bytes.length
                      + " bytes de données");
              System.out.println(
                  "Ce graphique compare les heures réelles vs attendues pour Alice et Bob");
              saveChartForVisualization(bytes, "comparative_chart_test");
            })
        .expectComplete()
        .verify();

    System.out.println("=== Fin du test ===");
  }

  @Test
  void testGenerateAndEmailChart() {
    System.out.println("=== Test : Génération et envoi du graphique mensuel par email ===");

    ReportingService reportingService = Mockito.mock(ReportingService.class);
    ChartGenerationService chartGenerationService = new ChartGenerationService();
    EmailService emailService = Mockito.mock(EmailService.class);

    ChartService chartService =
        new ChartService(reportingService, chartGenerationService, emailService);

    List<EmployeePerformanceDto> employees =
        List.of(
            new EmployeePerformanceDto("Alice", 100.0, 160.0),
            new EmployeePerformanceDto("Bob", 120.0, 170.0));
    System.out.println("Employés utilisés pour le reporting :");
    employees.forEach(
        e ->
            System.out.printf(
                " - %s : %.1f heures, %.1f objectifs\n",
                e.getEmployeeEmail(), e.getTotalHoursWorked(), e.getExpectedHoursThisMonth()));

    var reportingData = Mockito.mock(org.project.chartservice.dto.ReportingResultDto.class);
    Mockito.when(reportingData.getEmployees()).thenReturn(employees);

    Mockito.when(reportingService.getMonthlyReportingData(anyString()))
        .thenReturn(Mono.just(reportingData));
    Mockito.when(
            emailService.sendChartByEmail(
                anyString(), anyString(), any(), anyString(), anyString()))
        .thenReturn(Mono.empty());

    String testEmail = "youceyfouriniche11@gmail.com";

    Mono<Void> result =
        chartService.generateAndEmailChart("projectKey", ChartType.MONTHLY_BAR, testEmail);

    StepVerifier.create(result).expectComplete().verify();

    System.out.println("Vérification de l'envoi du graphique par email :");
    Mockito.verify(emailService)
        .sendChartByEmail(
            eq(testEmail),
            contains("Monthly Hours"),
            any(byte[].class),
            eq("Monthly Hours"),
            eq("projectKey"));
    System.out.println("L'email a bien été envoyé avec le graphique généré à : " + testEmail);

    System.out.println("=== Fin du test ===");
  }

  /**
   * Helper method to save chart byte arrays as PNG files for visual inspection
   *
   * @param chartData The byte array containing the chart image data
   * @param filename The name of the file to save (without extension)
   */
  private void saveChartForVisualization(byte[] chartData, String filename) {
    try {
      Path outputDir = Paths.get("target/test-charts");
      if (!Files.exists(outputDir)) {
        Files.createDirectories(outputDir);
      }

      Path filePath = outputDir.resolve(filename + ".png");
      Files.write(filePath, chartData);
      System.out.println("Chart saved for visualization: " + filePath.toAbsolutePath());
    } catch (IOException e) {
      System.err.println("Failed to save chart for visualization: " + e.getMessage());
    }
  }
}
