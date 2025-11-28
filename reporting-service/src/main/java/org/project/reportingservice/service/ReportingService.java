package org.project.reportingservice.service;

import org.project.issueevents.events.IssueUpsertedEvent;
import org.project.reportingservice.dto.WeeklyStatsDto;
import org.project.reportingservice.dto.WeeklySummaryDto;
import org.project.reportingservice.entity.EmployeePerformanceMetric;
import org.project.reportingservice.entity.ReportingIssue;
import org.project.reportingservice.repository.EmployeeMetricRepository;
import org.project.reportingservice.repository.ReportingIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private static final Logger logger = LoggerFactory.getLogger(ReportingService.class);
    private final ReportingIssueRepository issueRepository;
    private final EmployeeMetricRepository metricRepository;
    private final MetricCalculator calculator;

    public ReportingService(ReportingIssueRepository issueRepository,
                            EmployeeMetricRepository metricRepository,
                            MetricCalculator calculator) {
        this.issueRepository = issueRepository;
        this.metricRepository = metricRepository;
        this.calculator = calculator;
    }

    /**
     * Processes incoming Kafka events.
     * Updates the database and CLEARS relevant caches so the next chart request gets fresh data.
     */
    @Transactional
    @CacheEvict(value = {"performance", "monthly_team", "weekly_detailed"}, allEntries = true)
    public void processEvent(IssueUpsertedEvent event) {
        if (event.getAssignee() == null) {
            logger.debug("Skipping event for unassigned issue: {}", event.getIssueKey());
            return;
        }

        try {
            // 1. Convert Event Types to Local Entity Types
            LocalDateTime resolvedTime = null;
            if (event.getResolvedAt() != null) {
                resolvedTime = LocalDateTime.ofInstant(event.getResolvedAt(), ZoneId.of("UTC"));
            }

            String status = (resolvedTime != null) ? "Done" : "In Progress";

            // 2. Save/Update Local Replica in Reporting DB
            ReportingIssue issue = ReportingIssue.builder()
                    .projectKey(event.getProjectKey())
                    .issueKey(event.getIssueKey())
                    .assignee(event.getAssignee())
                    .timeSpentSeconds(event.getTimeSpentSeconds())
                    .originalEstimateSeconds(event.getOriginalEstimateSeconds()) // Important for Accuracy Metric
                    .storyPoints(event.getStoryPoints())
                    .resolved(resolvedTime)
                    .updated(LocalDateTime.now())
                    .status(status)
                    .build();

            issueRepository.save(issue);
            logger.info("Processed event for {}. Points: {}", issue.getIssueKey(), issue.getStoryPoints());

            // 3. Recalculate Monthly Metrics for this User (Entity Update)
            recalculateMetrics(event.getAssignee());

        } catch (Exception e) {
            logger.error("Failed to process event for {}: {}", event.getIssueKey(), e.getMessage());
        }
    }

    // --- 1. MONTHLY INDIVIDUAL REPORT (Existing) ---
    @Cacheable(value = "performance", key = "#period")
    public List<EmployeePerformanceMetric> getMetricsByPeriod(String period) {
        return metricRepository.findByMetricPeriod(period);
    }

    // --- 2. MONTHLY TEAM REPORT (New) ---
    // Aggregates individual monthly metrics into a Team Summary
    @Cacheable(value = "monthly_team", key = "#period")
    public WeeklyStatsDto getMonthlyTeamStats(String period) {
        List<EmployeePerformanceMetric> individuals = metricRepository.findByMetricPeriod(period);

        double totalPoints = individuals.stream().mapToDouble(m -> m.getTotalStoryPoints() != null ? m.getTotalStoryPoints() : 0).sum();
        int totalTickets = individuals.stream().mapToInt(m -> m.getTotalTicketsClosed() != null ? m.getTotalTicketsClosed() : 0).sum();
        double totalHours = individuals.stream().mapToDouble(m -> m.getTotalHoursLogged() != null ? m.getTotalHoursLogged() : 0).sum();

        // --- NEW AGGREGATION ---
        double totalEstimatedHours = individuals.stream()
                .mapToDouble(m -> m.getTotalEstimatedHours() != null ? m.getTotalEstimatedHours() : 0.0)
                .sum();
        // -----------------------

        double teamEfficiency = (totalHours > 0) ? (totalPoints / totalHours) : 0.0;

        double avgAccuracy = individuals.stream()
                .mapToDouble(m -> m.getEstimationAccuracy() != null ? m.getEstimationAccuracy() : 0)
                .average().orElse(0.0);

        return WeeklyStatsDto.builder()
                .totalStoryPoints(totalPoints)
                .totalTicketsClosed(totalTickets)
                .totalHoursLogged(totalHours)
                .totalEstimatedHours(totalEstimatedHours)
                .efficiencyScore(teamEfficiency)
                .estimationAccuracy(avgAccuracy)
                .build();
    }

    // --- 3. WEEKLY DETAILED REPORT (New) ---
    // Returns Team Totals + Employee Breakdown for each week
    @Cacheable(value = "weekly_detailed", key = "#startDateStr + '-' + #endDateStr")
    public Map<String, WeeklySummaryDto> getDetailedWeeklyAnalysis(String startDateStr, String endDateStr) {
        // A. Parse Dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime start = LocalDate.parse(startDateStr, formatter).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDateStr, formatter).atTime(LocalTime.MAX);

        // B. Fetch Raw Data
        List<ReportingIssue> issues = issueRepository.findByResolvedBetween(start, end);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        // C. Group Raw Issues by Week (e.g., "2025-W48")
        Map<String, List<ReportingIssue>> issuesByWeek = issues.stream()
                .collect(Collectors.groupingBy(issue -> {
                    int year = issue.getResolved().getYear();
                    int week = issue.getResolved().get(weekFields.weekOfWeekBasedYear());
                    return String.format("%d-W%02d", year, week);
                }, TreeMap::new, Collectors.toList()));

        // D. Calculate Stats
        Map<String, WeeklySummaryDto> result = new LinkedHashMap<>();

        issuesByWeek.forEach((week, weekIssues) -> {
            // 1. Calculate Whole Team Stats for this week
            WeeklyStatsDto teamStats = calculateStats(weekIssues);

            // 2. Group by Employee and Calculate Stats for each
            Map<String, WeeklyStatsDto> employeeBreakdown = weekIssues.stream()
                    .collect(Collectors.groupingBy(
                            issue -> issue.getAssignee() != null ? issue.getAssignee() : "Unassigned",
                            Collectors.collectingAndThen(Collectors.toList(), this::calculateStats)
                    ));

            result.put(week, new WeeklySummaryDto(teamStats, employeeBreakdown));
        });

        logger.info("Calculated detailed weekly stats from DB for {} to {}", startDateStr, endDateStr);
        return result;
    }

    // --- HELPER: CALCULATOR ENGINE ---
    // Calculates the 5 core metrics for any list of issues
    private WeeklyStatsDto calculateStats(List<ReportingIssue> issues) {
        double points = issues.stream()
                .mapToDouble(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0.0)
                .sum();

        int count = issues.size();

        long totalSeconds = issues.stream()
                .mapToLong(i -> i.getTimeSpentSeconds() != null ? i.getTimeSpentSeconds() : 0)
                .sum();

        long totalEstimatedSeconds = issues.stream()
                .mapToLong(i -> i.getOriginalEstimateSeconds() != null ? i.getOriginalEstimateSeconds() : 0)
                .sum();

        double hours = totalSeconds / 3600.0;
        double estimatedHours = totalEstimatedSeconds / 3600.0;

        // Efficiency: Points / Hour
        double efficiency = (hours > 0) ? (points / hours) : 0.0;

        // Accuracy: (Actual / Estimated) * 100
        double accuracy = (totalEstimatedSeconds > 0)
                ? ((double) totalSeconds / totalEstimatedSeconds) * 100.0
                : 0.0;

        return WeeklyStatsDto.builder()
                .totalStoryPoints(points)
                .totalTicketsClosed(count)
                .totalHoursLogged(hours)
                .totalEstimatedHours(estimatedHours)
                .efficiencyScore(efficiency)
                .estimationAccuracy(accuracy)
                .build();
    }

    // Internal helper to update monthly entity
    private void recalculateMetrics(String assignee) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59);
        String period = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        List<ReportingIssue> issues = issueRepository.findActiveIssuesByAssigneeAndDateRange(
                assignee, startOfMonth, endOfMonth);

        EmployeePerformanceMetric metric = calculator.calculate(assignee, period, issues);

        Optional<EmployeePerformanceMetric> existing =
                metricRepository.findByEmployeeEmailAndMetricPeriod(assignee, period);

        if (existing.isPresent()) {
            metric.setId(existing.get().getId());
        }

        metricRepository.save(metric);
        logger.info("Updated Performance Metrics for {} in period {}", assignee, period);
    }
}