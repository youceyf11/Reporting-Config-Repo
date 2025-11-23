package org.project.jirafetchservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jira_issue")
public class JiraIssueDbEntity {

  @Id
  @Column(name = "issue_key", unique = true, nullable = false)
  private String issueKey;

  @Column(name = "jira_id")
  private String jiraId;

  @Column(name = "project_key")
  private String projectKey;

  @Column(name = "self_url")
  private String selfUrl;

  @Column(columnDefinition = "TEXT")
  private String summary;

  @Column(name = "issue_type")
  private String issueType;

  private String status;
  private String priority;
  private String resolution;

  private String assignee;

  @Column(name = "assignee_email")
  private String assigneeEmail;

  private String reporter;

  @Column(name = "reporter_email")
  private String reporterEmail;

  private LocalDateTime created;
  private LocalDateTime updated;
  private LocalDateTime resolved;

  // --- Performance Metrics ---

  @Column(name = "time_spent_seconds")
  private Long timeSpentSeconds;

  @Column(name = "original_estimate_seconds")
  private Long originalEstimateSeconds;

  @Column(name = "remaining_estimate_seconds")
  private Long remainingEstimateSeconds;

  @Column(name = "story_points")
  private Double storyPoints;
}