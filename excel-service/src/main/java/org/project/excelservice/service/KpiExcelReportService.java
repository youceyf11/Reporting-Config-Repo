package org.project.excelservice.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.project.excelservice.dto.ReportingResultDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class KpiExcelReportService {

    private final KpiMetricsService kpiMetricsService;

    public KpiExcelReportService(KpiMetricsService kpiMetricsService) {
        this.kpiMetricsService = kpiMetricsService;
    }

    public ByteArrayResource generateSimpleReport(String projectKey) {
        log.info("Generating simple Excel report for project: {}", projectKey);
        
        ReportingResultDto reportData = kpiMetricsService.getReportingData(projectKey);
        XSSFWorkbook workbook = createSimpleWorkbook(reportData);
        return convertToResource(workbook);
    }

    private XSSFWorkbook createSimpleWorkbook(ReportingResultDto reportData) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monthly Report");
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("MONTHLY REPORTING - " + reportData.getProjectKey());
        titleCell.setCellStyle(createTitleStyle(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        
        rowNum++; // Empty row
        
        // Report Summary
        createSummarySection(sheet, rowNum, reportData, headerStyle, dataStyle);
        rowNum += 6;
        
        // Employee Rankings Table
        createEmployeeTable(sheet, rowNum, reportData, headerStyle, dataStyle);
        
        // Auto-size columns
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
        }
        
        return workbook;
    }

    private void createSummarySection(Sheet sheet, int startRow, ReportingResultDto reportData, 
                                    CellStyle headerStyle, CellStyle dataStyle) {
        
        // Report Generated At
        Row row1 = sheet.createRow(startRow);
        row1.createCell(0).setCellValue("Report Generated At:");
        row1.getCell(0).setCellStyle(headerStyle);
        row1.createCell(1).setCellValue(reportData.getReportGeneratedAt()); // Already a formatted string
        row1.getCell(1).setCellStyle(dataStyle);
        
        // Project Key
        Row row2 = sheet.createRow(startRow + 1);
        row2.createCell(0).setCellValue("Project Key:");
        row2.getCell(0).setCellStyle(headerStyle);
        row2.createCell(1).setCellValue(reportData.getProjectKey());
        row2.getCell(1).setCellStyle(dataStyle);
        
        // Period
        Row row3 = sheet.createRow(startRow + 2);
        row3.createCell(0).setCellValue("Period:");
        row3.getCell(0).setCellStyle(headerStyle);
        row3.createCell(1).setCellValue(reportData.getMonth() + " " + reportData.getYear());
        row3.getCell(1).setCellStyle(dataStyle);
        
        // Summary metrics in horizontal layout
        Row row4 = sheet.createRow(startRow + 3);
        row4.createCell(0).setCellValue("Total Employees:");
        row4.getCell(0).setCellStyle(headerStyle);
        row4.createCell(1).setCellValue(reportData.getTotalEmployees());
        row4.getCell(1).setCellStyle(dataStyle);
        
        row4.createCell(3).setCellValue("Total Hours Worked:");
        row4.getCell(3).setCellStyle(headerStyle);
        row4.createCell(4).setCellValue(reportData.getTotalHoursWorked());
        row4.getCell(4).setCellStyle(dataStyle);
        
        Row row5 = sheet.createRow(startRow + 4);
        row5.createCell(0).setCellValue("Total Issues Resolved:");
        row5.getCell(0).setCellStyle(headerStyle);
        row5.createCell(1).setCellValue(reportData.getTotalIssuesResolved());
        row5.getCell(1).setCellStyle(dataStyle);
        
        row5.createCell(3).setCellValue("Average Resolution Time (Hours):");
        row5.getCell(3).setCellStyle(headerStyle);
        row5.createCell(4).setCellValue(reportData.getAverageResolutionTimeHours());
        row5.getCell(4).setCellStyle(dataStyle);
    }

    private void createEmployeeTable(Sheet sheet, int startRow, ReportingResultDto reportData,
                                   CellStyle headerStyle, CellStyle dataStyle) {
        
        // Table title
        Row titleRow = sheet.createRow(startRow);
        titleRow.createCell(0).setCellValue("EMPLOYEE RANKINGS");
        titleRow.getCell(0).setCellStyle(createTitleStyle(sheet.getWorkbook()));
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 7));
        
        startRow += 2;
        
        // Headers
        Row headerRow = sheet.createRow(startRow);
        String[] headers = {"Rank", "Employee Name", "Email", "Issues Resolved", 
                           "Hours Worked", "Avg TTS (Days)", "Site", "Performance"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        startRow++;
        
        // Employee data rows
        for (ReportingResultDto.EmployeeRanking employee : reportData.getEmployeeRankings()) {
            Row row = sheet.createRow(startRow++);
            
            row.createCell(0).setCellValue(employee.getRank());
            row.createCell(1).setCellValue(employee.getEmployeeName());
            row.createCell(2).setCellValue(employee.getEmployeeEmail());
            row.createCell(3).setCellValue(employee.getIssuesResolved());
            row.createCell(4).setCellValue(employee.getHoursWorked());
            row.createCell(5).setCellValue(employee.getAverageTtsDays());
            row.createCell(6).setCellValue(employee.getSite());
            
            // Calculate performance indicator
            String performance = calculatePerformance(employee.getIssuesResolved(), employee.getHoursWorked());
            row.createCell(7).setCellValue(performance);
            
            // Apply data style to all cells
            for (int i = 0; i < 8; i++) {
                if (row.getCell(i) != null) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }
        }
    }

    private String calculatePerformance(int issuesResolved, double hoursWorked) {
        double efficiency = issuesResolved / (hoursWorked / 40.0); // Issues per week
        if (efficiency >= 3.0) return "Excellent";
        if (efficiency >= 2.0) return "Good";
        if (efficiency >= 1.0) return "Average";
        return "Below Average";
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private ByteArrayResource convertToResource(XSSFWorkbook workbook) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            workbook.close();
            return new ByteArrayResource(outputStream.toByteArray());
        } catch (IOException e) {
            log.error("Failed to convert workbook to resource", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }
}
