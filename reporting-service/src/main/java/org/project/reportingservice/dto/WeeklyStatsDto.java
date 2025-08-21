package org.project.reportingservice.dto;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WeeklyStatsDto {

  private String assignee;
  private Map<Integer, Double> hoursByWeek;
  private Double totalWeeklyHours;
  private Integer numberOfWeeks;

  public WeeklyStatsDto() {}

  public WeeklyStatsDto(String assignee, Map<Integer, Double> hoursByWeek) {
    this.assignee = assignee;
    this.hoursByWeek = hoursByWeek;
    this.totalWeeklyHours =
        hoursByWeek != null
            ? hoursByWeek.values().stream().mapToDouble(Double::doubleValue).sum()
            : 0.0;
    this.numberOfWeeks = hoursByWeek != null ? hoursByWeek.size() : 0;
  }

  /**
   * Sets the hours by week and recalculates total weekly hours and number of weeks.
   *
   * @param hoursByWeek a map where keys are week numbers and values are hours worked in that week
   */
  public void setHoursByWeek(Map<Integer, Double> hoursByWeek) {
    this.hoursByWeek = hoursByWeek;
    this.totalWeeklyHours =
        hoursByWeek != null
            ? hoursByWeek.values().stream().mapToDouble(Double::doubleValue).sum()
            : 0.0;
    this.numberOfWeeks = hoursByWeek != null ? hoursByWeek.size() : 0;
  }

  @Override
  public String toString() {
    return "WeeklyStatsDto{"
        + "assignee='"
        + assignee
        + '\''
        + ", hoursByWeek="
        + hoursByWeek
        + ", totalWeeklyHours="
        + totalWeeklyHours
        + ", numberOfWeeks="
        + numberOfWeeks
        + '}';
  }
}
