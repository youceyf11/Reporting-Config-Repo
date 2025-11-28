package org.project.chartservice.controller;

import lombok.RequiredArgsConstructor;
import org.project.chartservice.client.ReportingClient;
import org.project.chartservice.config.RabbitConfig;
import org.project.chartservice.dto.EmailRequestDto;
import org.project.chartservice.dto.EmployeeMetricDto;
import org.project.chartservice.dto.WeeklySummaryDto;
import org.project.chartservice.service.ChartGenerationService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
public class ChartController {

  private final ReportingClient reportingClient;
  private final ChartGenerationService chartGenService;
  private final RabbitTemplate rabbitTemplate;


  @GetMapping("/weekly-trend")
  public ResponseEntity<byte[]> getWeeklyChart(
          @RequestParam String startDate,
          @RequestParam String endDate) {

    Map<String, WeeklySummaryDto> weeklyData = reportingClient.getWeeklyBreakdown(startDate, endDate);
    byte[] image = chartGenService.generateWeeklyTrendChart(weeklyData);

    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
  }

  @GetMapping("/monthly")
  public ResponseEntity<byte[]> getMonthlyChart(@RequestParam String period) {
    List<EmployeeMetricDto> metrics = reportingClient.getMonthlyMetrics(period);
    byte[] image = chartGenService.generateMonthlyBarChart(metrics, period);

    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
  }

  @GetMapping("/comparative/time")
  public ResponseEntity<byte[]> getComparativeChart(
          @RequestParam String startDate,
          @RequestParam String endDate) {

    Map<String, WeeklySummaryDto> data = reportingClient.getWeeklyBreakdown(startDate, endDate);
    byte[] image = chartGenService.generateComparativeChart(data);

    return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(image);
  }

  @PostMapping("/email/comparative")
  public ResponseEntity<String> emailComparativeChart(
          @RequestParam String email,
          @RequestParam String startDate,
          @RequestParam String endDate) {

    // A. Fetch Data (Same data source as Weekly Trend)
    Map<String, WeeklySummaryDto> data = reportingClient.getWeeklyBreakdown(startDate, endDate);

    // B. Generate the Comparative Image
    byte[] chartImage = chartGenService.generateComparativeChart(data);

    // C. Send to Queue
    sendToQueue(email, "Planned vs Actual Effort Report", "comparative_effort.png", chartImage);

    return ResponseEntity.ok("Comparative chart queued for " + email);
  }

  // ==========================================
  // 2. EMAIL ENDPOINTS (Send via RabbitMQ)
  // ==========================================

  // Use this to email the Weekly Trend (Line Chart)
  @PostMapping("/email/weekly")
  public ResponseEntity<String> emailWeeklyChart(
          @RequestParam String email,
          @RequestParam String startDate,
          @RequestParam String endDate) {

    // A. Generate Image
    Map<String, WeeklySummaryDto> data = reportingClient.getWeeklyBreakdown(startDate, endDate);
    byte[] chartImage = chartGenService.generateWeeklyTrendChart(data);

    // B. Send (Reusing helper)
    sendToQueue(email, "Weekly Trend Chart", "weekly_trend.png", chartImage);

    return ResponseEntity.ok("Weekly chart queued for " + email);
  }

  // Use this to email the Monthly Report (Bar Chart)
  @PostMapping("/email/monthly")
  public ResponseEntity<String> emailMonthlyChart(
          @RequestParam String email,
          @RequestParam String period) {

    // A. Generate Image
    List<EmployeeMetricDto> metrics = reportingClient.getMonthlyMetrics(period);
    byte[] chartImage = chartGenService.generateMonthlyBarChart(metrics, period);

    // B. Send (Reusing helper)
    sendToQueue(email, "Monthly Performance Report: " + period, "monthly_performance.png", chartImage);

    return ResponseEntity.ok("Monthly chart queued for " + email);
  }

  // ==========================================
  // 3. HELPER METHODS
  // ==========================================

  /**
   * Wraps the file in a DTO and pushes it to RabbitMQ.
   * This logic is shared by all email endpoints.
   */
  private void sendToQueue(String email, String subject, String filename, byte[] image) {
    EmailRequestDto emailRequest = EmailRequestDto.builder()
            .to(email)
            .subject(subject)
            .body("Please find the requested chart attached.")
            .attachmentName(filename)
            .attachmentData(image)
            .build();

    rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, emailRequest);
  }

  @GetMapping("/health")
  public String health() {
    return "Chart Service is UP";
  }
}