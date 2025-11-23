package org.project.reportingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_performance_metric",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_email", "metric_period"}))
public class EmployeePerformanceMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(name = "metric_period", nullable = false)
    private String metricPeriod;

    // --- 1. Productivity ---
    private Double totalStoryPoints; // Velocity
    private Integer totalTicketsClosed;

    // --- 2. Efficiency ---
    private Double totalHoursLogged;
    private Double efficiencyScore; // Points per Hour

    // --- 3. Planning ---
    private Double estimationAccuracy; // % Variance

    private LocalDateTime lastCalculated;


}