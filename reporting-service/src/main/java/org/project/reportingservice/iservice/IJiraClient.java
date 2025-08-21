package org.project.reportingservice.iservice;

import org.project.reportingservice.dto.IssueSimpleDto;
import reactor.core.publisher.Flux;

public interface IJiraClient {
  Flux<IssueSimpleDto> fetchProjectIssues(String projectKey, int limit);

  Flux<IssueSimpleDto> fetchIssuesByJql(String jql, int limit);
}
