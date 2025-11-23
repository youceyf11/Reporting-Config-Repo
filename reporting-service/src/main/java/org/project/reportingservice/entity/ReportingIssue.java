package org.project.reportingservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reporting_issue")
public class ReportingIssue {
    @Id
    private String issueKey;
    private String projectKey;
    private String assignee;

    private Double storyPoints;
    private Long timeSpentSeconds;
    private Long originalEstimateSeconds;

    private String status;

    private LocalDateTime created;
    private LocalDateTime resolved;
    private LocalDateTime updated;

}