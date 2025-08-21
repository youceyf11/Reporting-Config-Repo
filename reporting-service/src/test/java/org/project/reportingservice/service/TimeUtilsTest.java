package org.project.reportingservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("TimeUtils Tests")
class TimeUtilsTest {

  private TimeUtils timeUtils;

  @BeforeEach
  void setUp() {
    timeUtils = new TimeUtils();
  }

  @Nested
  @DisplayName("getExpectedHoursForMonth() Tests")
  class ExpectedHoursInMonthTests {

    @Test
    @DisplayName("Should return 176 hours for standard month with 8 hours per day")
    void getExpectedHoursForMonth_shouldReturn176_forStandardMonth() {
      // Arrange
      YearMonth currentMonth = YearMonth.now();
      double hoursPerDay = 8.0;

      // Act
      Double expectedHours = timeUtils.getExpectedHoursForMonth(currentMonth, hoursPerDay);

      // Assert - 22 working days * 8 hours = 176 hours
      assertThat(expectedHours).isEqualTo(176.0);
    }

    @Test
    @DisplayName("Should be consistent across multiple calls")
    void getExpectedHoursForMonth_shouldBeConsistent() {
      // Arrange
      YearMonth currentMonth = YearMonth.now();
      double hoursPerDay = 8.0;

      // Act
      Double firstCall = timeUtils.getExpectedHoursForMonth(currentMonth, hoursPerDay);
      Double secondCall = timeUtils.getExpectedHoursForMonth(currentMonth, hoursPerDay);

      // Assert
      assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    @DisplayName("Should calculate different hours per day correctly")
    void getExpectedHoursForMonth_shouldCalculateDifferentHoursPerDay() {
      // Arrange
      YearMonth currentMonth = YearMonth.now();

      // Act & Assert
      assertThat(timeUtils.getExpectedHoursForMonth(currentMonth, 6.0)).isEqualTo(132.0); // 22 * 6
      assertThat(timeUtils.getExpectedHoursForMonth(currentMonth, 7.0)).isEqualTo(154.0); // 22 * 7
      assertThat(timeUtils.getExpectedHoursForMonth(currentMonth, 8.0)).isEqualTo(176.0); // 22 * 8
    }
  }

  @Nested
  @DisplayName("getCurrentYearMonth() Tests")
  class GetCurrentYearMonthTests {

    @Test
    @DisplayName("Should return current year and month")
    void getCurrentYearMonth_shouldReturnCurrentYearMonth() {
      // Act
      YearMonth result = timeUtils.getCurrentYearMonth();

      // Assert
      YearMonth expected = YearMonth.now();
      assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should return valid YearMonth object")
    void getCurrentYearMonth_shouldReturnValidYearMonth() {
      // Act
      YearMonth result = timeUtils.getCurrentYearMonth();

      // Assert
      assertThat(result).isNotNull();
      assertThat(result.getYear()).isGreaterThan(2020);
      assertThat(result.getMonthValue()).isBetween(1, 12);
    }
  }

  @Nested
  @DisplayName("Date Filtering Tests")
  class DateFilteringTests {

    @Test
    @DisplayName("Should identify current month dates correctly")
    void isInCurrentMonth_shouldIdentifyCurrentMonthDates() {
      // Arrange
      LocalDateTime currentMonthDate = LocalDateTime.now().withDayOfMonth(15);
      LocalDateTime lastMonthDate = LocalDateTime.now().minusMonths(1);
      LocalDateTime nextMonthDate = LocalDateTime.now().plusMonths(1);

      // Act & Assert
      assertThat(timeUtils.isInCurrentMonth(currentMonthDate)).isTrue();
      assertThat(timeUtils.isInCurrentMonth(lastMonthDate)).isFalse();
      assertThat(timeUtils.isInCurrentMonth(nextMonthDate)).isFalse();
    }

    @Test
    @DisplayName("Should handle edge cases for month boundaries")
    void isInCurrentMonth_shouldHandleMonthBoundaries() {
      // Arrange
      YearMonth currentMonth = timeUtils.getCurrentYearMonth();
      LocalDateTime firstDayOfMonth = currentMonth.atDay(1).atStartOfDay();
      LocalDateTime lastDayOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

      // Act & Assert
      assertThat(timeUtils.isInCurrentMonth(firstDayOfMonth)).isTrue();
      assertThat(timeUtils.isInCurrentMonth(lastDayOfMonth)).isTrue();
    }

