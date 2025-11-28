package org.project.excelservice.controller;

import org.project.excelservice.config.RabbitConfig;
import org.project.excelservice.dto.EmailRequestDto;
import org.project.excelservice.service.ExcelGenerationService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    private final ExcelGenerationService excelService;
    private final RabbitTemplate rabbitTemplate;

    public ExcelController(ExcelGenerationService excelService, RabbitTemplate rabbitTemplate) {
        this.excelService = excelService;
        this.rabbitTemplate = rabbitTemplate;
    }

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
        sendToQueue(email, "Raw Data Report: " + projectKey, filename, fileContent);

        return ResponseEntity.ok("Raw data report queued for email to " + email);
    }

    @PostMapping("/email/metrics")
    public ResponseEntity<String> emailMetrics(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String email) throws IOException {

        // 1. Generate File
        byte[] fileContent = excelService.generateMetricsReport(startDate, endDate);
        String filename = "Weekly_Metrics_" + startDate + "_to_" + endDate + ".xlsx";

        // 2. Send to Queue
        sendToQueue(email, "Weekly Metrics Report", filename, fileContent);

        return ResponseEntity.ok("Metrics report queued for email to " + email);
    }

    private void sendToQueue(String to, String subject, String filename, byte[] fileContent) {
        EmailRequestDto emailRequest = EmailRequestDto.builder()
                .to(to)
                .subject(subject)
                .body("Please find the requested Excel report attached.")
                .attachmentName(filename) // Extension .xlsx tells email client it's Excel
                .attachmentData(fileContent)
                .build();

        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, emailRequest);
    }

    @GetMapping("/health")
    public String health() {
        return "Excel Service is UP";
    }
}