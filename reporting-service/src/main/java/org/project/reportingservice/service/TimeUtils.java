package org.project.reportingservice.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.project.reportingservice.iservice.ITimeUtils;
import org.springframework.stereotype.Component;

@Component
public class TimeUtils implements ITimeUtils {

  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  @Override
  public double calculateHoursDifference(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) return 0.0;
    Duration duration = Duration.between(start, end);
    return duration.toMinutes() / 60.0;
  }

  @Override
  public long calculateDaysDifference(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) return 0;
    return ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate());
  }

  @Override
  public boolean isInCurrentMonth(LocalDateTime date) {
    if (date == null) return false;
    YearMonth currentMonth = YearMonth.now();
    YearMonth dateMonth = YearMonth.from(date);
    return currentMonth.equals(dateMonth);
  }

  @Override
  public boolean isInMonth(LocalDateTime date, String targetMonth) {
    if (date == null || targetMonth == null) return false;
    try {
      YearMonth target = YearMonth.parse(targetMonth, MONTH_FORMATTER);
      YearMonth dateMonth = YearMonth.from(date);
      return target.equals(dateMonth);
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getCurrentMonth(LocalDateTime date) {
    if (date == null) return null;
    return YearMonth.now().format(MONTH_FORMATTER);
  }

  @Override
  public String getMonthFromDate(LocalDateTime date) {
    if (date == null) return null;
    return YearMonth.from(date).format(MONTH_FORMATTER);
  }

  @Override
  public double secondsToHours(long seconds) {
    return seconds / 3600.0;
  }

  @Override
  public long hoursToSeconds(double hours) {
    return Math.round(hours * 3600);
  }

  @Override
  public String formatHours(double hours) {
    if (hours == 0) return "0h";
    if (hours < 1) return String.format(Locale.US, "%.2fh", hours);
    if (hours == Math.floor(hours)) return String.format(Locale.US, "%.0fh", hours);
    return String.format(Locale.US, "%.1fh", hours);
  }

  @Override
  public double calculatePerformancePercentage(double actual, double target) {
    if (target <= 0) return 0.0;
    return Math.min(actual / target, 1.0);
  }

  @Override
  public String determinePerformanceLevel(
      double percentage, double excellentThreshold, double goodThreshold, double poorThreshold) {
    if (percentage >= excellentThreshold) return "EXCELLENT";
    else if (percentage >= goodThreshold) return "GOOD";
    else if (percentage >= poorThreshold) return "AVERAGE";
    else return "POOR";
  }

  @Override
  public boolean isValidDate(LocalDateTime date) {
    return date != null && date.isBefore(LocalDateTime.now());
  }

  @Override
  public Integer getWorkingDaysInMonth(YearMonth yearMonth) {
    return 22; // Approximation
  }

  @Override
  public double getExpectedHoursForMonth(YearMonth yearMonth, double hoursPerDay) {
    return getWorkingDaysInMonth(yearMonth) * hoursPerDay;
  }

  @Override
  public double roundToTwoDecimals(double value) {
    return Math.round(value * 100.0) / 100.0;
  }

  @Override
  public String getCurrentYear() {
    return String.valueOf(Year.now().getValue());
  }

  @Override
  public YearMonth getCurrentYearMonth() {
    return YearMonth.now();
  }
}
