package org.project.chartservice.util;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import java.awt.*;

public class ChartThemeFactory {

    public static final Color COLOR_PRIMARY = new Color(54, 162, 235);
    public static final Color COLOR_BG = Color.WHITE;
    public static final Color COLOR_GRID = new Color(220, 220, 220);
    public static final Color COLOR_TEXT = new Color(80, 80, 80);
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_AXIS = new Font("SansSerif", Font.PLAIN, 12);

    public static void applyModernTheme(JFreeChart chart) {
        chart.setBackgroundPaint(COLOR_BG);
        chart.getTitle().setFont(FONT_TITLE);
        chart.getTitle().setPaint(COLOR_TEXT);

        Plot plot = chart.getPlot();
        plot.setBackgroundPaint(COLOR_BG);
        plot.setOutlinePaint(null);

        // 1. Handle Category Plots (Bar Charts AND Line Charts)
        if (plot instanceof CategoryPlot) {
            CategoryPlot categoryPlot = (CategoryPlot) plot;
            categoryPlot.setRangeGridlinePaint(COLOR_GRID);
            categoryPlot.setDomainGridlinesVisible(false);

            CategoryAxis domainAxis = categoryPlot.getDomainAxis();
            domainAxis.setTickLabelFont(FONT_AXIS);
            domainAxis.setLabelFont(FONT_AXIS);
            domainAxis.setTickLabelPaint(COLOR_TEXT);
            domainAxis.setAxisLinePaint(COLOR_TEXT);

            ValueAxis rangeAxis = categoryPlot.getRangeAxis();
            rangeAxis.setTickLabelFont(FONT_AXIS);
            rangeAxis.setLabelFont(FONT_AXIS);
            rangeAxis.setTickLabelPaint(COLOR_TEXT);
            rangeAxis.setAxisLinePaint(COLOR_TEXT);

            // CHECK RENDERER TYPE
            CategoryItemRenderer renderer = categoryPlot.getRenderer();

            if (renderer instanceof BarRenderer) {
                // Apply specific Bar Chart styling
                BarRenderer barRenderer = (BarRenderer) renderer;
                barRenderer.setBarPainter(new StandardBarPainter());
                barRenderer.setShadowVisible(false);
                barRenderer.setSeriesPaint(0, COLOR_PRIMARY);
            } else if (renderer instanceof LineAndShapeRenderer) {
                // Apply specific Line Chart styling
                LineAndShapeRenderer lineRenderer = (LineAndShapeRenderer) renderer;
                lineRenderer.setSeriesStroke(0, new BasicStroke(3.0f));
                lineRenderer.setSeriesPaint(0, COLOR_PRIMARY);
                lineRenderer.setDefaultShapesVisible(true);
                lineRenderer.setDefaultShapesFilled(true);
            }
        }

        // 2. Handle XY Plots (if you use them later)
        if (plot instanceof XYPlot) {
            XYPlot xyPlot = (XYPlot) plot;
            xyPlot.setRangeGridlinePaint(COLOR_GRID);
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) xyPlot.getRenderer();
            renderer.setSeriesStroke(0, new BasicStroke(3.0f));
            renderer.setSeriesPaint(0, COLOR_PRIMARY);
        }
    }
}