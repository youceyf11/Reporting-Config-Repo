package org.project.reportingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySummaryDto implements Serializable {
    private WeeklyStatsDto teamStats; // Team Aggregates
    private Map<String, WeeklyStatsDto> employeeStats; // Individual Aggregates
}