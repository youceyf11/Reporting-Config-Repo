package org.project.excelservice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class ReportingDataDto {
    // We will map the response from /api/reports/weekly-detailed here
    private WeeklyStatsDto teamStats;
    private Map<String, WeeklyStatsDto> employeeStats;

    @Data
    public static class WeeklyStatsDto {
        private Double totalStoryPoints;
        private Integer totalTicketsClosed;
        private Double totalHoursLogged;
        private Double totalEstimatedHours;
        private Double efficiencyScore;
        private Double estimationAccuracy;
    }
}