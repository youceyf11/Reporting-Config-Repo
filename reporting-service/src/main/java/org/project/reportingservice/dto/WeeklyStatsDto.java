package org.project.reportingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatsDto implements Serializable {
    // 1. Productivity
    private Double totalStoryPoints;      // Velocity
    private Integer totalTicketsClosed;   // Throughput

    // 2. Effort
    private Double totalHoursLogged;

    // 3. Performance (Derived)
    private Double efficiencyScore;       // Points per Hour
    private Double estimationAccuracy;    // % (Actual / Estimated)
}