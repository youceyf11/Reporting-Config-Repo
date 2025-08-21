package org.project.jirafetchservice.jiraApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Représentation brute de la réponse JSON de l'API Jira. Permet la désérialisation automatique des
 * tickets Jira avant transformation en entités métier. convertir la reponse JSON de l'API Jira en
 * objets Java.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JiraIssueApiResponse {
  private String id;
  private String key;
  private String self;
  private Fields fields;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Fields {
    private String summary;
    private IssueType issuetype;
    private Status status;
    private Priority priority;
    private Resolution resolution;
    private User assignee;
    private User reporter;
    private String created;
    private String updated;
    private String resolved;

    @JsonProperty("timespent")
    private Long timeSpentSeconds;

    @JsonProperty("customfield_10001")
    private String organization;

    @JsonProperty("customfield_10002")
    private String classification;

    @JsonProperty("customfield_10003")
    private String entity;

    @JsonProperty("customfield_10004")
    private String issueQuality;

    @JsonProperty("customfield_10005")
    private String medium;

    @JsonProperty("customfield_10006")
    private Double ttsDays;

    @JsonProperty("customfield_10007")
    private String site;

    @JsonProperty("customfield_10008")
    private String month;

    @JsonProperty("customfield_10009")
    private String quotaPerProject;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Status {
    private String name;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class IssueType {
    private String name;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Priority {
    private String name;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Resolution {
    private String name;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class User {
    private String displayName;
    private String emailAddress;
  }

  public void setSummary(String summary) {
    if (this.fields == null) {
      this.fields = new Fields();
    }
    this.fields.setSummary(summary);
  }

  public String getSummary() {
    return this.fields != null ? this.fields.getSummary() : null;
  }
}
