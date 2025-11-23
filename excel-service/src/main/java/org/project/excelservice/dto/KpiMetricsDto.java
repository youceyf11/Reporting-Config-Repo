package org.project.excelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiMetricsDto {
    
    // Project Overview KPIs
    private String projectKey;
    private String reportPeriod;
    private LocalDateTime generatedAt;
    
    // Team Performance KPIs
    private TeamPerformanceKpis teamPerformance;
    
    // Quality KPIs
    private QualityKpis quality;
    
    // Productivity KPIs
    private ProductivityKpis productivity;
    
    // Resource Utilization KPIs
    private ResourceUtilizationKpis resourceUtilization;
    
    // Employee KPIs
    private List<EmployeeKpi> employeeKpis;
    
    // Trend Analysis
    private List<TrendDataPoint> trends;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeamPerformanceKpis {
        private int totalEmployees;
        private double totalHoursWorked;
        private double averageHoursPerEmployee;
        private double teamEfficiencyPercentage;
        private int totalTicketsResolved;
        private double averageResolutionTimeHours;
        private String topPerformer;
        private double topPerformerScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityKpis {
        private double defectRate;
        private double reworkPercentage;
        private double customerSatisfactionScore;
        private int criticalIssuesCount;
        private double averageTimeToResolution;
        private double firstTimeFixRate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductivityKpis {
        private double velocityPoints;
        private double throughputTicketsPerDay;
        private double cycleTimeHours;
        private double leadTimeHours;
        private double burndownEfficiency;
        private int featuresDelivered;
        private double codeQualityScore;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceUtilizationKpis {
        private double capacityUtilization;
        private double overtimePercentage;
        private double idleTimePercentage;
        private double crossTrainingIndex;
        private int skillGapCount;
        private double resourceEfficiency;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeKpi {
        private String employeeName;
        private String email;
        private int tasksCompleted;
        private double hoursWorked;
        private double productivityScore;
        private double qualityRating;
        private String site;
        private int rank;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendDataPoint {
        private String period;
        private String metric;
        private double value;
        private double changePercentage;
        private String trend; // "IMPROVING", "DECLINING", "STABLE"
        
        // Legacy fields for backward compatibility
        private String week;
        private String date;
        private Double velocity;
        private Double bugRate;
        private Double teamMorale;
    }
}
