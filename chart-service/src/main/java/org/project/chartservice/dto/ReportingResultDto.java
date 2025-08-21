package org.project.chartservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportingResultDto {
  // Map to 'employeeRankings' field from reporting-service JSON
  @JsonProperty("employeeRankings")
  private List<EmployeePerformanceDto> employees;

  private String month;
  private String year;
  private String projectKey;

  public ReportingResultDto() {}

  // Convenience constructor for only employee list (kept for backward compatibility)
  public ReportingResultDto(List<EmployeePerformanceDto> employees) {
    this.employees = employees;
  }

  public ReportingResultDto(
      String month, String year, String projectKey, List<EmployeePerformanceDto> employeeRankings) {
    this.month = month;
    this.year = year;
    this.projectKey = projectKey;
    this.employees = employeeRankings;
  }
}
