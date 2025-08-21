package org.project.reportingservice.response;

/** Classe Integererne pour la réponse des statistiques mensuelles */
public class MonthlyStatsResponse {
  private Double totalHoursWorked;
  private Integer totalEmployees;
  private Double averageHoursPerEmployee;

  public MonthlyStatsResponse() {}

  public MonthlyStatsResponse(
      Double totalHoursWorked, Integer totalEmployees, Double averageHoursPerEmployee) {
    this.totalHoursWorked = totalHoursWorked;
    this.totalEmployees = totalEmployees;
    this.averageHoursPerEmployee = averageHoursPerEmployee;
  }

  public Double getTotalHoursWorked() {
    return totalHoursWorked;
  }

  public void setTotalHoursWorked(Double totalHoursWorked) {
    this.totalHoursWorked = totalHoursWorked;
  }

  public Integer getTotalEmployees() {
    return totalEmployees;
  }

  public void setTotalEmployees(Integer totalEmployees) {
    this.totalEmployees = totalEmployees;
  }

  public Double getAverageHoursPerEmployee() {
    return averageHoursPerEmployee;
  }

  public void setAverageHoursPerEmployee(Double averageHoursPerEmployee) {
    this.averageHoursPerEmployee = averageHoursPerEmployee;
  }
}
