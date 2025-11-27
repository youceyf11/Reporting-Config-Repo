package org.project.excelservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JiraIssueDto {
    private String issueKey;
    private String projectKey;
    private String summary;
    private String status;
    private String assignee;
    private String issueType;
    private Double storyPoints;
    private Long timeSpentSeconds;
    private Long originalEstimateSeconds;
    private LocalDateTime created;
    private LocalDateTime resolved;
}