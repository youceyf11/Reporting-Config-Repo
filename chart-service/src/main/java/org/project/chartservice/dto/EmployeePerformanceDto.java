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

    public void setEmployeeEmail(String employeeEmail) {
    this.employeeEmail = employeeEmail;
  }

    public void setTotalHoursWorked(Double totalHoursWorked) {
    this.totalHoursWorked = totalHoursWorked;
  }

    public void setExpectedHoursThisMonth(Double expectedHoursThisMonth) {
    this.expectedHoursThisMonth = expectedHoursThisMonth;
  }
}
