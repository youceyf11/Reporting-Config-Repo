package org.project.excelservice.repository;

import java.time.LocalDateTime;
import org.project.excelservice.entity.Issue;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/** */
public interface IssueRepository extends ReactiveCrudRepository<Issue, String> {
  @Query(
      """
           SELECT * FROM jira_issue
           WHERE project_key = :projectKey
             AND updated > :updatedAfter
           """)
  Flux<Issue> findByProjectKeyAndUpdatedAfter(
      @Param("projectKey") String projectKey, @Param("updatedAfter") LocalDateTime updatedAfter);
}
