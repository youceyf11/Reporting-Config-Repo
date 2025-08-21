package org.project.reportingservice.service;

import java.time.LocalDateTime;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.iservice.IJiraClient;
import org.springframework.context.annotation.Profile;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * IJiraClient implementation used only when the <code>testdata</code> Spring profile is active.
 * Instead of calling the remote jira-fetch-service, it simply queries the local in-memory H2
 * database that is pre-populated by <code>data-test.sql</code>.
 */
@Service
@Profile("testdata")
public class TestDataJiraClient implements IJiraClient {

  private final DatabaseClient dbClient;

  public TestDataJiraClient(DatabaseClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public Flux<IssueSimpleDto> fetchProjectIssues(String projectKey, int limit) {
    // NOTE: PostgreSQL does not allow binding a LIMIT value as a parameter in prepared statements.
    // Using a bind here makes the query return zero rows. We hard-code the limit instead.
    String sql = "SELECT * FROM jira_issue WHERE project_key = :pk LIMIT 100";
    return dbClient
        .sql(sql)
        .bind("pk", projectKey)
        // limit hard-coded in SQL
        .map(
            (row, meta) ->
                IssueSimpleDto.builder()
                    .issueKey(row.get("issue_key", String.class))
                    .summary(row.get("summary", String.class))
                    .issueType(row.get("issue_type", String.class))
                    .status(row.get("status", String.class))
                    .priority(row.get("priority", String.class))
                    .resolution(row.get("resolution", String.class))
                    .assignee(row.get("assignee", String.class))
                    .reporter(row.get("reporter", String.class))
                    .organization(row.get("organization", String.class))
                    .created(row.get("created", LocalDateTime.class))
                    .updated(row.get("updated", LocalDateTime.class))
                    .resolved(row.get("resolved", LocalDateTime.class))
                    .timeSpentSeconds(row.get("time_spent_seconds", Long.class))
                    .classification(row.get("classification", String.class))
                    .entity(row.get("entity", String.class))
                    .issueQuality(row.get("issue_quality", String.class))
                    .medium(row.get("medium", String.class))
                    .ttsDays(row.get("tts_days", Double.class))
                    .site(row.get("site", String.class))
                    .month(row.get("issue_month", String.class))
                    .quotaPerProject(row.get("quota_per_project", String.class))
                    .build())
        .all();
  }

  @Override
  public Flux<IssueSimpleDto> fetchIssuesByJql(String jql, int limit) {
    // For the test profile we do not parse JQL – return an empty Flux or extend as needed.
    return Flux.empty();
  }

  // Timestamp columns are mapped directly; helper no longer used.
}
