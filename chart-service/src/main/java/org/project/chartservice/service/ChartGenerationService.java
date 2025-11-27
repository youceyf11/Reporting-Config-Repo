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

  /**
   * Generates a Bar Chart comparing Employee Efficiency for a specific month.
   */
  public byte[] generateMonthlyBarChart(List<EmployeeMetricDto> metrics, String period) {
    // 1. Create Dataset
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for (EmployeeMetricDto metric : metrics) {
      String name = metric.getEmployeeEmail().split("@")[0]; // "alice"
      dataset.addValue(metric.getEfficiencyScore(), "Efficiency", name);
    }

    // 2. Create Chart
    JFreeChart chart = ChartFactory.createBarChart(
            "Monthly Efficiency - " + period, // Chart Title
            "Employee",                       // X-Axis Label
            "Efficiency (Points/Hour)",       // Y-Axis Label
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false                // Legend, Tooltips, URLs
    );

    // 3. Apply Modern Theme
    ChartThemeFactory.applyModernTheme(chart);

    return toBytes(chart);
  }

  /**
   * Generates a Line Chart showing Team Velocity over the weeks.
   */
  public byte[] generateWeeklyTrendChart(Map<String, WeeklySummaryDto> weeklyData) {
    // 1. Create Dataset
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    // The Map is already sorted by ReportingService (TreeMap), so we just iterate
    for (Map.Entry<String, WeeklySummaryDto> entry : weeklyData.entrySet()) {
      String week = entry.getKey();
      WeeklySummaryDto summary = entry.getValue();

      // Extract Team Stats
      if (summary.getTeamStats() != null) {
        Double totalPoints = summary.getTeamStats().getTotalStoryPoints();
        // Handle nulls safely
        double value = (totalPoints != null) ? totalPoints : 0.0;

        // Add to dataset: (Value, Series Name, X-Axis Category)
        dataset.addValue(value, "Team Velocity", week);
      }
    }

    // 2. Create Line Chart
    JFreeChart chart = ChartFactory.createLineChart(
            "Weekly Velocity Trend",      // Chart Title
            "Week",                       // X-Axis Label
            "Total Story Points",         // Y-Axis Label
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false             // Legend=true to see "Team Velocity" label
    );

    // 3. Apply Modern Theme
    ChartThemeFactory.applyModernTheme(chart);

    // 4. Specific Tweak for Line Charts: Add "Dots" on the line
    CategoryPlot plot = chart.getCategoryPlot();

    // Check if the theme applied a LineAndShapeRenderer, if so, cast it
    if (plot.getRenderer() instanceof LineAndShapeRenderer) {
      LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();

      renderer.setDefaultShapesVisible(true); // Show dots at data points
      renderer.setDefaultShapesFilled(true);
      renderer.setSeriesStroke(0, new BasicStroke(3.0f)); // Thicker line
      renderer.setSeriesPaint(0, ChartThemeFactory.COLOR_PRIMARY); // Blue line
    }

    return toBytes(chart);
  }
  /**
   * Helper to convert JFreeChart object to PNG byte array.
   */
  private byte[] toBytes(JFreeChart chart) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      // Render the chart as a PNG image with dimensions 800x600
      ChartUtils.writeChartAsPNG(out, chart, 800, 600);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Error rendering chart to image", e);
    }
  }
}