    @Test
    @DisplayName("Should handle null dates")
    void isInCurrentMonth_shouldHandleNullDates() {
      // Act & Assert
      assertThat(timeUtils.isInCurrentMonth(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("Performance Calculation Tests")
  class PerformanceCalculationTests {

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 40.0, 80.0, 120.0, 160.0, 200.0})
    @DisplayName("Should calculate performance percentage correctly")
    void calculatePerformancePercentage_shouldCalculateCorrectly(double hoursWorked) {
      // Arrange
      double expectedHours = 160.0;

      // Act
      double percentage = timeUtils.calculatePerformancePercentage(hoursWorked, expectedHours);

      // Assert
      if (hoursWorked == 0.0) {
        assertThat(percentage).isEqualTo(0.0);
      } else if (hoursWorked == 160.0) {
        assertThat(percentage).isEqualTo(1.0); // TimeUtils caps at 1.0 (100%)
      } else if (hoursWorked == 80.0) {
        assertThat(percentage).isEqualTo(0.5);
      } else if (hoursWorked > 160.0) {
        assertThat(percentage).isEqualTo(1.0); // Capped at 1.0
      }

      assertThat(percentage).isGreaterThanOrEqualTo(0.0);
      assertThat(percentage).isLessThanOrEqualTo(1.0);
    }

    @Test
    @DisplayName("Should determine performance levels correctly")
    void determinePerformanceLevel_shouldDetermineCorrectly() {
      // Act & Assert
      assertThat(timeUtils.determinePerformanceLevel(0.95, 0.9, 0.75, 0.6))
          .isEqualTo("EXCELLENT"); // 95%
      assertThat(timeUtils.determinePerformanceLevel(0.80, 0.9, 0.75, 0.6))
          .isEqualTo("GOOD"); // 80%
      assertThat(timeUtils.determinePerformanceLevel(0.65, 0.9, 0.75, 0.6))
          .isEqualTo("AVERAGE"); // 65%
      assertThat(timeUtils.determinePerformanceLevel(0.40, 0.9, 0.75, 0.6))
          .isEqualTo("POOR"); // 40%
    }

    @Test
    @DisplayName("Should handle edge performance thresholds")
    void determinePerformanceLevel_shouldHandleEdgeThresholds() {
      // Act & Assert - Test exact threshold values
      assertThat(timeUtils.determinePerformanceLevel(0.90, 0.9, 0.75, 0.6))
          .isEqualTo("EXCELLENT"); // Exactly 90%
      assertThat(timeUtils.determinePerformanceLevel(0.75, 0.9, 0.75, 0.6))
          .isEqualTo("GOOD"); // Exactly 75%
      assertThat(timeUtils.determinePerformanceLevel(0.60, 0.9, 0.75, 0.6))
          .isEqualTo("AVERAGE"); // Exactly 60%
    }
  }

  @Nested
  @DisplayName("Time Conversion Tests")
  class TimeConversionTests {

    @Test
    @DisplayName("Should convert seconds to hours correctly")
    void secondsToHours_shouldConvertCorrectly() {
      // Act & Assert
      assertThat(timeUtils.secondsToHours(3600)).isEqualTo(1.0); // 1 hour
      assertThat(timeUtils.secondsToHours(7200)).isEqualTo(2.0); // 2 hours
      assertThat(timeUtils.secondsToHours(1800)).isEqualTo(0.5); // 30 minutes
      assertThat(timeUtils.secondsToHours(0)).isEqualTo(0.0); // 0 hours
    }

    @Test
    @DisplayName("Should convert hours to seconds correctly")
    void hoursToSeconds_shouldConvertCorrectly() {
      // Act & Assert
      assertThat(timeUtils.hoursToSeconds(1.0)).isEqualTo(3600); // 1 hour
      assertThat(timeUtils.hoursToSeconds(2.0)).isEqualTo(7200); // 2 hours
      assertThat(timeUtils.hoursToSeconds(0.5)).isEqualTo(1800); // 30 minutes
      assertThat(timeUtils.hoursToSeconds(0.0)).isEqualTo(0); // 0 hours
    }

    @Test
    @DisplayName("Should handle large time values")
    void secondsToHours_shouldHandleLargeValues() {
      // Arrange
      long largeSeconds = 86400; // 24 hours in seconds

      // Act
      double hours = timeUtils.secondsToHours(largeSeconds);

      // Assert
      assertThat(hours).isEqualTo(24.0);
    }

    @Test
    @DisplayName("Should format hours correctly")
    void formatHours_shouldFormatCorrectly() {
      // Act & Assert
      assertThat(timeUtils.formatHours(0.0)).isEqualTo("0h");
      assertThat(timeUtils.formatHours(0.25)).isEqualTo("0.25h");
      assertThat(timeUtils.formatHours(1.0)).isEqualTo("1h");
      assertThat(timeUtils.formatHours(1.5)).isEqualTo("1.5h");
      assertThat(timeUtils.formatHours(24.0)).isEqualTo("24h");
    }
  }

  @Nested
  @DisplayName("Date Utility Tests")
  class DateUtilityTests {

    @Test
    @DisplayName("Should get current month string correctly")
    void getCurrentMonth_shouldReturnCurrentMonth() {
      // Arrange
      LocalDateTime now = LocalDateTime.now();

      // Act
      String currentMonth = timeUtils.getCurrentMonth(now);

      // Assert
      assertThat(currentMonth).isNotNull();
      assertThat(currentMonth).matches("\\d{4}-\\d{2}"); // Format: YYYY-MM
    }

    @Test
    @DisplayName("Should get month from date correctly")
    void getMonthFromDate_shouldReturnCorrectMonth() {
      // Arrange
      LocalDateTime testDate = LocalDateTime.of(2024, 3, 15, 12, 0);

      // Act
      String month = timeUtils.getMonthFromDate(testDate);

      // Assert
      assertThat(month).isEqualTo("2024-03");
    }

    @Test
    @DisplayName("Should get current year correctly")
    void getCurrentYear_shouldReturnCurrentYear() {
      // Act
      String currentYear = timeUtils.getCurrentYear();

      // Assert
      assertThat(currentYear).isNotNull();
      assertThat(Integer.parseInt(currentYear)).isGreaterThanOrEqualTo(2024);
    }

    @Test
    @DisplayName("Should validate dates correctly")
    void isValidDate_shouldValidateCorrectly() {
      // Arrange
      LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
      LocalDateTime futureDate = LocalDateTime.now().plusDays(1);

      // Act & Assert
      assertThat(timeUtils.isValidDate(pastDate)).isTrue();
      assertThat(timeUtils.isValidDate(futureDate)).isFalse();
      assertThat(timeUtils.isValidDate(null)).isFalse();
    }
  }

  @Nested
  @DisplayName("Working Days Tests")
  class WorkingDaysTests {

    @Test
    @DisplayName("Should return 22 working days for any month")
    void getWorkingDaysInMonth_shouldReturn22() {
      // Arrange
      YearMonth january = YearMonth.of(2024, 1);
      YearMonth february = YearMonth.of(2024, 2);
      YearMonth december = YearMonth.of(2024, 12);

      // Act & Assert
      assertThat(timeUtils.getWorkingDaysInMonth(january)).isEqualTo(22);
      assertThat(timeUtils.getWorkingDaysInMonth(february)).isEqualTo(22);
      assertThat(timeUtils.getWorkingDaysInMonth(december)).isEqualTo(22);
    }
  }

  @Nested
  @DisplayName("Utility Methods Tests")
  class UtilityMethodsTests {

    @Test
    @DisplayName("Should round to two decimals correctly")
    void roundToTwoDecimals_shouldRoundCorrectly() {
      // Act & Assert
      assertThat(timeUtils.roundToTwoDecimals(1.234567)).isEqualTo(1.23);
      assertThat(timeUtils.roundToTwoDecimals(1.999)).isEqualTo(2.0);
      assertThat(timeUtils.roundToTwoDecimals(0.0)).isEqualTo(0.0);
      assertThat(timeUtils.roundToTwoDecimals(10.456)).isEqualTo(10.46);
    }

    @Test
    @DisplayName("Should calculate hours difference correctly")
    void calculateHoursDifference_shouldCalculateCorrectly() {
      // Arrange
      LocalDateTime start = LocalDateTime.of(2024, 1, 1, 9, 0);
      LocalDateTime end = LocalDateTime.of(2024, 1, 1, 17, 0);

      // Act
      double hoursDiff = timeUtils.calculateHoursDifference(start, end);

      // Assert
      assertThat(hoursDiff).isEqualTo(8.0);
    }

    @Test
    @DisplayName("Should calculate days difference correctly")
    void calculateDaysDifference_shouldCalculateCorrectly() {
      // Arrange
      LocalDateTime start = LocalDateTime.of(2024, 1, 1, 9, 0);
      LocalDateTime end = LocalDateTime.of(2024, 1, 5, 17, 0);

      // Act
      long daysDiff = timeUtils.calculateDaysDifference(start, end);

      // Assert
      assertThat(daysDiff).isEqualTo(4);
    }

    @Test
    @DisplayName("Should handle null values in calculations")
    void shouldHandleNullValues() {
      // Act & Assert
      assertThat(timeUtils.calculateHoursDifference(null, LocalDateTime.now())).isEqualTo(0.0);
      assertThat(timeUtils.calculateDaysDifference(null, null)).isEqualTo(0);
      assertThat(timeUtils.getCurrentMonth(null)).isNull();
      assertThat(timeUtils.getMonthFromDate(null)).isNull();
    }
  }
}
