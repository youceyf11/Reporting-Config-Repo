package org.project.issueevents.events;

import java.time.Instant;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IssueUpsertedEvent {
  private String projectKey;
  private String issueKey;
  private String assignee;
  private Long timeSpentSeconds;
  private Double storyPoints;
  private Instant resolvedAt;


}
