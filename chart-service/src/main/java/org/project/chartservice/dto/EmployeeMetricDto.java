package org.project.chartservice.dto;

import lombok.Data;

@Data
public class EmployeeMetricDto {
    private String employeeEmail;
    private Double totalStoryPoints;
    private Integer totalTicketsClosed;
    private Double totalHoursLogged;
    private Double efficiencyScore;
    private Double estimationAccuracy;
}