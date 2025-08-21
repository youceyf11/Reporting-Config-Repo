package org.project.reportingservice.iservice;

import java.time.LocalDateTime;
import java.time.YearMonth;

public interface ITimeUtils {
  double calculateHoursDifference(LocalDateTime start, LocalDateTime end);

  long calculateDaysDifference(LocalDateTime start, LocalDateTime end);

  boolean isInCurrentMonth(LocalDateTime date);

  boolean isInMonth(LocalDateTime date, String targetMonth);

  String getCurrentMonth(LocalDateTime date);

  String getMonthFromDate(LocalDateTime date);

  double secondsToHours(long seconds);

  long hoursToSeconds(double hours);

  String formatHours(double hours);

  double calculatePerformancePercentage(double actual, double target);

  String determinePerformanceLevel(
      double percentage, double excellentThreshold, double goodThreshold, double poorThreshold);

  boolean isValidDate(LocalDateTime date);

  Integer getWorkingDaysInMonth(YearMonth yearMonth);

  double getExpectedHoursForMonth(YearMonth yearMonth, double hoursPerDay);

  double roundToTwoDecimals(double value);

  String getCurrentYear();

  YearMonth getCurrentYearMonth();
}
