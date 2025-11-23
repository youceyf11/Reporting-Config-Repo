package org.project.reportingservice.repository;

import org.project.reportingservice.entity.ReportingIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportingIssueRepository extends JpaRepository<ReportingIssue, String> {

    // Find all issues for a user resolved in a specific date range
    @Query("SELECT r FROM ReportingIssue r WHERE r.assignee = :assignee AND r.resolved BETWEEN :startDate AND :endDate")
    List<ReportingIssue> findClosedIssuesByAssigneeAndDateRange(String assignee, LocalDateTime startDate, LocalDateTime endDate);

    // Find all issues worked on by user in date range (for Time logging)
    @Query("SELECT r FROM ReportingIssue r WHERE r.assignee = :assignee AND r.updated BETWEEN :startDate AND :endDate")
    List<ReportingIssue> findActiveIssuesByAssigneeAndDateRange(String assignee, LocalDateTime startDate, LocalDateTime endDate);

}
