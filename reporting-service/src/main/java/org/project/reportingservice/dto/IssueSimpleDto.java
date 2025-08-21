package org.project.reportingservice.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IssueSimpleDto {

  private String issueKey; // ex: JIRA-123
  private String summary; // résumé du ticket
  private String issueType; // Bug, Task, Story, etc.
  private String status; // Done, In Progress, etc.
  private String priority; // High, Medium, Low
  private String resolution; // Fixed, Won't Fix, etc.
  private String assignee; // Employé assigné
  private String reporter; // Créateur du ticket
  private String organization; // Équipe ou client lié
  private LocalDateTime created; // Date création
  private LocalDateTime updated; // Dernière mise à jour
  private LocalDateTime resolved; // Date de résolution
  private Long timeSpentSeconds; // en secondes
  private String classification; // Catégorie du ticket
  private String entity; // Département ou composant
  private String issueQuality; // Mesure de qualité (ex. "Good")
  private String medium; // Canal (web, mobile, etc.)
  private Double ttsDays; // Time to Solve (en jours)
  private String site; // Lieu ou site de travail
  private String month; // Pour regrouper les stats mensuelles
  private String quotaPerProject; // Heures attendues sur le projet

  public IssueSimpleDto(String issueKey, String summary) {
    this.issueKey = issueKey;
    this.summary = summary;
  }

  // Constructor for test data creation with key fields
  public IssueSimpleDto(String issueKey, String assignee, Long timeSpentSeconds, String resolved) {
    this.issueKey = issueKey;
    this.assignee = assignee;
    this.timeSpentSeconds = timeSpentSeconds;
    this.resolved = resolved != null ? LocalDateTime.parse(resolved) : null;
  }

  public boolean isResolved() {
    return resolved != null;
  }

  /**
   * Converts timeSpentSeconds to hours
   *
   * @return time spent in hours, or 0.0 if timeSpentSeconds is null
   */
  public Double getTimeSpentHours() {
    if (timeSpentSeconds == null) {
      return 0.0;
    }
    return timeSpentSeconds / 3600.0;
  }
}
