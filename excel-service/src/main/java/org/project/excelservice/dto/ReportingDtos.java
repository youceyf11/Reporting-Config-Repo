package org.project.excelservice.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class ReportingDtos {

  @Getter
  @Setter
  @Builder
  public static class ReportingResultDto {
    private String month;
    private String year;
    private String projectKey;
    private List<EmployeePerformanceDto> employeeRankings;

    public String getMonth() {
      return month;
    }

    public void setMonth(String month) {
      this.month = month;
    }

    public String getYear() {
      return year;
    }

    public void setYear(String year) {
      this.year = year;
    }

    public String getProjectKey() {
      return projectKey;
    }

    public void setProjectKey(String projectKey) {
      this.projectKey = projectKey;
    }

    public List<EmployeePerformanceDto> getEmployeeRankings() {
      return employeeRankings;
    }

    public void setEmployeeRankings(List<EmployeePerformanceDto> employeeRankings) {
      this.employeeRankings = employeeRankings;
    }
  }

  @Getter
  @Setter
  @Builder
  public static class EmployeePerformanceDto {
    private String assignee;
    private Double totalHoursWorked;
    private Integer resolvedIssuesCount;
    private Double averageResolutionTimeHours;
    private Double performancePercentage;
    private String performanceLevel;
    private Integer ranking;
    private Double expectedHours;

    public String getAssignee() {
      return assignee;
    }

    public void setAssignee(String assignee) {
      this.assignee = assignee;
    }

    public Double getTotalHoursWorked() {
      return totalHoursWorked;
    }

    public void setTotalHoursWorked(Double totalHoursWorked) {
      this.totalHoursWorked = totalHoursWorked;
    }

    public Integer getResolvedIssuesCount() {
      return resolvedIssuesCount;
    }

    public void setResolvedIssuesCount(Integer resolvedIssuesCount) {
      this.resolvedIssuesCount = resolvedIssuesCount;
    }

    public Double getAverageResolutionTimeHours() {
      return averageResolutionTimeHours;
    }

    public void setAverageResolutionTimeHours(Double averageResolutionTimeHours) {
      this.averageResolutionTimeHours = averageResolutionTimeHours;
    }

    public Double getPerformancePercentage() {
      return performancePercentage;
    }

    public void setPerformancePercentage(Double performancePercentage) {
      this.performancePercentage = performancePercentage;
    }

    public String getPerformanceLevel() {
      return performanceLevel;
    }

    public void setPerformanceLevel(String performanceLevel) {
      this.performanceLevel = performanceLevel;
    }

    public Integer getRanking() {
      return ranking;
    }

    public void setRanking(Integer ranking) {
      this.ranking = ranking;
    }

    public Double getExpectedHours() {
      return expectedHours;
    }

    public void setExpectedHours(Double expectedHours) {
      this.expectedHours = expectedHours;
    }
  }
}
