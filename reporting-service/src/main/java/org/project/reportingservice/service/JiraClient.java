package org.project.reportingservice.service;

import java.nio.charset.StandardCharsets;
import org.project.reportingservice.dto.IssueSimpleDto;
import org.project.reportingservice.iservice.IJiraClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

@Service
@Profile("!testdata")
public class JiraClient implements IJiraClient {

  private final WebClient jiraFetchWebClient;

  public JiraClient(@Qualifier("jiraFetchWebClient") WebClient jiraFetchWebClient) {
    this.jiraFetchWebClient = jiraFetchWebClient;
  }

  @Override
  public Flux<IssueSimpleDto> fetchProjectIssues(String projectKey, int limit) {
    return jiraFetchWebClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/jira/projects/{projectKey}/issues")
                    .queryParam("limit", limit)
                    .build(projectKey))
        .retrieve()
        .bodyToFlux(IssueSimpleDto.class)
        .doOnError(
            error -> {
              System.err.println("[JiraClient] Error fetching project issues: " + error);
              if (error instanceof WebClientResponseException) {
                WebClientResponseException ex = (WebClientResponseException) error;
                System.err.println("[JiraClient] Status code: " + ex.getStatusCode().value());
                System.err.println(
                    "[JiraClient] Response body: "
                        + ex.getResponseBodyAsString(StandardCharsets.UTF_8));
              }
            })
        .onErrorResume(
            error -> {
              error.printStackTrace();
              return Flux.empty();
            });
  }

  @Override
  public Flux<IssueSimpleDto> fetchIssuesByJql(String jql, int limit) {
    return jiraFetchWebClient
        .get()
        .uri(
            uriBuilder ->
                uriBuilder
                    .path("/api/jira/search")
                    .queryParam("jql", jql)
                    .queryParam("limit", limit)
                    .build())
        .retrieve()
        .bodyToFlux(IssueSimpleDto.class)
        .doOnError(
            error -> {
              System.err.println("[JiraClient] Error fetching issues by JQL: " + error);
              if (error instanceof WebClientResponseException) {
                WebClientResponseException ex = (WebClientResponseException) error;
                System.err.println("[JiraClient] Status code: " + ex.getStatusCode().value());
                System.err.println(
                    "[JiraClient] Response body: "
                        + ex.getResponseBodyAsString(StandardCharsets.UTF_8));
              }
            })
        .onErrorResume(
            error -> {
              error.printStackTrace();
              return Flux.empty();
            });
  }
}
