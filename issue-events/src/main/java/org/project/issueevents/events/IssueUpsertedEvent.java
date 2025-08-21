package org.project.issueevents.events;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IssueUpsertedEvent {
  private String projectKey;
  private String issueKey;
  private String assignee;
  private double timeSpentHours;
  private Instant resolutionDate;
}
