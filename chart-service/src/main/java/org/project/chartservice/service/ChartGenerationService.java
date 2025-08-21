package org.project.chartservice.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.style.Styler;
import org.project.chartservice.IService.IChartGenerationService;
import org.project.chartservice.dto.EmployeePerformanceDto;
import org.project.chartservice.enums.ChartType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChartGenerationService implements IChartGenerationService {

  @Override
  public Mono<byte[]> generateChart(
      List<EmployeePerformanceDto> employees, ChartType chartType, String projectKey) {
    return Mono.fromCallable(
            () -> {
              switch (chartType) {
                case WEEKLY_BAR:
                  return generateWeeklyBarChart(employees, projectKey);
                case MONTHLY_BAR:
                  return generateMonthlyBarChart(employees, projectKey);
                case COMPARATIVE:
                  return generateComparativeChart(employees, projectKey);
                default:
                  throw new IllegalArgumentException("Unsupported chart type: " + chartType);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private byte[] generateWeeklyBarChart(List<EmployeePerformanceDto> employees, String projectKey)
      throws IOException {
    if (employees == null) {
      throw new IllegalArgumentException("La liste des employés est nulle");
    }
    CategoryChart chart =
        new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title("Weekly Hours Worked - Project: " + projectKey)
            .xAxisTitle("Week")
            .yAxisTitle("Hours Worked")
            .build();

    chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
    // chart.getStyler().setAnnotationsVisible(true);

    // Simulate weekly data (in real scenario, you'd get this from the reporting service)
    List<String> weeks = Arrays.asList("Week 1", "Week 2", "Week 3", "Week 4");

    employees.forEach(
        employee -> {
          // Simulate weekly breakdown (distribute monthly hours across weeks)
          double totalHours =
              employee.getTotalHoursWorked() != null ? employee.getTotalHoursWorked() : 0;
          double[] weeklyHours = {
            totalHours * 0.25, totalHours * 0.20, totalHours * 0.30, totalHours * 0.25
          };
          List<Double> weeklyHoursList =
              Arrays.stream(weeklyHours).boxed().collect(Collectors.toList());

          chart.addSeries(employee.getEmployeeEmail().split("@")[0], weeks, weeklyHoursList);
        });

    return chartToByteArray(chart);
  }

  private byte[] generateMonthlyBarChart(List<EmployeePerformanceDto> employees, String projectKey)
      throws IOException {
    if (employees == null) {
      throw new IllegalArgumentException("La liste des employés est nulle");
    }
    CategoryChart chart =
        new CategoryChartBuilder()
            .width(800)
            .height(600)
            .title("Monthly Hours Worked - Project: " + projectKey)
            .xAxisTitle("Employee")
            .yAxisTitle("Hours Worked")
            .build();

    chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
    // chart.getStyler().setAnnotationsVisible(true);

    List<String> employeeNames =
        employees.stream()
            .map(emp -> emp.getEmployeeEmail().split("@")[0])
            .collect(Collectors.toList());

    List<Double> hoursWorked =
        employees.stream()
            .map(emp -> emp.getTotalHoursWorked() != null ? emp.getTotalHoursWorked() : 0)
            .collect(Collectors.toList());

    chart.addSeries("Hours Worked", employeeNames, hoursWorked);

    return chartToByteArray(chart);
  }

  private byte[] generateComparativeChart(List<EmployeePerformanceDto> employees, String projectKey)
      throws IOException {
    if (employees == null) {
      throw new IllegalArgumentException("La liste des employés est nulle");
    }
    CategoryChart chart =
        new CategoryChartBuilder()
            .width(900)
            .height(600)
            .title("Actual vs Expected Hours - Project: " + projectKey)
            .xAxisTitle("Employee")
            .yAxisTitle("Hours")
            .build();

    chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
    // chart.getStyler().setAnnotationsVisible(true);

    List<String> employeeNames =
        employees.stream()
            .map(emp -> emp.getEmployeeEmail().split("@")[0])
            .collect(Collectors.toList());

    List<Double> actualHours =
        employees.stream()
            .map(emp -> emp.getTotalHoursWorked() != null ? emp.getTotalHoursWorked() : 0)
            .collect(Collectors.toList());

    List<Double> expectedHours =
        employees.stream()
            .map(
                emp ->
                    emp.getExpectedHoursThisMonth() != null ? emp.getExpectedHoursThisMonth() : 0)
            .collect(Collectors.toList());

    chart.addSeries("Actual Hours", employeeNames, actualHours);
    chart.addSeries("Expected Hours", employeeNames, expectedHours);

    return chartToByteArray(chart);
  }

  private byte[] chartToByteArray(CategoryChart chart) throws IOException {
    // Génère le PNG en bytes directement
    return BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);
  }
}
