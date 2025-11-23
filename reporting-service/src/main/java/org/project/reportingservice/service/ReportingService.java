package org.project.reportingservice.service;

import org.project.issueevents.events.IssueUpsertedEvent;
import org.project.reportingservice.entity.EmployeePerformanceMetric;
import org.project.reportingservice.entity.ReportingIssue;
import org.project.reportingservice.repository.EmployeeMetricRepository;
import org.project.reportingservice.repository.ReportingIssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

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

    @Transactional
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
                    .storyPoints(event.getStoryPoints())
                    .resolved(resolvedTime)
                    .updated(LocalDateTime.now())
                    .status(status)
                    .build();

            issueRepository.save(issue);
            logger.info("Processed event for {}. Points: {}", issue.getIssueKey(), issue.getStoryPoints());

            // 3. Recalculate Metrics for this User
            recalculateMetrics(event.getAssignee());

        } catch (Exception e) {
            logger.error("Failed to process event for {}: {}", event.getIssueKey(), e.getMessage());
        }
    }

    private void recalculateMetrics(String assignee) {
        // Calculate for Current Month
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).withHour(0).withMinute(0);
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59);
        String period = now.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // Fetch all issues for this user in this month to aggregate
        List<ReportingIssue> issues = issueRepository.findActiveIssuesByAssigneeAndDateRange(
                assignee, startOfMonth, endOfMonth);

        EmployeePerformanceMetric metric = calculator.calculate(assignee, period, issues);

        // Upsert Metric
        Optional<EmployeePerformanceMetric> existing =
                metricRepository.findByEmployeeEmailAndMetricPeriod(assignee, period);

        if (existing.isPresent()) {
            metric.setId(existing.get().getId());
        }

        metricRepository.save(metric);
        logger.info("Updated Performance Metrics for {} in period {}", assignee, period);
    }

    public List<EmployeePerformanceMetric> getMetricsByPeriod(String period) {
        return metricRepository.findByMetricPeriod(period);
    }
}