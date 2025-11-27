package org.project.excelservice.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.excelservice.client.ServiceClient;
import org.project.excelservice.dto.JiraIssueDto;
import org.project.excelservice.dto.ReportingDataDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class ExcelGenerationService {

    private final ServiceClient serviceClient;

    public ExcelGenerationService(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    // --- FILE 1: RAW DATA ---
    public byte[] generateRawDataReport(String projectKey) throws IOException {
        List<JiraIssueDto> issues = serviceClient.getRawIssues(projectKey);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Raw Data");
            createHeader(sheet, "Key", "Summary", "Assignee", "Status", "Points", "Hours Spent", "Created", "Resolved");

            int rowIdx = 1;
            for (JiraIssueDto issue : issues) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(issue.getIssueKey());
                row.createCell(1).setCellValue(issue.getSummary());
                row.createCell(2).setCellValue(issue.getAssignee());
                row.createCell(3).setCellValue(issue.getStatus());
                row.createCell(4).setCellValue(issue.getStoryPoints() != null ? issue.getStoryPoints() : 0);
                row.createCell(5).setCellValue(issue.getTimeSpentSeconds() != null ? issue.getTimeSpentSeconds() / 3600.0 : 0);
                row.createCell(6).setCellValue(issue.getCreated().toString());
                row.createCell(7).setCellValue(issue.getResolved() != null ? issue.getResolved().toString() : "");
            }
            return toBytes(workbook);
        }
    }

    // --- FILE 2: REPORTING METRICS ---
    public byte[] generateMetricsReport(String startDate, String endDate) throws IOException {
        // Map<Week, Data>
        Map<String, ReportingDataDto> data = serviceClient.getWeeklyReport(startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Weekly Metrics");

            // Header
            createHeader(sheet, "Week", "Team Velocity", "Team Throughput", "Team Hours", "Efficiency", "Accuracy %", "Top Performer");

            int rowIdx = 1;
            for (Map.Entry<String, ReportingDataDto> entry : data.entrySet()) {
                String week = entry.getKey();
                ReportingDataDto.WeeklyStatsDto team = entry.getValue().getTeamStats();
                Map<String, ReportingDataDto.WeeklyStatsDto> employees = entry.getValue().getEmployeeStats();

                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(week);

                if (team != null) {
                    row.createCell(1).setCellValue(team.getTotalStoryPoints());
                    row.createCell(2).setCellValue(team.getTotalTicketsClosed());
                    row.createCell(3).setCellValue(team.getTotalHoursLogged());
                    row.createCell(4).setCellValue(team.getEfficiencyScore());
                    row.createCell(5).setCellValue(team.getEstimationAccuracy());
                }

                // Logic to find top performer based on Efficiency
                String topEmployee = employees.entrySet().stream()
                        .max(Map.Entry.comparingByValue((e1, e2) -> Double.compare(e1.getEfficiencyScore(), e2.getEfficiencyScore())))
                        .map(Map.Entry::getKey)
                        .orElse("N/A");

                row.createCell(6).setCellValue(topEmployee);
            }
            return toBytes(workbook);
        }
    }

    private void createHeader(Sheet sheet, String... headers) {
        Row headerRow = sheet.createRow(0);
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private byte[] toBytes(Workbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}