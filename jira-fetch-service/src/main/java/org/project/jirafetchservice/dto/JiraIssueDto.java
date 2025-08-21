package org.project.jirafetchservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueDto {
  private String id;
  private String issueKey;
  private String projectKey;
  private String self;
  private String summary;
  private String issueType;
  private String status;
  private String priority;
  private String resolution;
  private String assignee;
  private String assigneeEmail;
  private String reporter;
  private String reporterEmail;
  private String created;
  private String updated;
  private String resolved;
  private Long timeSpentSeconds;
  private String organization;
  private String classification;
  private String entity;
  private String issueQuality;
  private String medium;
  private Double ttsDays;
  private String site;
  private String month;
  private String quotaPerProject;
}
