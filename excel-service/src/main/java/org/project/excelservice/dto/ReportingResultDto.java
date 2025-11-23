package org.project.excelservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReportingResultDto {
    
    @JsonProperty("reportGeneratedAt")
    private String reportGeneratedAt;
    
    @JsonProperty("projectKey")
    private String projectKey;
    
    @JsonProperty("month")
    private String month;
    
    @JsonProperty("year")
    private String year;
    
    @JsonProperty("totalEmployees")
    private Integer totalEmployees;
    
    @JsonProperty("totalHoursWorked")
    private Double totalHoursWorked;
    
    @JsonProperty("totalIssuesResolved")
    private Integer totalIssuesResolved;
    
    @JsonProperty("averageResolutionTimeHours")
    private Double averageResolutionTimeHours;
    
    @JsonProperty("employeeRankings")
    private List<EmployeeRanking> employeeRankings;
    
    @Data
    public static class EmployeeRanking {
        
        @JsonProperty("rank")
        private Integer rank;
        
        @JsonProperty("employeeName")
        private String employeeName;
        
        @JsonProperty("employeeEmail")
        private String employeeEmail;
        
        @JsonProperty("issuesResolved")
        private Integer issuesResolved;
        
        @JsonProperty("hoursWorked")
        private Double hoursWorked;
        
        @JsonProperty("averageTtsDays")
        private Double averageTtsDays;
        
        @JsonProperty("site")
        private String site;
    }
}
