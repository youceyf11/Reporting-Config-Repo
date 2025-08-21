package org.project.reportingservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

/**
 * Event DTO for receiving issue upserted events from jira-fetch-service This is a copy of the event
 * structure for deserialization
 */
public class IssueUpsertedEvent {
  public String eventId;
  public int eventVersion;

  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
      timezone = "UTC")
  public Instant occurredAt;

  public String producer;
  public String traceId;

  // Issue data
  public String projectKey;
  public String issueKey;
  public String assignee;
  public double timeSpentHours;

  @JsonFormat(
      shape = JsonFormat.Shape.STRING,
      pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
      timezone = "UTC")
  public Instant resolutionDate;

  public String status;
  public String summary;

  public String getProjectKey() {
    return projectKey;
  }

  public IssueUpsertedEvent() {}

  @Override
  public String toString() {
    return String.format(
        "IssueUpsertedEvent{issueKey='%s', projectKey='%s', assignee='%s', timeSpentHours=%.2f}",
        issueKey, projectKey, assignee, timeSpentHours);
  }
}
