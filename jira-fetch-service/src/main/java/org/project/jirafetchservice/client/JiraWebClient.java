package org.project.jirafetchservice.client;

import org.project.jirafetchservice.jiraApi.JiraIssueApiResponse;
import org.project.jirafetchservice.jiraApi.JiraSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Client for Interacting with the Jira REST API. This client provides methods to fetch issues and
 * search for issues using JQL.
 */
@Component // indique à Spring que la classe annotée doit être détectée automatiquement et
// enregistrée comme un bean dans le contexte d’application
public class JiraWebClient {

  private final WebClient webClient;

  // Test-friendly constructor without @Qualifier for unit testing
  public JiraWebClient(WebClient webClient) {
    this.webClient = webClient;
  }

  public Mono<JiraIssueApiResponse> getIssue(String issueKey) {
    return webClient
        .get()
        .uri("/rest/api/2/issue/{key}", issueKey)
        .retrieve()
        .bodyToMono(JiraIssueApiResponse.class);
  }

  public Mono<JiraSearchResponse> searchIssues(String jql) {
    return webClient
        .get()
        .uri(uriBuilder -> uriBuilder.path("/rest/api/2/search").queryParam("jql", jql).build())
        .retrieve()
        .bodyToMono(JiraSearchResponse.class);
  }

  // Méthode pour rechercher les issues d'un projet spécifique

  public Flux<JiraIssueApiResponse> searchProjectIssues(String projectKey) {
    String jql = "project = " + projectKey;
    return searchIssues(jql).flatMapMany(response -> Flux.fromIterable(response.getIssues()));
  }

  // Ajouter cette méthode dans votre JiraWebClient.java
  public Flux<JiraIssueApiResponse> getAllProjects() {
    return webClient
        .get()
        .uri("/rest/api/2/project")
        .retrieve()
        .bodyToFlux(JiraIssueApiResponse.class)
        .onErrorResume(
            error -> {
              System.err.println(" Erreur getAllProjects: " + error.getMessage());
              return Flux.empty();
            });
  }
}
