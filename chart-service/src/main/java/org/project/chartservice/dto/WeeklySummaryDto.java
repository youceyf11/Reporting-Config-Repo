package org.project.chartservice.dto;

import lombok.Data;
import java.util.Map;

@Data
public class WeeklySummaryDto {
    private WeeklyStatsDto teamStats;
    private Map<String, WeeklyStatsDto> employeeStats;
}