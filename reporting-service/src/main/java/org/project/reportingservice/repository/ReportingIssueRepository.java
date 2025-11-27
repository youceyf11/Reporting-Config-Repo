package org.project.reportingservice.repository;

import org.project.reportingservice.entity.ReportingIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportingIssueRepository extends JpaRepository<ReportingIssue, String> {

    // Used for Time Logging checks (Existing)
    @Query("SELECT r FROM ReportingIssue r WHERE r.assignee = :assignee AND r.updated BETWEEN :startDate AND :endDate")
    List<ReportingIssue> findActiveIssuesByAssigneeAndDateRange(String assignee, LocalDateTime startDate, LocalDateTime endDate);

    // --- ADD THIS METHOD ---
    // Used for Weekly Detailed Analysis (Fetches ALL resolved issues in the timeframe)
    List<ReportingIssue> findByResolvedBetween(LocalDateTime startDate, LocalDateTime endDate);
}