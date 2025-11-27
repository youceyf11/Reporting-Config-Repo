package org.project.issueevents.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueUpsertedEvent {
  private String issueKey;
  private String projectKey;
  private String assignee;
  private String status;
  private Double storyPoints;
  private Long timeSpentSeconds;
  private Long originalEstimateSeconds;
  private Instant resolvedAt;
  private Instant updatedAt;
}