package org.project.jirafetchservice.dto;

import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IssueSimpleDto {

  private String projectKey;
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
  private long timeSpentSeconds; // en secondes
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
}
