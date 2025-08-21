package org.project.excelservice.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/** */
@Table("jira_issue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
  @Id private String id;
  private String issueKey;
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
  private LocalDateTime created;
  private LocalDateTime updated;
  private LocalDateTime resolved;
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
