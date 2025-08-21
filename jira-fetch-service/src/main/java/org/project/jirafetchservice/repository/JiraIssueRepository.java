package org.project.jirafetchservice.repository;

import java.util.List;
import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import reactor.core.publisher.Flux;

@RepositoryRestResource
public interface JiraIssueRepository extends ReactiveCrudRepository<JiraIssueDbEntity, String> {
  Flux<JiraIssueDbEntity> findByStatus(String status);

  Flux<JiraIssueDbEntity> findByAssignee(String assignee);

  Flux<JiraIssueDbEntity> findByIssueKey(String issueKey);

  Flux<JiraIssueDbEntity> findByProjectKey(String projectKey);

  Flux<JiraIssueDbEntity> findByIssueKeyIn(List<String> issueKeys);

  Flux<JiraIssueDbEntity> findByProjectKeyIsNotNull();
}
