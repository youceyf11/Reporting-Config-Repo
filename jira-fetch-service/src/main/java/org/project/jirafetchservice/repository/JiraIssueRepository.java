package org.project.jirafetchservice.repository;

import org.project.jirafetchservice.entity.JiraIssueDbEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JiraIssueRepository extends JpaRepository<JiraIssueDbEntity, String> {
  List<JiraIssueDbEntity> findByStatus(String status);

  List<JiraIssueDbEntity> findByAssignee(String assignee);

  Optional<JiraIssueDbEntity> findByIssueKey(String issueKey);

  List<JiraIssueDbEntity> findByProjectKey(String projectKey);

  List<JiraIssueDbEntity> findByIssueKeyIn(List<String> issueKeys);

  List<JiraIssueDbEntity> findByProjectKeyIsNotNull();
}