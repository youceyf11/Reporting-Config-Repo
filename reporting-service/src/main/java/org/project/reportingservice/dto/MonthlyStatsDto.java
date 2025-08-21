package org.project.reportingservice.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonthlyStatsDto {

  private String assignee;
  private Map<Integer, Double> hoursByMonth;
  private Double totalMonthlyHours;
  private Double expectedHours;
  private Double performanceRatio;

  public MonthlyStatsDto() {}

  /**
   * Constructs a MonthlyStatsDto with the given assignee, hours by month, and expected hours.
   *
   * @param assignee the name of the assignee
   * @param hoursByMonth a map where keys are month numbers (1-12) and values are hours worked in
   *     that month
   * @param expectedHours the expected number of hours for the month
   */
  public MonthlyStatsDto(String assignee, Map<Integer, Double> hoursByMonth, Double expectedHours) {
    this.assignee = assignee;
    this.hoursByMonth = hoursByMonth;
    this.expectedHours = expectedHours;
    this.totalMonthlyHours =
        hoursByMonth != null
            ? hoursByMonth.values().stream().mapToDouble(Double::doubleValue).sum()
            : 0.0;
    this.performanceRatio =
        (expectedHours != null && expectedHours > 0)
            ? (this.totalMonthlyHours / expectedHours)
            : 0.0;
  }

  /**
   * Sets the hours by month and recalculates total monthly hours and performance ratio.
   *
   * @param hoursByMonth a map where keys are month numbers (1-12) and values are hours worked in
   *     that month
   */
  public void setHoursByMonth(Map<Integer, Double> hoursByMonth) {
    this.hoursByMonth = hoursByMonth;
    this.totalMonthlyHours =
        hoursByMonth != null
            ? hoursByMonth.values().stream().mapToDouble(Double::doubleValue).sum()
            : 0.0;
    this.performanceRatio =
        (expectedHours != null && expectedHours > 0)
            ? (this.totalMonthlyHours / expectedHours)
            : 0.0;
  }

  /**
   * Sets the expected hours and recalculates the performance ratio.
   *
   * @param expectedHours the expected number of hours for the month
   */
  public void setExpectedHours(Double expectedHours) {
    this.expectedHours = expectedHours;
    this.performanceRatio =
        (expectedHours != null && expectedHours > 0)
            ? (this.totalMonthlyHours / expectedHours)
            : 0.0;
  }

  @Override
  public String toString() {
    return "MonthlyStatsDto{"
        + "assignee='"
        + assignee
        + '\''
        + ", hoursByMonth="
        + hoursByMonth
        + ", totalMonthlyHours="
        + totalMonthlyHours
        + ", expectedHours="
        + expectedHours
        + ", performanceRatio="
        + performanceRatio
        + '}';
  }
}
