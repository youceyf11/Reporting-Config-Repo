package org.project.jirafetchservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("jira_issue")
public class Issue {
  @Id private UUID id;

  @Column("issue_key")
  private String issueKey;

  private String summary;
  private String issueType;
  private String status;
  private String priority;
  private String resolution;
  private String organization;
  private LocalDateTime created;
  private LocalDateTime updated;
  private LocalDateTime resolved;
  private long timeSpentSeconds;
  private String classification;
  private String entity;
  private String issueQuality;
  private String medium;
  private Double ttsDays;
  private String site;
  private String month;
  private String quotaPerProject;

  // Pour les relations, stocke juste l'identifiant (UUID ou String)
  @Column("assignee_id")
  private UUID assigneeId;

  @Column("reporter_id")
  private UUID reporterId;
}
