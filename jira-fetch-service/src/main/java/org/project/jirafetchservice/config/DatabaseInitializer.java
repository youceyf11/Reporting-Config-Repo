package org.project.jirafetchservice.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

  @Autowired private R2dbcEntityTemplate r2dbcEntityTemplate;

  @Override
  public void run(String... args) throws Exception {
    // Supprime la suppression systématique de la table pour conserver les données de test
    // String dropTableSql = "DROP TABLE IF EXISTS jira_issue";

    String createTableSql =
        """
            CREATE TABLE IF NOT EXISTS jira_issue (
                id VARCHAR(255) PRIMARY KEY,
                issue_key VARCHAR(255) UNIQUE NOT NULL,
                project_key VARCHAR(255) NOT NULL,
                self_url VARCHAR(500),
                summary TEXT,
                issue_type VARCHAR(255),
                status VARCHAR(255),
                priority VARCHAR(255),
                resolution VARCHAR(255),
                assignee VARCHAR(255),
                assignee_email VARCHAR(255),
                reporter VARCHAR(255),
                reporter_email VARCHAR(255),
                created TIMESTAMP,
                updated TIMESTAMP,
                resolved TIMESTAMP,
                time_spent_seconds BIGINT,
                organization VARCHAR(255),
                classification VARCHAR(255),
                entity VARCHAR(255),
                issue_quality VARCHAR(255),
                medium VARCHAR(255),
                tts_days DECIMAL(10,2),
                site VARCHAR(255),
                issue_month VARCHAR(255),
                quota_per_project VARCHAR(255)
            )
            """;

    try {
      r2dbcEntityTemplate
          .getDatabaseClient()
          // .sql(dropTableSql).then()  // Désactivé : on ne supprime plus la table
          .sql(createTableSql)
          .then()
          .then(
              r2dbcEntityTemplate
                  .getDatabaseClient()
                  .sql(
                      "CREATE INDEX IF NOT EXISTS idx_jira_issue_project_key ON jira_issue(project_key)")
                  .then())
          .then(
              r2dbcEntityTemplate
                  .getDatabaseClient()
                  .sql("CREATE INDEX IF NOT EXISTS idx_jira_issue_status ON jira_issue(status)")
                  .then())
          .then(
              r2dbcEntityTemplate
                  .getDatabaseClient()
                  .sql("CREATE INDEX IF NOT EXISTS idx_jira_issue_assignee ON jira_issue(assignee)")
                  .then())
          .then(
              r2dbcEntityTemplate
                  .getDatabaseClient()
                  .sql("CREATE OR REPLACE VIEW issue AS SELECT * FROM jira_issue")
                  .then())
          .block();

      System.out.println("✅ Table jira_issue créée avec succès");
    } catch (Exception e) {
      System.err.println("❌ Erreur création table: " + e.getMessage());
      e.printStackTrace();
    }
  }
}
