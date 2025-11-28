package org.project.excelservice.controller;

import lombok.RequiredArgsConstructor;
import org.project.excelservice.client.ServiceClient;
import org.project.excelservice.config.RabbitConfig;
import org.project.excelservice.dto.EmailRequestDto;
import org.project.excelservice.dto.ReportingDataDto;
import org.project.excelservice.service.AiAnalysisService;
import org.project.excelservice.service.ExcelGenerationService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final ExcelGenerationService excelService;
    private final RabbitTemplate rabbitTemplate;
    private final AiAnalysisService aiService;
    private final ServiceClient serviceClient;

    // 1. Download Raw Data (Updates)
    @GetMapping("/raw/{projectKey}")
    public ResponseEntity<ByteArrayResource> downloadRawData(@PathVariable String projectKey) throws IOException {
        byte[] data = excelService.generateRawDataReport(projectKey);
        return createResponse(data, "Raw_Data_" + projectKey + ".xlsx");
    }

    // 2. Download Reporting Metrics
    @GetMapping("/metrics")
    public ResponseEntity<ByteArrayResource> downloadMetrics(
            @RequestParam String startDate,
            @RequestParam String endDate) throws IOException {

        byte[] data = excelService.generateMetricsReport(startDate, endDate);
        return createResponse(data, "Metrics_Report.xlsx");
    }

    private ResponseEntity<ByteArrayResource> createResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(data));
    }


    @PostMapping("/email/raw/{projectKey}")
    public ResponseEntity<String> emailRawData(
            @PathVariable String projectKey,
            @RequestParam String email) throws IOException {

        // 1. Generate File (Same logic as download)
        byte[] fileContent = excelService.generateRawDataReport(projectKey);
        String filename = "Raw_Data_" + projectKey + ".xlsx";

        // 2. Send to Queue
        sendToQueue(email, "Raw Data Report: " + projectKey, filename, fileContent, "Please find the raw data report attached.");

        return ResponseEntity.ok("Raw data report queued for email to " + email);
    }

    @PostMapping("/email/metrics")
    public ResponseEntity<String> emailMetrics(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String email) throws IOException {

        // A. Fetch Data for AI
        Map<String, ReportingDataDto> reportData = serviceClient.getWeeklyReport(startDate, endDate);

        // B. Generate AI Summary
        String aiSummary = "";
        if (!reportData.isEmpty()) {
            ReportingDataDto weekData = reportData.values().iterator().next();
            aiSummary = aiService.generateExecutiveSummary(weekData);
        }

        // C. Generate Excel
        byte[] fileContent = excelService.generateMetricsReport(startDate, endDate);
        String filename = "Weekly_Metrics_" + startDate + ".xlsx";

        // D. Build Email Body
        String emailBody = String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <h2 style="color: #2c3e50;">Weekly Performance Report</h2>
                    <p><b>Period:</b> %s to %s</p>
                    <hr style="border: 0; border-top: 1px solid #eee;"/>
                    
                    <h3 style="color: #2c3e50;">AI Executive Analysis</h3>
                    <div style="background-color: #f9f9f9; padding: 15px; border-left: 4px solid #007bff; white-space: pre-wrap; font-family: inherit;">%s</div>
                    
                    <hr style="border: 0; border-top: 1px solid #eee;"/>
                    <p>Please find the detailed Excel metrics attached.</p>
                </body>
                </html>
                """, startDate, endDate, aiSummary);

        // E. Send
        sendToQueue(email, "Weekly AI Report", filename, fileContent, emailBody);

        return ResponseEntity.ok("AI Metrics report queued for email to " + email);
    }

    @GetMapping("/ai/test")
    public ResponseEntity<String> testAiGeneration(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        Map<String, ReportingDataDto> reportData = serviceClient.getWeeklyReport(startDate, endDate);

        if (reportData.isEmpty()) {
            return ResponseEntity.ok("No data found to analyze.");
        }

        ReportingDataDto weekData = reportData.values().iterator().next();
        String summary = aiService.generateExecutiveSummary(weekData);

        return ResponseEntity.ok(summary);
    }

    private void sendToQueue(String to, String subject, String filename, byte[] fileContent, String body) {
        EmailRequestDto emailRequest = EmailRequestDto.builder()
                .to(to)
                .subject(subject)
                .body(body)
                .attachmentName(filename)
                .attachmentData(fileContent)
                .build();

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, emailRequest);
    }

    @GetMapping("/health")
    public String health() {
        return "Excel Service is UP";
    }
}