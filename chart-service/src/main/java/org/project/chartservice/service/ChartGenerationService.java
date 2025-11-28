package org.project.chartservice.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.project.chartservice.dto.EmployeeMetricDto;
import org.project.chartservice.dto.WeeklySummaryDto;
import org.project.chartservice.util.ChartThemeFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ChartGenerationService {

  // =========================================================
  // 1. MONTHLY BAR CHART (Employee Efficiency Comparison)
  // =========================================================
  public byte[] generateMonthlyBarChart(List<EmployeeMetricDto> metrics, String period) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for (EmployeeMetricDto metric : metrics) {
      String name = metric.getEmployeeEmail().split("@")[0];
      dataset.addValue(metric.getEfficiencyScore(), "Efficiency", name);
    }

    JFreeChart chart = ChartFactory.createBarChart(
            "Monthly Efficiency - " + period,
            "Employee",
            "Efficiency (Points/Hour)",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false
    );

    ChartThemeFactory.applyModernTheme(chart);
    return toBytes(chart);
  }

  // =========================================================
  // 2. WEEKLY TREND CHART (Line Chart - Team Velocity)
  // =========================================================
  public byte[] generateWeeklyTrendChart(Map<String, WeeklySummaryDto> weeklyData) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    for (Map.Entry<String, WeeklySummaryDto> entry : weeklyData.entrySet()) {
      String week = entry.getKey();
      WeeklySummaryDto summary = entry.getValue();

      if (summary.getTeamStats() != null) {
        Double totalPoints = summary.getTeamStats().getTotalStoryPoints();
        double value = (totalPoints != null) ? totalPoints : 0.0;
        dataset.addValue(value, "Team Velocity", week);
      }
    }

    JFreeChart chart = ChartFactory.createLineChart(
            "Weekly Velocity Trend",
            "Week",
            "Total Story Points",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
    );

    ChartThemeFactory.applyModernTheme(chart);

    // Specific Tweak: Add dots to the line
    CategoryPlot plot = chart.getCategoryPlot();
    if (plot.getRenderer() instanceof LineAndShapeRenderer) {
      LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
      renderer.setDefaultShapesVisible(true);
      renderer.setDefaultShapesFilled(true);
      renderer.setSeriesStroke(0, new BasicStroke(3.0f));
      renderer.setSeriesPaint(0, ChartThemeFactory.COLOR_PRIMARY);
    }

    return toBytes(chart);
  }

  // =========================================================
  // 3. COMPARATIVE CHART (Grouped Bar - Planned vs Actual)
  // =========================================================
  public byte[] generateComparativeChart(Map<String, WeeklySummaryDto> weeklyData) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    for (Map.Entry<String, WeeklySummaryDto> entry : weeklyData.entrySet()) {
      String week = entry.getKey();
      WeeklySummaryDto summary = entry.getValue();

      if (summary.getTeamStats() != null) {
        // Bar 1: Actual Hours
        Double actualObj = summary.getTeamStats().getTotalHoursLogged();
        double actual = (actualObj != null) ? actualObj : 0.0;
        dataset.addValue(actual, "Actual Hours", week);

        // Bar 2: Estimated Hours
        Double estObj = summary.getTeamStats().getTotalEstimatedHours();
        double estimated = (estObj != null) ? estObj : 0.0;
        dataset.addValue(estimated, "Estimated Hours", week);
      }
    }

    JFreeChart chart = ChartFactory.createBarChart(
            "Planned vs Actual Effort (Team)",
            "Week",
            "Hours",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
    );

    ChartThemeFactory.applyModernTheme(chart);
    return toBytes(chart);
  }

  // =========================================================
  // HELPER
  // =========================================================
  private byte[] toBytes(JFreeChart chart) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      ChartUtils.writeChartAsPNG(out, chart, 800, 600);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Error rendering chart to image", e);
    }
  }
}