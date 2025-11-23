package org.project.reportingservice.service;

import org.project.reportingservice.entity.EmployeePerformanceMetric;
import org.project.reportingservice.entity.ReportingIssue;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MetricCalculator {

    public EmployeePerformanceMetric calculate(String email, String period, List<ReportingIssue> issues) {
        double totalPoints = 0.0;
        long totalSeconds = 0;
        int closedCount = 0;
        long totalOriginalEstimate = 0;

        for (ReportingIssue issue : issues) {
            // Velocity Logic: Only count if resolved
            if (issue.getResolved() != null) {
                totalPoints += (issue.getStoryPoints() != null ? issue.getStoryPoints() : 0.0);
                closedCount++;
            }

            // Time Logic: Sum all time spent
            if (issue.getTimeSpentSeconds() != null) {
                totalSeconds += issue.getTimeSpentSeconds();
            }

            if (issue.getOriginalEstimateSeconds() != null) {
                totalOriginalEstimate += issue.getOriginalEstimateSeconds();
            }
        }

        double totalHours = totalSeconds / 3600.0;

        // Efficiency: Points / Hour
        double efficiency = (totalHours > 0) ? (totalPoints / totalHours) : 0.0;

        // Accuracy: (Actual / Estimated) * 100. Ideally 100%.
        double accuracy = (totalOriginalEstimate > 0) ?
                ((double) totalSeconds / totalOriginalEstimate) * 100.0 : 0.0;

        return EmployeePerformanceMetric.builder()
                .employeeEmail(email)
                .metricPeriod(period)
                .totalStoryPoints(totalPoints)
                .totalTicketsClosed(closedCount)
                .totalHoursLogged(totalHours)
                .efficiencyScore(efficiency)
                .estimationAccuracy(accuracy)
                .lastCalculated(LocalDateTime.now())
                .build();
    }
}