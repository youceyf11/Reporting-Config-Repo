package org.project.chartservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for employee performance data. This class is used to transfer employee performance
 * information such as total hours worked and expected hours for the month.
 */
@Getter
@Setter
public class EmployeePerformanceDto {
  @JsonProperty("employeeEmail")
  private String employeeEmail;

  @JsonProperty("totalHoursWorked")
  private Double totalHoursWorked;

  @JsonProperty("expectedHoursThisMonth")
  private Double expectedHoursThisMonth;

  // Constructors
  public EmployeePerformanceDto() {}

  public EmployeePerformanceDto(
      String employeeEmail, Double totalHoursWorked, Double expectedHoursThisMonth) {
    this.employeeEmail = employeeEmail;
    this.totalHoursWorked = totalHoursWorked;
    this.expectedHoursThisMonth = expectedHoursThisMonth;
  }

  // Getters and Setters
  public String getEmployeeEmail() {
    return employeeEmail;
  }

  public void setEmployeeEmail(String employeeEmail) {
    this.employeeEmail = employeeEmail;
  }

  public Double getTotalHoursWorked() {
    return totalHoursWorked;
  }

  public void setTotalHoursWorked(Double totalHoursWorked) {
    this.totalHoursWorked = totalHoursWorked;
  }

  public Double getExpectedHoursThisMonth() {
    return expectedHoursThisMonth;
  }

  public void setExpectedHoursThisMonth(Double expectedHoursThisMonth) {
    this.expectedHoursThisMonth = expectedHoursThisMonth;
  }
}
