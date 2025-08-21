package org.project.jirafetchservice.entity;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("jira_issue")
public class JiraIssueDbEntity implements Persistable<String> {

  @Id private String id;

  @Column("issue_key")
  private String issueKey;

  @Transient private boolean newEntity = true;

  @Column("project_key")
  private String projectKey;

  @Column("self_url")
  private String selfUrl;

  private String summary;

  @Column("issue_type")
  private String issueType;

  private String status;
  private String priority;
  private String resolution;
  private String assignee;

  @Column("assignee_email")
  private String assigneeEmail;

  private String reporter;

  @Column("reporter_email")
  private String reporterEmail;

  private LocalDateTime created;
  private LocalDateTime updated;
  private LocalDateTime resolved;

  @Column("time_spent_seconds")
  private Long timeSpentSeconds;

  @Column("organization")
  private String organization;

  @Column("classification")
  private String classification;

  @Column("entity")
  private String entity;

  @Column("issue_quality")
  private String issueQuality;

  @Column("medium")
  private String medium;

  @Column("tts_days")
  private Double ttsDays;

  @Column("site")
  private String site;

  @Column("issue_month")
  private String issueMonth;

  @Column("quota_per_project")
  private String quotaPerProject;

  // Implement Persistable Integererface
  @Override
  public String getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return newEntity;
  }

  // Constructors
  public JiraIssueDbEntity() {}

  public JiraIssueDbEntity(String id) {
    this.id = id;
    this.newEntity = true;
  }

  // Standard setters
  public void setId(String id) {
    this.id = id;
  }

  public void setNewEntity(boolean newEntity) {
    this.newEntity = newEntity;
  }

  // All other getters and setters
  public String getIssueKey() {
    return issueKey;
  }

  public void setIssueKey(String issueKey) {
    this.issueKey = issueKey;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }

  public String getSelfUrl() {
    return selfUrl;
  }

  public void setSelfUrl(String selfUrl) {
    this.selfUrl = selfUrl;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getIssueType() {
    return issueType;
  }

  public void setIssueType(String issueType) {
    this.issueType = issueType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getResolution() {
    return resolution;
  }

  public void setResolution(String resolution) {
    this.resolution = resolution;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public String getAssigneeEmail() {
    return assigneeEmail;
  }

  public void setAssigneeEmail(String assigneeEmail) {
    this.assigneeEmail = assigneeEmail;
  }

  public String getReporter() {
    return reporter;
  }

  public void setReporter(String reporter) {
    this.reporter = reporter;
  }

  public String getReporterEmail() {
    return reporterEmail;
  }

  public void setReporterEmail(String reporterEmail) {
    this.reporterEmail = reporterEmail;
  }

  public LocalDateTime getCreated() {
    return created;
  }

  public void setCreated(LocalDateTime created) {
    this.created = created;
  }

  public LocalDateTime getUpdated() {
    return updated;
  }

  public void setUpdated(LocalDateTime updated) {
    this.updated = updated;
  }

  public LocalDateTime getResolved() {
    return resolved;
  }

  public void setResolved(LocalDateTime resolved) {
    this.resolved = resolved;
  }

  public Long getTimeSpentSeconds() {
    return timeSpentSeconds;
  }

  public void setTimeSpentSeconds(Long timeSpentSeconds) {
    this.timeSpentSeconds = timeSpentSeconds;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getClassification() {
    return classification;
  }

  public void setClassification(String classification) {
    this.classification = classification;
  }

  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public String getIssueQuality() {
    return issueQuality;
  }

  public void setIssueQuality(String issueQuality) {
    this.issueQuality = issueQuality;
  }

  public String getMedium() {
    return medium;
  }

  public void setMedium(String medium) {
    this.medium = medium;
  }

  public Double getTtsDays() {
    return ttsDays;
  }

  public void setTtsDays(Double ttsDays) {
    this.ttsDays = ttsDays;
  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getIssueMonth() {
    return issueMonth;
  }

  public void setIssueMonth(String issueMonth) {
    this.issueMonth = issueMonth;
  }

  public String getQuotaPerProject() {
    return quotaPerProject;
  }

  public void setQuotaPerProject(String quotaPerProject) {
    this.quotaPerProject = quotaPerProject;
  }

  // toString method for debugging
  @Override
  public String toString() {
    return "JiraIssueDbEntity{"
        + "id='"
        + id
        + '\''
        + ", issueKey='"
        + issueKey
        + '\''
        + ", projectKey='"
        + projectKey
        + '\''
        + ", summary='"
        + summary
        + '\''
        + ", isNew="
        + newEntity
        + '}';
  }
}
