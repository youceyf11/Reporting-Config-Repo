package org.project.jirafetchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueSimpleDto {
  private String projectKey;
  private String issueKey;
  private String summary;
  private String issueType;
  private String status;
  private String priority;
  private String resolution;
  private String assignee;
  private String reporter;

  private LocalDateTime created;
  private LocalDateTime updated;
  private LocalDateTime resolved;

  private Long timeSpentSeconds;
  private Long originalEstimateSeconds;
  private Double storyPoints;
}