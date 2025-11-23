package org.project.jirafetchservice.jirapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

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
    private Object description;
    private String created;
    private String updated;

    @JsonProperty("resolutiondate")
    private String resolved;

    // --- Standard Objects ---
    private IssueType issuetype;
    private Status status;
    private Priority priority;
    private Resolution resolution;
    private User assignee;
    private User reporter;
    private Project project;

    // --- Agile & Performance Metrics ---

    @JsonProperty("timespent")
    private Long timeSpentSeconds; // Actual work logged

    @JsonProperty("timeoriginalestimate")
    private Long originalEstimateSeconds; // What was planned

    @JsonProperty("timeestimate")
    private Long remainingEstimateSeconds; // What is left

    @JsonProperty("customfield_10016")
    private Double storyPoints; // Complexity / Velocity
  }

  // --- Nested Classes ---
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Status { private String name; }
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class IssueType { private String name; }
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Priority { private String name; }
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Resolution { private String name; }
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Project { private String key; private String name; }
  @Data @JsonIgnoreProperties(ignoreUnknown = true)
  public static class User { private String displayName; private String emailAddress; }

  public String getSummary() { return this.fields != null ? this.fields.getSummary() : null; }
}