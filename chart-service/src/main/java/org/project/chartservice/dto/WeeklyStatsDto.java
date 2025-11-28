package org.project.chartservice.dto;

import lombok.Data;

@Data
public class WeeklyStatsDto {
    private Double totalStoryPoints;
    private Integer totalTicketsClosed;
    private Double totalHoursLogged;
    private Double totalEstimatedHours;
    private Double efficiencyScore;
    private Double estimationAccuracy;
}