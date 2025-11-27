package org.project.excelservice.controller;

import org.project.excelservice.service.ExcelGenerationService;
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

    public ExcelController(ExcelGenerationService excelService) {
        this.excelService = excelService;
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
}