package org.project.reportingservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/** DTO représentant les performances d'un employé */
@Getter
@Setter
public class EmployeePerformanceDto {

  // Getters et Setters
  @JsonProperty("employeeEmail")
  private String employeeEmail;

  @JsonProperty("totalHoursWorked")
  private Double totalHoursWorked;

  @JsonProperty("expectedHoursThisMonth")
  private Double expectedHoursThisMonth;

  @JsonProperty("totalIssuesResolved")
  private Integer totalIssuesResolved;

  @JsonProperty("averageResolutionTimeHours")
  private Double averageResolutionTimeHours;

  @JsonProperty("performancePercentage")
  private Double performancePercentage;

  @JsonProperty("performanceLevel")
  private String performanceLevel;

  @JsonProperty("ranking")
  private Integer ranking;

  public EmployeePerformanceDto() {}

  public EmployeePerformanceDto(
      String employeeEmail,
      Double totalHoursWorked,
      Integer totalIssuesResolved,
      Double averageResolutionTimeHours,
      Double performancePercentage,
      String performanceLevel,
      Double expectedHoursThisMonth) {
    this.employeeEmail = employeeEmail;
    this.totalHoursWorked = totalHoursWorked;
    this.totalIssuesResolved = totalIssuesResolved;
    this.averageResolutionTimeHours = averageResolutionTimeHours;
    this.performancePercentage = performancePercentage;
    this.performanceLevel = performanceLevel;
    this.expectedHoursThisMonth = expectedHoursThisMonth;
  }

  @Override
  public String toString() {
    return "EmployeePerformanceDto{"
        + "employeeEmail='"
        + employeeEmail
        + '\''
        + ", totalHoursWorked="
        + totalHoursWorked
        + ", totalIssuesResolved="
        + totalIssuesResolved
        + ", performancePercentage="
        + performancePercentage
        + '}';
  }
}
